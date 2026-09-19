package com.vayro.booking.service;

import com.vayro.booking.client.VehicleServiceClient;
import com.vayro.booking.dto.*;
import com.vayro.booking.entity.Booking;
import com.vayro.booking.entity.BookingStatus;
import com.vayro.booking.entity.PaymentStatus;
import com.vayro.booking.exception.*;
import com.vayro.booking.repository.BookingRepository;
import com.vayro.booking.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VehicleServiceClient vehicleServiceClient;

    @Mock
    private com.vayro.booking.client.NotificationServiceClient notificationServiceClient;

    private BookingServiceImpl bookingService;

    private AuthenticatedUser normalUser;
    private AuthenticatedUser adminUser;
    private AuthenticatedUser anotherUser;
    private VehicleDto availableVehicle;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(bookingRepository, vehicleServiceClient, notificationServiceClient, BigDecimal.valueOf(0.18));

        normalUser = new AuthenticatedUser("1001", "user@vayro.com", "USER", "Alice Customer");
        adminUser = new AuthenticatedUser("9999", "admin@vayro.com", "ADMIN", "Vayro Admin");
        anotherUser = new AuthenticatedUser("1002", "bob@vayro.com", "USER", "Bob Test");

        availableVehicle = new VehicleDto();
        availableVehicle.setId("veh-001");
        availableVehicle.setName("BMW 3 Series");
        availableVehicle.setBrand("BMW");
        availableVehicle.setModel("330i");
        availableVehicle.setPricePerDay(4500.00);
        availableVehicle.setStatus("AVAILABLE");
    }

    @Test
    @DisplayName("1: Booking creation succeeds with generated reference")
    void testCreateBookingSuccess() {
        BookingCreateRequest request = new BookingCreateRequest(
                "veh-001",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(4),
                "Vijayawada Airport",
                "Vijayawada Airport"
        );

        when(vehicleServiceClient.getVehicleById("veh-001")).thenReturn(Optional.of(availableVehicle));
        when(bookingRepository.findOverlappingBookings(anyString(), any(), any(), anyCollection())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(request, normalUser, "token");

        assertNotNull(response);
        assertNotNull(response.getBookingReference());
        assertTrue(response.getBookingReference().startsWith("BK-"));
        assertEquals("veh-001", response.getVehicleId());
        assertEquals(normalUser.getUserId(), response.getUserId());
        assertEquals(BookingStatus.PENDING, response.getStatus());
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
    }

    @Test
    @DisplayName("2: Rental days, base price, add-ons, discount, tax, and total calculated correctly")
    void testPriceAndDurationCalculations() {
        LocalDateTime pickup = LocalDateTime.now().plusDays(1);
        LocalDateTime dropoff = pickup.plusDays(3);

        BookingCreateRequest request = new BookingCreateRequest("veh-001", pickup, dropoff, "HQ", "HQ");
        request.setAddOns(List.of(
                new AddOnRequest("GPS Navigation", 1, BigDecimal.valueOf(300.00)),
                new AddOnRequest("Child Seat", 2, BigDecimal.valueOf(250.00))
        ));
        request.setDiscountAmount(BigDecimal.valueOf(800.00));

        when(vehicleServiceClient.getVehicleById("veh-001")).thenReturn(Optional.of(availableVehicle));
        when(bookingRepository.findOverlappingBookings(anyString(), any(), any(), anyCollection())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.createBooking(request, normalUser, "token");

        assertEquals(3, response.getRentalDays());
        assertEquals(new BigDecimal("13500.00"), response.getBaseAmount());
        assertEquals(new BigDecimal("800.00"), response.getAddOnsAmount());
        assertEquals(new BigDecimal("800.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("2430.00"), response.getTaxAmount());
        assertEquals(new BigDecimal("15930.00"), response.getTotalAmount());
    }

    @Test
    @DisplayName("3: Discount greater than subtotal is safely capped and total is non-negative")
    void testDiscountCappedAtSubtotal() {
        LocalDateTime pickup = LocalDateTime.now().plusDays(1);
        LocalDateTime dropoff = pickup.plusDays(1);

        BookingCreateRequest request = new BookingCreateRequest("veh-001", pickup, dropoff, "HQ", "HQ");
        request.setDiscountAmount(BigDecimal.valueOf(50000.00));

        when(vehicleServiceClient.getVehicleById("veh-001")).thenReturn(Optional.of(availableVehicle));
        when(bookingRepository.findOverlappingBookings(anyString(), any(), any(), anyCollection())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.createBooking(request, normalUser, "token");

        assertEquals(new BigDecimal("4500.00"), response.getBaseAmount());
        assertEquals(new BigDecimal("4500.00"), response.getDiscountAmount());
        assertEquals(new BigDecimal("0.00"), response.getTaxAmount());
        assertEquals(new BigDecimal("0.00"), response.getTotalAmount());
    }

    @Test
    @DisplayName("4: Add-on with blank name is rejected")
    void testInvalidAddOnBlankName() {
        BookingCreateRequest req = new BookingCreateRequest("veh-001", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), "HQ", "HQ");
        req.setAddOns(List.of(new AddOnRequest("", 1, BigDecimal.valueOf(100))));
        when(vehicleServiceClient.getVehicleById("veh-001")).thenReturn(Optional.of(availableVehicle));
        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(req, normalUser, "token"));
    }

    @Test
    @DisplayName("5: Add-on with negative quantity is rejected")
    void testInvalidAddOnNegativeQuantity() {
        BookingCreateRequest req = new BookingCreateRequest("veh-001", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), "HQ", "HQ");
        req.setAddOns(List.of(new AddOnRequest("GPS", -1, BigDecimal.valueOf(100))));
        when(vehicleServiceClient.getVehicleById("veh-001")).thenReturn(Optional.of(availableVehicle));
        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(req, normalUser, "token"));
    }

    @Test
    @DisplayName("6: Add-on with negative unit price is rejected")
    void testInvalidAddOnNegativeUnitPrice() {
        BookingCreateRequest req = new BookingCreateRequest("veh-001", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), "HQ", "HQ");
        req.setAddOns(List.of(new AddOnRequest("GPS", 1, BigDecimal.valueOf(-50))));
        when(vehicleServiceClient.getVehicleById("veh-001")).thenReturn(Optional.of(availableVehicle));
        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(req, normalUser, "token"));
    }

    @Test
    @DisplayName("7: Rental duration ceil rule: 1min, 1hr, 23hr, 24hr, 24hr+1min, 48hr")
    void testRentalDurationScenarios() {
        LocalDateTime base = LocalDateTime.now().plusDays(2);
        assertEquals(1, calculateDays(base, base.plusMinutes(1)));
        assertEquals(1, calculateDays(base, base.plusHours(1)));
        assertEquals(1, calculateDays(base, base.plusHours(23)));
        assertEquals(1, calculateDays(base, base.plusHours(24)));
        assertEquals(2, calculateDays(base, base.plusHours(24).plusMinutes(1)));
        assertEquals(2, calculateDays(base, base.plusHours(48)));
    }

    private int calculateDays(LocalDateTime start, LocalDateTime end) {
        BookingCreateRequest req = new BookingCreateRequest("veh-001", start, end, "HQ", "HQ");
        when(vehicleServiceClient.getVehicleById("veh-001")).thenReturn(Optional.of(availableVehicle));
        when(bookingRepository.findOverlappingBookings(anyString(), any(), any(), anyCollection())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse res = bookingService.createBooking(req, normalUser, "token");
        return res.getRentalDays();
    }

    @Test
    @DisplayName("8: Reject past pickup date for new reservations")
    void testPastPickupDateRejected() {
        BookingCreateRequest request = new BookingCreateRequest(
                "veh-001",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(2),
                "HQ", "HQ"
        );
        assertThrows(InvalidBookingDateException.class, () -> bookingService.createBooking(request, normalUser, "token"));
    }

    @Test
    @DisplayName("9: Reject return date before pickup date")
    void testReturnBeforePickupRejected() {
        BookingCreateRequest req = new BookingCreateRequest(
                "veh-001",
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(1),
                "HQ", "HQ"
        );
        assertThrows(InvalidBookingDateException.class, () -> bookingService.createBooking(req, normalUser, "token"));
    }

    @Test
    @DisplayName("10: Reject equal pickup and return date")
    void testEqualPickupAndReturnRejected() {
        LocalDateTime sameTime = LocalDateTime.now().plusDays(2);
        BookingCreateRequest req = new BookingCreateRequest("veh-001", sameTime, sameTime, "HQ", "HQ");
        assertThrows(InvalidBookingDateException.class, () -> bookingService.createBooking(req, normalUser, "token"));
    }

    @Test
    @DisplayName("11: Vehicle not found rejected safely")
    void testVehicleNotFoundRejected() {
        BookingCreateRequest req = new BookingCreateRequest("non-existent", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), "HQ", "HQ");
        when(vehicleServiceClient.getVehicleById("non-existent")).thenReturn(Optional.empty());
        assertThrows(VehicleUnavailableException.class, () -> bookingService.createBooking(req, normalUser, "token"));
    }

    @Test
    @DisplayName("12: Vehicle status not AVAILABLE rejected safely")
    void testVehicleUnavailableStatusRejected() {
        availableVehicle.setStatus("MAINTENANCE");
        BookingCreateRequest req = new BookingCreateRequest("veh-001", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), "HQ", "HQ");
        when(vehicleServiceClient.getVehicleById("veh-001")).thenReturn(Optional.of(availableVehicle));
        assertThrows(VehicleUnavailableException.class, () -> bookingService.createBooking(req, normalUser, "token"));
    }

    @Test
    @DisplayName("13: Overlapping active booking rejected with BookingOverlapException")
    void testOverlappingBookingRejected() {
        Booking existing = new Booking();
        existing.setId(5L);
        existing.setVehicleId("veh-001");
        existing.setStatus(BookingStatus.CONFIRMED);

        when(vehicleServiceClient.getVehicleById("veh-001")).thenReturn(Optional.of(availableVehicle));
        when(bookingRepository.findOverlappingBookings(anyString(), any(), any(), anyCollection()))
                .thenReturn(List.of(existing));

        BookingCreateRequest request = new BookingCreateRequest("veh-001", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(3), "HQ", "HQ");
        assertThrows(BookingOverlapException.class, () -> bookingService.createBooking(request, normalUser, "token"));
    }

    @Test
    @DisplayName("14: Non-overlapping booking succeeds")
    void testNonOverlappingBookingSucceeds() {
        when(vehicleServiceClient.getVehicleById("veh-001")).thenReturn(Optional.of(availableVehicle));
        when(bookingRepository.findOverlappingBookings(anyString(), any(), any(), anyCollection())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingCreateRequest request = new BookingCreateRequest("veh-001", LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(12), "HQ", "HQ");
        BookingResponse res = bookingService.createBooking(request, normalUser, "token");
        assertNotNull(res);
    }

    @Test
    @DisplayName("15: CANCELLED booking does not block availability")
    void testCancelledBookingDoesNotBlockAvailability() {
        LocalDateTime pickup = LocalDateTime.now().plusDays(1);
        LocalDateTime dropoff = LocalDateTime.now().plusDays(3);
        when(bookingRepository.findOverlappingBookings(eq("veh-001"), eq(pickup), eq(dropoff), anyCollection()))
                .thenReturn(Collections.emptyList());

        AvailabilityResponse response = bookingService.checkAvailability("veh-001", pickup, dropoff);
        assertTrue(response.isAvailable());
    }

    @Test
    @DisplayName("16: COMPLETED booking does not block availability")
    void testCompletedBookingDoesNotBlockAvailability() {
        LocalDateTime pickup = LocalDateTime.now().plusDays(1);
        LocalDateTime dropoff = LocalDateTime.now().plusDays(3);
        when(bookingRepository.findOverlappingBookings(eq("veh-001"), eq(pickup), eq(dropoff), anyCollection()))
                .thenReturn(Collections.emptyList());

        AvailabilityResponse response = bookingService.checkAvailability("veh-001", pickup, dropoff);
        assertTrue(response.isAvailable());
    }

    @Test
    @DisplayName("17: PENDING booking blocks availability")
    void testPendingBookingBlocksAvailability() {
        LocalDateTime pickup = LocalDateTime.now().plusDays(1);
        LocalDateTime dropoff = LocalDateTime.now().plusDays(3);
        Booking pending = new Booking();
        pending.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findOverlappingBookings(eq("veh-001"), eq(pickup), eq(dropoff), anyCollection()))
                .thenReturn(List.of(pending));

        AvailabilityResponse response = bookingService.checkAvailability("veh-001", pickup, dropoff);
        assertFalse(response.isAvailable());
    }

    @Test
    @DisplayName("18: CONFIRMED booking blocks availability")
    void testConfirmedBookingBlocksAvailability() {
        LocalDateTime pickup = LocalDateTime.now().plusDays(1);
        LocalDateTime dropoff = LocalDateTime.now().plusDays(3);
        Booking confirmed = new Booking();
        confirmed.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findOverlappingBookings(eq("veh-001"), eq(pickup), eq(dropoff), anyCollection()))
                .thenReturn(List.of(confirmed));

        AvailabilityResponse response = bookingService.checkAvailability("veh-001", pickup, dropoff);
        assertFalse(response.isAvailable());
    }

    @Test
    @DisplayName("19: ACTIVE booking blocks availability")
    void testActiveBookingBlocksAvailability() {
        LocalDateTime pickup = LocalDateTime.now().plusDays(1);
        LocalDateTime dropoff = LocalDateTime.now().plusDays(3);
        Booking active = new Booking();
        active.setStatus(BookingStatus.ACTIVE);
        when(bookingRepository.findOverlappingBookings(eq("veh-001"), eq(pickup), eq(dropoff), anyCollection()))
                .thenReturn(List.of(active));

        AvailabilityResponse response = bookingService.checkAvailability("veh-001", pickup, dropoff);
        assertFalse(response.isAvailable());
    }

    @Test
    @DisplayName("20: User creates booking under own JWT identity")
    void testUserCreatesBookingUnderOwnIdentity() {
        BookingCreateRequest request = new BookingCreateRequest("veh-001", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), "Vijayawada", "Vijayawada");
        when(vehicleServiceClient.getVehicleById("veh-001")).thenReturn(Optional.of(availableVehicle));
        when(bookingRepository.findOverlappingBookings(anyString(), any(), any(), anyCollection())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(55L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(request, normalUser, "token");
        assertEquals(normalUser.getUserId(), response.getUserId());
        assertEquals(normalUser.getEmail(), response.getCustomerEmail());
        assertEquals(normalUser.getName(), response.getCustomerName());
    }

    @Test
    @DisplayName("21: User can retrieve own booking")
    void testUserCanRetrieveOwnBooking() {
        Booking b = createSampleBooking(10L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.CONFIRMED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(b));

        BookingResponse res = bookingService.getBookingById(10L, normalUser);
        assertEquals(10L, res.getId());
    }

    @Test
    @DisplayName("22: User cannot retrieve another user's booking")
    void testUserCannotRetrieveAnotherUsersBooking() {
        Booking b = createSampleBooking(20L, "other-user", "other@user.com", BookingStatus.CONFIRMED);
        when(bookingRepository.findById(20L)).thenReturn(Optional.of(b));

        assertThrows(UnauthorizedBookingAccessException.class, () -> bookingService.getBookingById(20L, normalUser));
    }

    @Test
    @DisplayName("23: Admin can retrieve any booking")
    void testAdminCanRetrieveAnyBooking() {
        Booking b = createSampleBooking(30L, "other-user", "other@user.com", BookingStatus.ACTIVE);
        when(bookingRepository.findById(30L)).thenReturn(Optional.of(b));

        BookingResponse res = bookingService.getBookingById(30L, adminUser);
        assertNotNull(res);
        assertEquals(30L, res.getId());
    }

    @Test
    @DisplayName("24: Admin can list all bookings")
    void testAdminCanListBookings() {
        Booking b = createSampleBooking(1L, "user-1", "user1@vayro.com", BookingStatus.CONFIRMED);
        when(bookingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(b), PageRequest.of(0, 10), 1));

        PageResponse<BookingResponse> res = bookingService.getAllBookings(null, null, null, null, PageRequest.of(0, 10));
        assertEquals(1, res.getContent().size());
    }

    @Test
    @DisplayName("25: Valid cancellation on PENDING booking works")
    void testValidCancellationPending() {
        Booking pending = createSampleBooking(1L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse res = bookingService.cancelBooking(1L, new BookingCancelRequest("Change of plans"), normalUser, "token");
        assertEquals(BookingStatus.CANCELLED, res.getStatus());
    }

    @Test
    @DisplayName("26: Valid cancellation on CONFIRMED booking works")
    void testValidCancellationConfirmed() {
        Booking confirmed = createSampleBooking(2L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.CONFIRMED);
        when(bookingRepository.findById(2L)).thenReturn(Optional.of(confirmed));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse res = bookingService.cancelBooking(2L, null, normalUser, "token");
        assertEquals(BookingStatus.CANCELLED, res.getStatus());
    }

    @Test
    @DisplayName("27: Cannot cancel already CANCELLED booking")
    void testCannotCancelAlreadyCancelledBooking() {
        Booking cancelled = createSampleBooking(3L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.CANCELLED);
        when(bookingRepository.findById(3L)).thenReturn(Optional.of(cancelled));

        assertThrows(InvalidBookingStateException.class, () -> bookingService.cancelBooking(3L, null, normalUser, "token"));
    }

    @Test
    @DisplayName("28: Cannot cancel ACTIVE booking")
    void testCannotCancelActiveBooking() {
        Booking active = createSampleBooking(4L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.ACTIVE);
        when(bookingRepository.findById(4L)).thenReturn(Optional.of(active));

        assertThrows(InvalidBookingStateException.class, () -> bookingService.cancelBooking(4L, null, normalUser, "token"));
    }

    @Test
    @DisplayName("29: Cannot cancel RETURNED booking")
    void testCannotCancelReturnedBooking() {
        Booking returned = createSampleBooking(5L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.RETURNED);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(returned));

        assertThrows(InvalidBookingStateException.class, () -> bookingService.cancelBooking(5L, null, normalUser, "token"));
    }

    @Test
    @DisplayName("30: Cannot cancel COMPLETED booking")
    void testCannotCancelCompletedBooking() {
        Booking completed = createSampleBooking(6L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.COMPLETED);
        when(bookingRepository.findById(6L)).thenReturn(Optional.of(completed));

        assertThrows(InvalidBookingStateException.class, () -> bookingService.cancelBooking(6L, null, normalUser, "token"));
    }

    @Test
    @DisplayName("31: PENDING -> CONFIRMED transition succeeds")
    void testConfirmTransition() {
        Booking b = createSampleBooking(100L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.PENDING);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse res = bookingService.confirmBooking(100L, adminUser, "token");
        assertEquals(BookingStatus.CONFIRMED, res.getStatus());
    }

    @Test
    @DisplayName("32: CONFIRMED -> ACTIVE transition succeeds and sets vehicle RENTED")
    void testActivateTransition() {
        Booking b = createSampleBooking(101L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.CONFIRMED);
        when(bookingRepository.findById(101L)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse res = bookingService.activateBooking(101L, adminUser, "token");
        assertEquals(BookingStatus.ACTIVE, res.getStatus());
        verify(vehicleServiceClient, times(1)).updateVehicleStatus("veh-001", "RENTED", "token");
    }

    @Test
    @DisplayName("33: ACTIVE -> RETURNED transition succeeds and sets vehicle AVAILABLE")
    void testReturnTransition() {
        Booking b = createSampleBooking(102L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.ACTIVE);
        when(bookingRepository.findById(102L)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse res = bookingService.returnBooking(102L, adminUser, "token");
        assertEquals(BookingStatus.RETURNED, res.getStatus());
        assertNotNull(res.getReturnedAt());
        verify(vehicleServiceClient, times(1)).updateVehicleStatus("veh-001", "AVAILABLE", "token");
    }

    @Test
    @DisplayName("34: RETURNED -> COMPLETED transition succeeds")
    void testCompleteTransition() {
        Booking b = createSampleBooking(103L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.RETURNED);
        when(bookingRepository.findById(103L)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse res = bookingService.completeBooking(103L, adminUser, "token");
        assertEquals(BookingStatus.COMPLETED, res.getStatus());
    }

    @Test
    @DisplayName("35: Invalid transition PENDING -> ACTIVE rejected")
    void testInvalidPendingToActive() {
        Booking b = createSampleBooking(201L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.PENDING);
        when(bookingRepository.findById(201L)).thenReturn(Optional.of(b));
        assertThrows(InvalidBookingStateException.class, () -> bookingService.activateBooking(201L, adminUser, "token"));
    }

    @Test
    @DisplayName("36: Invalid transition PENDING -> RETURNED rejected")
    void testInvalidPendingToReturned() {
        Booking b = createSampleBooking(202L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.PENDING);
        when(bookingRepository.findById(202L)).thenReturn(Optional.of(b));
        assertThrows(InvalidBookingStateException.class, () -> bookingService.returnBooking(202L, adminUser, "token"));
    }

    @Test
    @DisplayName("37: Invalid transition PENDING -> COMPLETED rejected")
    void testInvalidPendingToCompleted() {
        Booking b = createSampleBooking(203L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.PENDING);
        when(bookingRepository.findById(203L)).thenReturn(Optional.of(b));
        assertThrows(InvalidBookingStateException.class, () -> bookingService.completeBooking(203L, adminUser, "token"));
    }

    @Test
    @DisplayName("38: Invalid transition CONFIRMED -> RETURNED rejected")
    void testInvalidConfirmedToReturned() {
        Booking b = createSampleBooking(204L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.CONFIRMED);
        when(bookingRepository.findById(204L)).thenReturn(Optional.of(b));
        assertThrows(InvalidBookingStateException.class, () -> bookingService.returnBooking(204L, adminUser, "token"));
    }

    @Test
    @DisplayName("39: Invalid transition CONFIRMED -> COMPLETED rejected")
    void testInvalidConfirmedToCompleted() {
        Booking b = createSampleBooking(205L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.CONFIRMED);
        when(bookingRepository.findById(205L)).thenReturn(Optional.of(b));
        assertThrows(InvalidBookingStateException.class, () -> bookingService.completeBooking(205L, adminUser, "token"));
    }

    @Test
    @DisplayName("40: Invalid transition ACTIVE -> CONFIRMED rejected")
    void testInvalidActiveToConfirmed() {
        Booking b = createSampleBooking(206L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.ACTIVE);
        when(bookingRepository.findById(206L)).thenReturn(Optional.of(b));
        assertThrows(InvalidBookingStateException.class, () -> bookingService.confirmBooking(206L, adminUser, "token"));
    }

    @Test
    @DisplayName("41: Invalid transition RETURNED -> ACTIVE rejected")
    void testInvalidReturnedToActive() {
        Booking b = createSampleBooking(207L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.RETURNED);
        when(bookingRepository.findById(207L)).thenReturn(Optional.of(b));
        assertThrows(InvalidBookingStateException.class, () -> bookingService.activateBooking(207L, adminUser, "token"));
    }

    @Test
    @DisplayName("42: Invalid transition COMPLETED -> ACTIVE rejected")
    void testInvalidCompletedToActive() {
        Booking b = createSampleBooking(208L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.COMPLETED);
        when(bookingRepository.findById(208L)).thenReturn(Optional.of(b));
        assertThrows(InvalidBookingStateException.class, () -> bookingService.activateBooking(208L, adminUser, "token"));
    }

    @Test
    @DisplayName("43: Invalid transition CANCELLED -> CONFIRMED rejected")
    void testInvalidCancelledToConfirmed() {
        Booking b = createSampleBooking(209L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.CANCELLED);
        when(bookingRepository.findById(209L)).thenReturn(Optional.of(b));
        assertThrows(InvalidBookingStateException.class, () -> bookingService.confirmBooking(209L, adminUser, "token"));
    }

    @Test
    @DisplayName("44: Invalid transition CANCELLED -> ACTIVE rejected")
    void testInvalidCancelledToActive() {
        Booking b = createSampleBooking(210L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.CANCELLED);
        when(bookingRepository.findById(210L)).thenReturn(Optional.of(b));
        assertThrows(InvalidBookingStateException.class, () -> bookingService.activateBooking(210L, adminUser, "token"));
    }

    @Test
    @DisplayName("45: Availability endpoint returns true when vehicle is available")
    void testAvailabilityReturnsTrue() {
        LocalDateTime pickup = LocalDateTime.now().plusDays(2);
        LocalDateTime dropoff = LocalDateTime.now().plusDays(4);
        when(bookingRepository.findOverlappingBookings(eq("veh-001"), eq(pickup), eq(dropoff), anyCollection()))
                .thenReturn(Collections.emptyList());

        AvailabilityResponse available = bookingService.checkAvailability("veh-001", pickup, dropoff);
        assertTrue(available.isAvailable());
    }

    @Test
    @DisplayName("46: Availability endpoint returns false when vehicle has conflicting booking")
    void testAvailabilityReturnsFalse() {
        LocalDateTime pickup = LocalDateTime.now().plusDays(2);
        LocalDateTime dropoff = LocalDateTime.now().plusDays(4);
        Booking active = new Booking();
        active.setStatus(BookingStatus.ACTIVE);
        when(bookingRepository.findOverlappingBookings(eq("veh-001"), eq(pickup), eq(dropoff), anyCollection()))
                .thenReturn(List.of(active));

        AvailabilityResponse booked = bookingService.checkAvailability("veh-001", pickup, dropoff);
        assertFalse(booked.isAvailable());
    }

    @Test
    @DisplayName("47: getMyBookings retrieves paged bookings for current user")
    void testGetMyBookings() {
        Booking b = createSampleBooking(200L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.CONFIRMED);
        when(bookingRepository.findByUserId(eq(normalUser.getUserId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(b), PageRequest.of(0, 10), 1));

        PageResponse<BookingSummaryResponse> myBookings = bookingService.getMyBookings(normalUser, PageRequest.of(0, 10));
        assertNotNull(myBookings);
        assertEquals(1, myBookings.getContent().size());
    }

    @Test
    @DisplayName("48: getBookingByReference succeeds for owner")
    void testGetBookingByReference() {
        Booking b = createSampleBooking(300L, normalUser.getUserId(), normalUser.getEmail(), BookingStatus.PENDING);
        b.setBookingReference("BK-UNIQUE-REF");
        when(bookingRepository.findByBookingReference("BK-UNIQUE-REF")).thenReturn(Optional.of(b));

        BookingResponse res = bookingService.getBookingByReference("BK-UNIQUE-REF", normalUser);
        assertEquals("BK-UNIQUE-REF", res.getBookingReference());
    }

    private Booking createSampleBooking(Long id, String userId, String email, BookingStatus status) {
        Booking b = new Booking();
        b.setId(id);
        b.setBookingReference("BK-" + id);
        b.setVehicleId("veh-001");
        b.setVehicleName("BMW 3 Series");
        b.setUserId(userId);
        b.setCustomerName("Customer " + userId);
        b.setCustomerEmail(email);
        b.setPickupDateTime(LocalDateTime.now().plusDays(1));
        b.setReturnDateTime(LocalDateTime.now().plusDays(3));
        b.setRentalDays(2);
        b.setPricePerDay(BigDecimal.valueOf(1000));
        b.setBaseAmount(BigDecimal.valueOf(2000));
        b.setAddOnsAmount(BigDecimal.ZERO);
        b.setDiscountAmount(BigDecimal.ZERO);
        b.setTaxAmount(BigDecimal.valueOf(360));
        b.setTotalAmount(BigDecimal.valueOf(2360));
        b.setStatus(status);
        b.setPaymentStatus(PaymentStatus.PENDING);
        b.setPickupLocation("Vijayawada");
        b.setReturnLocation("Vijayawada");
        return b;
    }

    @Test
    @DisplayName("Notification: createBooking triggers async booking confirmation notification")
    void createBooking_triggersAsyncConfirmationNotification() {
        BookingCreateRequest request = new BookingCreateRequest(
                "veh-001",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(4),
                "Hyderabad",
                "Hyderabad"
        );
        request.setCustomerPhone("+91 99887 76655");

        when(vehicleServiceClient.getVehicleById("veh-001")).thenReturn(Optional.of(availableVehicle));
        when(bookingRepository.findOverlappingBookings(anyString(), any(), any(), anyList())).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(101L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(request, normalUser, "test-jwt-token");

        assertNotNull(response);
        verify(notificationServiceClient, times(1))
                .sendBookingConfirmedNotificationAsync(any(Booking.class));
    }

    @Test
    @DisplayName("Notification: returnBooking triggers async vehicle return admin notification")
    void returnBooking_triggersAsyncVehicleReturnAdminNotification() {
        Booking activeBooking = createSampleBooking(201L, "1001", "user@vayro.com", BookingStatus.ACTIVE);
        when(bookingRepository.findById(201L)).thenReturn(Optional.of(activeBooking));
        when(vehicleServiceClient.updateVehicleStatus("veh-001", "AVAILABLE", "admin-jwt-token")).thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.returnBooking(201L, adminUser, "admin-jwt-token");

        assertNotNull(response);
        assertEquals(BookingStatus.RETURNED, response.getStatus());
        verify(notificationServiceClient, times(1))
                .sendVehicleReturnedAdminNotificationAsync(any(Booking.class));
    }

    @Test
    @DisplayName("Notification: cancelBooking does NOT trigger vehicle return notification")
    void cancelBooking_doesNotTriggerVehicleReturnNotification() {
        Booking pendingBooking = createSampleBooking(301L, "1001", "user@vayro.com", BookingStatus.PENDING);
        when(bookingRepository.findById(301L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancelBooking(301L, new BookingCancelRequest("Change of plans"), normalUser, "jwt-token");

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        verify(notificationServiceClient, never())
                .sendVehicleReturnedAdminNotificationAsync(any(Booking.class));
    }
}
