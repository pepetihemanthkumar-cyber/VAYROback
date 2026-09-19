package com.vayro.booking.controller;

import com.vayro.booking.dto.*;
import com.vayro.booking.entity.BookingStatus;
import com.vayro.booking.security.AuthenticatedUser;
import com.vayro.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Create a new booking reservation.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token
    ) {
        BookingResponse response = bookingService.createBooking(request, currentUser, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve the authenticated customer's own bookings.
     */
    @GetMapping("/my")
    public ResponseEntity<PageResponse<BookingSummaryResponse>> getMyBookings(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<BookingSummaryResponse> response = bookingService.getMyBookings(currentUser, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve booking by numeric ID (owner or ADMIN).
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        BookingResponse response = bookingService.getBookingById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve booking by unique reference (owner or ADMIN).
     */
    @GetMapping("/reference/{reference}")
    public ResponseEntity<BookingResponse> getBookingByReference(
            @PathVariable String reference,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        BookingResponse response = bookingService.getBookingByReference(reference, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Administrator query endpoint for fleet reservations with multi-field search and pagination.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<BookingResponse>> getAllBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String vehicleId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<BookingResponse> response = bookingService.getAllBookings(status, vehicleId, userId, search, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel an eligible reservation (Owner or ADMIN).
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) BookingCancelRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token
    ) {
        BookingResponse response = bookingService.cancelBooking(id, request, currentUser, token);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirm a pending reservation (ADMIN only).
     */
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token
    ) {
        BookingResponse response = bookingService.confirmBooking(id, currentUser, token);
        return ResponseEntity.ok(response);
    }

    /**
     * Activate a confirmed booking upon customer vehicle pickup (ADMIN only).
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> activateBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token
    ) {
        BookingResponse response = bookingService.activateBooking(id, currentUser, token);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark vehicle as returned after drop-off (ADMIN only).
     */
    @PatchMapping("/{id}/return")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> returnBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token
    ) {
        BookingResponse response = bookingService.returnBooking(id, currentUser, token);
        return ResponseEntity.ok(response);
    }

    /**
     * Finalize and complete reservation lifecycle (ADMIN only).
     */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> completeBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token
    ) {
        BookingResponse response = bookingService.completeBooking(id, currentUser, token);
        return ResponseEntity.ok(response);
    }

    /**
     * Check vehicle date/time availability before reservation creation (Public / Authenticated).
     */
    @GetMapping("/availability/{vehicleId}")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @PathVariable String vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime pickupDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime returnDateTime
    ) {
        AvailabilityResponse response = bookingService.checkAvailability(vehicleId, pickupDateTime, returnDateTime);
        return ResponseEntity.ok(response);
    }
}
