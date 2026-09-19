package com.vayro.booking.repository;

import com.vayro.booking.entity.Booking;
import com.vayro.booking.entity.BookingStatus;
import com.vayro.booking.entity.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    private static final List<BookingStatus> BLOCKING = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.ACTIVE
    );

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
    }

    private Booking createBooking(String ref, String vehicleId, String userId, LocalDateTime pickup, LocalDateTime returnTime, BookingStatus status) {
        Booking b = new Booking();
        b.setBookingReference(ref);
        b.setVehicleId(vehicleId);
        b.setVehicleName("Test Vehicle");
        b.setUserId(userId);
        b.setCustomerName("Customer " + userId);
        b.setCustomerEmail(userId + "@vayro.com");
        b.setPickupDateTime(pickup);
        b.setReturnDateTime(returnTime);
        b.setRentalDays(3);
        b.setPricePerDay(BigDecimal.valueOf(1000));
        b.setBaseAmount(BigDecimal.valueOf(3000));
        b.setAddOnsAmount(BigDecimal.ZERO);
        b.setDiscountAmount(BigDecimal.ZERO);
        b.setTaxAmount(BigDecimal.valueOf(540));
        b.setTotalAmount(BigDecimal.valueOf(3540));
        b.setStatus(status);
        b.setPaymentStatus(PaymentStatus.PENDING);
        b.setPickupLocation("Vijayawada");
        b.setReturnLocation("Vijayawada");
        return bookingRepository.save(b);
    }

    @Test
    @DisplayName("Repository: findByBookingReference returns matching entity")
    void testFindByBookingReference() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 1, 10, 0);
        createBooking("BK-REF-100", "veh-1", "user-1", base, base.plusDays(3), BookingStatus.CONFIRMED);

        Optional<Booking> found = bookingRepository.findByBookingReference("BK-REF-100");
        assertTrue(found.isPresent());
        assertEquals("BK-REF-100", found.get().getBookingReference());
    }

    @Test
    @DisplayName("Repository: findByUserId returns paged bookings")
    void testFindByUserId() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 1, 10, 0);
        createBooking("BK-1", "veh-1", "user-A", base, base.plusDays(2), BookingStatus.PENDING);
        createBooking("BK-2", "veh-2", "user-A", base.plusDays(5), base.plusDays(7), BookingStatus.CONFIRMED);
        createBooking("BK-3", "veh-1", "user-B", base.plusDays(10), base.plusDays(12), BookingStatus.PENDING);

        Page<Booking> userABookings = bookingRepository.findByUserId("user-A", PageRequest.of(0, 10));
        assertEquals(2, userABookings.getTotalElements());
    }

    @Test
    @DisplayName("Overlap Boundary: Canonical tests matching exact prompt boundary scenarios")
    void testCanonicalBoundaryOverlapCases() {
        // Base Booking A: 2026-10-01 10:00 to 2026-10-03 10:00
        LocalDateTime startA = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime endA = LocalDateTime.of(2026, 10, 3, 10, 0);
        createBooking("BK-A", "veh-100", "user-1", startA, endA, BookingStatus.CONFIRMED);

        // Case 1: 2026-10-03 10:00 to 2026-10-05 10:00 -> ALLOWED (ends exactly when next begins)
        List<Booking> res1 = bookingRepository.findOverlappingBookings(
                "veh-100",
                LocalDateTime.of(2026, 10, 3, 10, 0),
                LocalDateTime.of(2026, 10, 5, 10, 0),
                BLOCKING
        );
        assertTrue(res1.isEmpty(), "Adjacent booking starting at dropoff time must be ALLOWED");

        // Case 2: 2026-10-02 10:00 to 2026-10-04 10:00 -> REJECTED (overlaps middle)
        List<Booking> res2 = bookingRepository.findOverlappingBookings(
                "veh-100",
                LocalDateTime.of(2026, 10, 2, 10, 0),
                LocalDateTime.of(2026, 10, 4, 10, 0),
                BLOCKING
        );
        assertEquals(1, res2.size(), "Partial overlap must be REJECTED");

        // Case 3: 2026-09-30 10:00 to 2026-10-01 10:00 -> ALLOWED (ends exactly when A begins)
        List<Booking> res3 = bookingRepository.findOverlappingBookings(
                "veh-100",
                LocalDateTime.of(2026, 9, 30, 10, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0),
                BLOCKING
        );
        assertTrue(res3.isEmpty(), "Adjacent booking ending at pickup time must be ALLOWED");

        // Case 4: 2026-10-01 09:59 to 2026-10-01 10:01 -> REJECTED (1 minute overlap with start)
        List<Booking> res4 = bookingRepository.findOverlappingBookings(
                "veh-100",
                LocalDateTime.of(2026, 10, 1, 9, 59),
                LocalDateTime.of(2026, 10, 1, 10, 1),
                BLOCKING
        );
        assertEquals(1, res4.size(), "1-minute boundary overlap with pickup time must be REJECTED");

        // Case 5: 2026-10-03 09:59 to 2026-10-03 10:01 -> REJECTED (1 minute overlap with end)
        List<Booking> res5 = bookingRepository.findOverlappingBookings(
                "veh-100",
                LocalDateTime.of(2026, 10, 3, 9, 59),
                LocalDateTime.of(2026, 10, 3, 10, 1),
                BLOCKING
        );
        assertEquals(1, res5.size(), "1-minute boundary overlap with return time must be REJECTED");

        // Case 6: Contained completely inside (2026-10-01 12:00 to 2026-10-02 12:00) -> REJECTED
        List<Booking> res6 = bookingRepository.findOverlappingBookings(
                "veh-100",
                LocalDateTime.of(2026, 10, 1, 12, 0),
                LocalDateTime.of(2026, 10, 2, 12, 0),
                BLOCKING
        );
        assertEquals(1, res6.size(), "Contained window must be REJECTED");

        // Case 7: Surrounding completely (2026-09-30 08:00 to 2026-10-04 18:00) -> REJECTED
        List<Booking> res7 = bookingRepository.findOverlappingBookings(
                "veh-100",
                LocalDateTime.of(2026, 9, 30, 8, 0),
                LocalDateTime.of(2026, 10, 4, 18, 0),
                BLOCKING
        );
        assertEquals(1, res7.size(), "Surrounding window must be REJECTED");
    }

    @Test
    @DisplayName("Repository: CANCELLED, RETURNED, and COMPLETED bookings do not block availability")
    void testNonBlockingStatusesInQuery() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 1, 10, 0);
        createBooking("BK-CANCELLED", "veh-200", "user-1", base, base.plusDays(3), BookingStatus.CANCELLED);
        createBooking("BK-COMPLETED", "veh-200", "user-1", base.plusDays(5), base.plusDays(8), BookingStatus.COMPLETED);
        createBooking("BK-RETURNED", "veh-200", "user-1", base.plusDays(10), base.plusDays(13), BookingStatus.RETURNED);

        // Check overlapping window on CANCELLED -> Allowed
        List<Booking> res1 = bookingRepository.findOverlappingBookings("veh-200", base.plusDays(1), base.plusDays(2), BLOCKING);
        assertTrue(res1.isEmpty());

        // Check overlapping window on COMPLETED -> Allowed
        List<Booking> res2 = bookingRepository.findOverlappingBookings("veh-200", base.plusDays(6), base.plusDays(7), BLOCKING);
        assertTrue(res2.isEmpty());

        // Check overlapping window on RETURNED -> Allowed
        List<Booking> res3 = bookingRepository.findOverlappingBookings("veh-200", base.plusDays(11), base.plusDays(12), BLOCKING);
        assertTrue(res3.isEmpty());
    }
}
