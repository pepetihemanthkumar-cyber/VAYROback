package com.vayro.booking.repository;

import com.vayro.booking.entity.Booking;
import com.vayro.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByBookingReference(String bookingReference);

    Optional<Booking> findByPaymentOrderId(String paymentOrderId);

    Page<Booking> findByUserId(String userId, Pageable pageable);

    Page<Booking> findByVehicleId(String vehicleId, Pageable pageable);

    List<Booking> findByVehicleIdAndStatusIn(String vehicleId, Collection<BookingStatus> statuses);

    /**
     * Find active bookings for the same vehicle that overlap with the requested [pickupDateTime, returnDateTime] window.
     * Core overlap condition: existing.pickupDateTime < requestedReturn AND existing.returnDateTime > requestedPickup.
     */
    @Query("SELECT b FROM Booking b WHERE b.vehicleId = :vehicleId " +
           "AND b.status IN (:blockingStatuses) " +
           "AND b.pickupDateTime < :returnDateTime " +
           "AND b.returnDateTime > :pickupDateTime")
    List<Booking> findOverlappingBookings(
            @Param("vehicleId") String vehicleId,
            @Param("pickupDateTime") LocalDateTime pickupDateTime,
            @Param("returnDateTime") LocalDateTime returnDateTime,
            @Param("blockingStatuses") Collection<BookingStatus> blockingStatuses
    );

    @Query("SELECT b FROM Booking b WHERE b.vehicleId = :vehicleId " +
           "AND b.id <> :excludeId " +
           "AND b.status IN (:blockingStatuses) " +
           "AND b.pickupDateTime < :returnDateTime " +
           "AND b.returnDateTime > :pickupDateTime")
    List<Booking> findOverlappingBookingsExcludingId(
            @Param("vehicleId") String vehicleId,
            @Param("excludeId") Long excludeId,
            @Param("pickupDateTime") LocalDateTime pickupDateTime,
            @Param("returnDateTime") LocalDateTime returnDateTime,
            @Param("blockingStatuses") Collection<BookingStatus> blockingStatuses
    );
}
