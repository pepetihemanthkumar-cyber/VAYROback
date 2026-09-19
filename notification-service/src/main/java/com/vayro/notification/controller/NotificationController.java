package com.vayro.notification.controller;

import com.vayro.notification.dto.BookingConfirmedNotificationRequest;
import com.vayro.notification.dto.NotificationResponse;
import com.vayro.notification.dto.VehicleReturnedNotificationRequest;
import com.vayro.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/booking-confirmed")
    public ResponseEntity<NotificationResponse> handleBookingConfirmed(
            @Valid @RequestBody BookingConfirmedNotificationRequest request
    ) {
        NotificationResponse response = notificationService.processBookingConfirmedNotification(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vehicle-returned")
    public ResponseEntity<NotificationResponse> handleVehicleReturned(
            @Valid @RequestBody VehicleReturnedNotificationRequest request
    ) {
        NotificationResponse response = notificationService.processVehicleReturnedNotification(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{idempotencyKey}")
    public ResponseEntity<NotificationResponse> getStatus(
            @PathVariable String idempotencyKey
    ) {
        NotificationResponse response = notificationService.getNotificationByIdempotencyKey(idempotencyKey);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/booking/{bookingReference}")
    public ResponseEntity<List<NotificationResponse>> getBookingNotifications(
            @PathVariable String bookingReference
    ) {
        List<NotificationResponse> list = notificationService.getNotificationsByBookingReference(bookingReference);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/latest/{bookingReference}")
    public ResponseEntity<NotificationResponse> getLatestNotification(
            @PathVariable String bookingReference
    ) {
        NotificationResponse response = notificationService.getLatestNotificationForBooking(bookingReference);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend/{bookingReference}")
    public ResponseEntity<NotificationResponse> resendNotification(
            @PathVariable String bookingReference
    ) {
        NotificationResponse response = notificationService.resendBookingConfirmedNotification(bookingReference);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invoice/{bookingReference}")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable String bookingReference
    ) {
        byte[] pdfBytes = notificationService.getPdfInvoiceByBookingReference(bookingReference);
        if (pdfBytes == null || pdfBytes.length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "VAYRO-Invoice-" + bookingReference + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/test")
    public ResponseEntity<NotificationResponse> testEmailNotification(
            @RequestParam(required = false, defaultValue = "test@vayro.com") String email
    ) {
        BookingConfirmedNotificationRequest req = new BookingConfirmedNotificationRequest();
        req.setBookingReference("TEST-" + System.currentTimeMillis());
        req.setVehicleName("Test Mercedes Benz");
        req.setCustomerName("Test User");
        req.setCustomerEmail(email);
        req.setPickupDateTime(LocalDateTime.now().plusDays(1));
        req.setReturnDateTime(LocalDateTime.now().plusDays(3));
        req.setPickupLocation("VAYRO Airport Hub");
        req.setRentalDays(2);
        req.setTotalAmount(new java.math.BigDecimal("4500.00"));

        NotificationResponse res = notificationService.processBookingConfirmedNotification(req);
        return ResponseEntity.ok(res);
    }
}
