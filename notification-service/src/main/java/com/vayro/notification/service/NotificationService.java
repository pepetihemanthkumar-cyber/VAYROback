package com.vayro.notification.service;

import com.vayro.notification.dto.BookingConfirmedNotificationRequest;
import com.vayro.notification.dto.NotificationResponse;
import com.vayro.notification.dto.VehicleReturnedNotificationRequest;

import java.util.List;

public interface NotificationService {

    /**
     * Dispatches booking confirmation HTML email with PDF invoice to the customer's authenticated email.
     * Guaranteed idempotent by booking reference.
     */
    NotificationResponse processBookingConfirmedNotification(BookingConfirmedNotificationRequest request);

    /**
     * Dispatches vehicle return notification HTML email to the server-configured admin email address.
     * Guaranteed idempotent by booking reference.
     */
    NotificationResponse processVehicleReturnedNotification(VehicleReturnedNotificationRequest request);

    /**
     * Explicit resend of booking confirmation email.
     */
    NotificationResponse resendBookingConfirmedNotification(String bookingReference);

    /**
     * Look up notification status by idempotency key.
     */
    NotificationResponse getNotificationByIdempotencyKey(String idempotencyKey);

    /**
     * Look up all notification history for a booking reference.
     */
    List<NotificationResponse> getNotificationsByBookingReference(String bookingReference);

    /**
     * Look up latest notification status for a booking reference.
     */
    NotificationResponse getLatestNotificationForBooking(String bookingReference);

    /**
     * Generates or fetches stored PDF invoice for a booking reference.
     */
    byte[] getPdfInvoiceByBookingReference(String bookingReference);
}
