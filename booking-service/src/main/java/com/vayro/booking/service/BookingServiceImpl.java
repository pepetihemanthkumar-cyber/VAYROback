package com.vayro.booking.service;

import com.vayro.booking.client.NotificationServiceClient;
import com.vayro.booking.client.VehicleServiceClient;
import com.vayro.booking.dto.*;
import com.vayro.booking.entity.Booking;
import com.vayro.booking.entity.BookingStatus;
import com.vayro.booking.entity.PaymentStatus;
import com.vayro.booking.exception.*;
import com.vayro.booking.repository.BookingRepository;
import com.vayro.booking.security.AuthenticatedUser;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private static final List<BookingStatus> BLOCKING_STATUSES = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.ACTIVE
    );

    private static final int STRIPE_COUNT = 128;
    private final Object[] lockStripes;

    private final BookingRepository bookingRepository;
    private final VehicleServiceClient vehicleServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final BigDecimal taxRate;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            VehicleServiceClient vehicleServiceClient,
            NotificationServiceClient notificationServiceClient,
            @Value("${app.booking.tax-rate:0.18}") BigDecimal taxRate
    ) {
        this.bookingRepository = bookingRepository;
        this.vehicleServiceClient = vehicleServiceClient;
        this.notificationServiceClient = notificationServiceClient;
        this.taxRate = taxRate;

        // Initialize bounded lock stripes for JVM-local thread synchronization
        this.lockStripes = new Object[STRIPE_COUNT];
        for (int i = 0; i < STRIPE_COUNT; i++) {
            this.lockStripes[i] = new Object();
        }
    }

    private Object getLockStripe(String vehicleId) {
        int hash = vehicleId != null ? Math.abs(vehicleId.hashCode() % STRIPE_COUNT) : 0;
        return lockStripes[hash];
    }

    @Override
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request, AuthenticatedUser currentUser, String authToken) {
        validateBookingDates(request.getPickupDateTime(), request.getReturnDateTime());

        // Enforce past date restriction for new reservation creation (allow 5 min clock skew buffer)
        if (request.getPickupDateTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new InvalidBookingDateException("Pickup date and time cannot be in the past.");
        }

        // Concurrency lock striped by vehicleId (JVM-local synchronization)
        Object lock = getLockStripe(request.getVehicleId());
        synchronized (lock) {
            // 0. Idempotency Check for Duplicate Payment Callbacks / Submissions
            if (request.getPaymentOrderId() != null && !request.getPaymentOrderId().isBlank()) {
                Optional<Booking> existing = bookingRepository.findByPaymentOrderId(request.getPaymentOrderId().trim());
                if (existing.isPresent()) {
                    log.info("Idempotent booking hit for paymentOrderId: {}. Returning existing booking reference: {}",
                            request.getPaymentOrderId(), existing.get().getBookingReference());
                    return BookingResponse.fromEntity(existing.get());
                }
            }

            // 1. Fetch and validate vehicle from Vehicle Service
            VehicleDto vehicle = vehicleServiceClient.getVehicleById(request.getVehicleId())
                    .orElseThrow(() -> new VehicleUnavailableException("Vehicle with ID '" + request.getVehicleId() + "' was not found or Vehicle Service is unavailable."));

            if (vehicle.getStatus() != null && ("MAINTENANCE".equalsIgnoreCase(vehicle.getStatus()) || "INACTIVE".equalsIgnoreCase(vehicle.getStatus()))) {
                throw new VehicleUnavailableException("Vehicle '" + vehicle.getName() + "' is currently not available (Status: " + vehicle.getStatus() + ").");
            }

            // 2. Enforce overlap check (PENDING, CONFIRMED, ACTIVE)
            List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                    request.getVehicleId(),
                    request.getPickupDateTime(),
                    request.getReturnDateTime(),
                    BLOCKING_STATUSES
            );

            if (!overlapping.isEmpty()) {
                throw new BookingOverlapException("Vehicle '" + vehicle.getName() + "' is already booked for the selected period.");
            }

            // 3. Calculate rental days: max(1, ceil(durationMinutes / 1440))
            int rentalDays = calculateRentalDays(request.getPickupDateTime(), request.getReturnDateTime());

            // 4. Calculate prices with BigDecimal
            BigDecimal pricePerDay = vehicle.getPricePerDay() != null
                    ? BigDecimal.valueOf(vehicle.getPricePerDay()).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal baseAmount = pricePerDay.multiply(BigDecimal.valueOf(rentalDays)).setScale(2, RoundingMode.HALF_UP);

            BigDecimal addOnsAmount = BigDecimal.ZERO;
            if (request.getAddOns() != null) {
                for (AddOnRequest addOn : request.getAddOns()) {
                    if (addOn.getName() == null || addOn.getName().isBlank()) {
                        throw new IllegalArgumentException("Add-on name is required.");
                    }
                    if (addOn.getQuantity() == null || addOn.getQuantity() <= 0) {
                        throw new IllegalArgumentException("Add-on quantity must be at least 1.");
                    }
                    if (addOn.getUnitPrice() == null || addOn.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("Add-on unit price cannot be negative.");
                    }
                    BigDecimal itemTotal = addOn.getUnitPrice().multiply(BigDecimal.valueOf(addOn.getQuantity()));
                    addOnsAmount = addOnsAmount.add(itemTotal);
                }
            }
            addOnsAmount = addOnsAmount.setScale(2, RoundingMode.HALF_UP);

            BigDecimal discountAmount = BigDecimal.ZERO;
            if (request.getDiscountAmount() != null && request.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal eligible = baseAmount.add(addOnsAmount);
                discountAmount = request.getDiscountAmount().min(eligible).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal subtotal = baseAmount.add(addOnsAmount).subtract(discountAmount).max(BigDecimal.ZERO);
            BigDecimal taxAmount = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalAmount = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

            // 5. Payment Verification & Status Determination
            String payStatus = request.getPaymentStatus() != null ? request.getPaymentStatus().trim().toUpperCase() : null;
            if ("FAILED".equals(payStatus) || "CANCELLED".equals(payStatus)) {
                throw new IllegalArgumentException("Payment was not completed successfully (Status: " + payStatus + "). Booking cannot be confirmed.");
            }

            boolean isPaid = "PAID".equals(payStatus) || "COMPLETED".equals(payStatus);

            // 6. Build entity
            Booking booking = new Booking();
            String bookingRef = generateBookingReference();
            booking.setBookingReference(bookingRef);
            booking.setVehicleId(vehicle.getId());
            booking.setVehicleName(vehicle.getName());
            String email = (request.getCustomerEmail() != null && !request.getCustomerEmail().trim().isEmpty())
                    ? request.getCustomerEmail().trim().toLowerCase()
                    : (currentUser.getEmail() != null ? currentUser.getEmail().trim().toLowerCase() : "customer@vayro.com");

            if (!email.contains("@") || email.length() < 5) {
                throw new IllegalArgumentException("Invalid customer email address: " + email);
            }

            String name = (request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty())
                    ? request.getCustomerName().trim()
                    : (currentUser.getName() != null ? currentUser.getName().trim() : "Valued Customer");

            booking.setUserId(currentUser.getUserId());
            booking.setCustomerName(name);
            booking.setCustomerEmail(email);
            booking.setPickupDateTime(request.getPickupDateTime());
            booking.setReturnDateTime(request.getReturnDateTime());
            booking.setRentalDays(rentalDays);
            booking.setPricePerDay(pricePerDay);
            booking.setBaseAmount(baseAmount);
            booking.setAddOnsAmount(addOnsAmount);
            booking.setDiscountAmount(discountAmount);
            booking.setTaxAmount(taxAmount);
            booking.setTotalAmount(totalAmount);

            if (isPaid) {
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setPaymentStatus(PaymentStatus.PAID);
                booking.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod().trim().toUpperCase() : "UPI");
                booking.setPaymentVerifiedAt(LocalDateTime.now());
            } else {
                booking.setStatus(BookingStatus.PENDING);
                booking.setPaymentStatus(PaymentStatus.PENDING);
                booking.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod().trim().toUpperCase() : "PAY_AT_PICKUP");
            }

            booking.setPaymentReference(request.getPaymentReference() != null && !request.getPaymentReference().isBlank()
                    ? request.getPaymentReference().trim()
                    : "PAY-" + bookingRef);
            booking.setPaymentOrderId(request.getPaymentOrderId() != null && !request.getPaymentOrderId().isBlank()
                    ? request.getPaymentOrderId().trim()
                    : null);

            booking.setCustomerPhone(request.getCustomerPhone());
            booking.setPickupLocation(request.getPickupLocation());
            booking.setReturnLocation(request.getReturnLocation());
            booking.setNotes(request.getNotes());

            Booking saved = bookingRepository.save(booking);
            log.info("Booking created successfully with reference: {} (Status: {}, Payment: {}) for vehicleId: {} by user: {}",
                    saved.getBookingReference(), saved.getStatus(), saved.getPaymentStatus(), saved.getVehicleId(), saved.getCustomerEmail());

            // Asynchronously dispatch booking confirmation email & invoice (non-blocking, failure does not roll back booking)
            notificationServiceClient.sendBookingConfirmedNotificationAsync(saved);

            return BookingResponse.fromEntity(saved);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingSummaryResponse> getMyBookings(AuthenticatedUser currentUser, Pageable pageable) {
        Page<Booking> page = bookingRepository.findByUserId(currentUser.getUserId(), pageable);
        return new PageResponse<>(
                page.getContent().stream().map(BookingSummaryResponse::fromEntity).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, AuthenticatedUser currentUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));

        verifyBookingAccess(booking, currentUser);
        return BookingResponse.fromEntity(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String reference, AuthenticatedUser currentUser) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with reference: " + reference));

        verifyBookingAccess(booking, currentUser);
        return BookingResponse.fromEntity(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getAllBookings(
            BookingStatus status,
            String vehicleId,
            String userId,
            String search,
            Pageable pageable
    ) {
        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (vehicleId != null && !vehicleId.isBlank()) {
                predicates.add(cb.equal(root.get("vehicleId"), vehicleId));
            }
            if (userId != null && !userId.isBlank()) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase().trim() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("bookingReference")), pattern),
                        cb.like(cb.lower(root.get("customerName")), pattern),
                        cb.like(cb.lower(root.get("customerEmail")), pattern),
                        cb.like(cb.lower(root.get("vehicleName")), pattern)
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Booking> page = bookingRepository.findAll(spec, pageable);
        return new PageResponse<>(
                page.getContent().stream().map(BookingResponse::fromEntity).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long id, BookingCancelRequest request, AuthenticatedUser currentUser, String authToken) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));

        verifyBookingAccess(booking, currentUser);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Booking " + booking.getBookingReference() + " is already cancelled.");
        }
        if (booking.getStatus() == BookingStatus.ACTIVE) {
            throw new InvalidBookingStateException("Cannot cancel an ACTIVE booking. Active rentals must be returned first.");
        }
        if (booking.getStatus() == BookingStatus.RETURNED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new InvalidBookingStateException("Cannot cancel a completed or returned booking.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        if (request != null && request.getReason() != null && !request.getReason().isBlank()) {
            String currentNotes = booking.getNotes() != null ? booking.getNotes() : "";
            booking.setNotes((currentNotes + " [Cancelled: " + request.getReason() + "]").trim());
        }

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} cancelled successfully by user {}", saved.getBookingReference(), currentUser.getEmail());
        return BookingResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(Long id, AuthenticatedUser currentUser, String authToken) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingStateException("Cannot confirm booking with status " + booking.getStatus() + ". Only PENDING bookings can be confirmed.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} confirmed by admin {}", saved.getBookingReference(), currentUser.getEmail());
        return BookingResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public BookingResponse activateBooking(Long id, AuthenticatedUser currentUser, String authToken) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException("Cannot activate booking with status " + booking.getStatus() + ". Only CONFIRMED bookings can be activated.");
        }

        booking.setStatus(BookingStatus.ACTIVE);
        booking.setPaymentStatus(PaymentStatus.PAID);
        Booking saved = bookingRepository.save(booking);

        // Synchronize vehicle status to RENTED in Vehicle Service
        vehicleServiceClient.updateVehicleStatus(saved.getVehicleId(), "RENTED", authToken);

        log.info("Booking {} activated (status ACTIVE) by admin {}", saved.getBookingReference(), currentUser.getEmail());
        return BookingResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public BookingResponse returnBooking(Long id, AuthenticatedUser currentUser, String authToken) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new InvalidBookingStateException("Cannot return booking with status " + booking.getStatus() + ". Only ACTIVE bookings can be marked as RETURNED.");
        }

        booking.setStatus(BookingStatus.RETURNED);
        booking.setReturnedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        // Synchronize vehicle status back to AVAILABLE in Vehicle Service
        vehicleServiceClient.updateVehicleStatus(saved.getVehicleId(), "AVAILABLE", authToken);

        // Asynchronously dispatch vehicle return notification to Admin (non-blocking, failure does not roll back return)
        notificationServiceClient.sendVehicleReturnedAdminNotificationAsync(saved);

        log.info("Booking {} vehicle returned (status RETURNED) by admin {}", saved.getBookingReference(), currentUser.getEmail());
        return BookingResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public BookingResponse completeBooking(Long id, AuthenticatedUser currentUser, String authToken) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));

        if (booking.getStatus() != BookingStatus.RETURNED) {
            throw new InvalidBookingStateException("Cannot complete booking with status " + booking.getStatus() + ". Only RETURNED bookings can be marked as COMPLETED.");
        }

        if (booking.getReturnedAt() == null) {
            booking.setReturnedAt(LocalDateTime.now());
        }
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setPaymentStatus(PaymentStatus.PAID);
        Booking saved = bookingRepository.save(booking);

        // Ensure vehicle status is marked AVAILABLE in Vehicle Service
        vehicleServiceClient.updateVehicleStatus(saved.getVehicleId(), "AVAILABLE", authToken);

        log.info("Booking {} marked COMPLETED by admin {}", saved.getBookingReference(), currentUser.getEmail());
        return BookingResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(String vehicleId, LocalDateTime pickupDateTime, LocalDateTime returnDateTime) {
        validateBookingDates(pickupDateTime, returnDateTime);

        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                vehicleId,
                pickupDateTime,
                returnDateTime,
                BLOCKING_STATUSES
        );

        boolean available = overlapping.isEmpty();
        String message = available
                ? "Vehicle is available for the requested period."
                : "Vehicle has " + overlapping.size() + " conflicting reservation(s) during the requested period.";

        return new AvailabilityResponse(vehicleId, available, pickupDateTime, returnDateTime, message);
    }

    private void validateBookingDates(LocalDateTime pickup, LocalDateTime returnTime) {
        if (pickup == null || returnTime == null) {
            throw new InvalidBookingDateException("Both pickupDateTime and returnDateTime are required.");
        }

        if (returnTime.isBefore(pickup) || returnTime.isEqual(pickup)) {
            throw new InvalidBookingDateException("Return date/time must be strictly after pickup date/time.");
        }
    }

    private int calculateRentalDays(LocalDateTime pickup, LocalDateTime returnTime) {
        Duration duration = Duration.between(pickup, returnTime);
        long totalMinutes = duration.toMinutes();
        int days = (int) Math.ceil((double) totalMinutes / 1440.0);
        return Math.max(1, days);
    }

    private void verifyBookingAccess(Booking booking, AuthenticatedUser currentUser) {
        if (currentUser.isAdmin()) {
            return;
        }

        boolean isOwner = booking.getUserId().equals(currentUser.getUserId())
                || booking.getCustomerEmail().equalsIgnoreCase(currentUser.getEmail());

        if (!isOwner) {
            throw new UnauthorizedBookingAccessException("You do not have permission to view or manage this booking.");
        }
    }

    private String generateBookingReference() {
        return "BK-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
