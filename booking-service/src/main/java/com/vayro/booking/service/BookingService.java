package com.vayro.booking.service;

import com.vayro.booking.dto.*;
import com.vayro.booking.entity.BookingStatus;
import com.vayro.booking.security.AuthenticatedUser;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface BookingService {

    BookingResponse createBooking(BookingCreateRequest request, AuthenticatedUser currentUser, String authToken);

    PageResponse<BookingSummaryResponse> getMyBookings(AuthenticatedUser currentUser, Pageable pageable);

    BookingResponse getBookingById(Long id, AuthenticatedUser currentUser);

    BookingResponse getBookingByReference(String reference, AuthenticatedUser currentUser);

    PageResponse<BookingResponse> getAllBookings(BookingStatus status, String vehicleId, String userId, String search, Pageable pageable);

    BookingResponse cancelBooking(Long id, BookingCancelRequest request, AuthenticatedUser currentUser, String authToken);

    BookingResponse confirmBooking(Long id, AuthenticatedUser currentUser, String authToken);

    BookingResponse activateBooking(Long id, AuthenticatedUser currentUser, String authToken);

    BookingResponse returnBooking(Long id, AuthenticatedUser currentUser, String authToken);

    BookingResponse completeBooking(Long id, AuthenticatedUser currentUser, String authToken);

    AvailabilityResponse checkAvailability(String vehicleId, LocalDateTime pickupDateTime, LocalDateTime returnDateTime);
}
