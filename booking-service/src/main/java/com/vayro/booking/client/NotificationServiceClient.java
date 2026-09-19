package com.vayro.booking.client;

import com.vayro.booking.entity.Booking;

public interface NotificationServiceClient {

    /**
     * Asynchronously sends a booking confirmation HTML email with PDF invoice to the customer.
     * Guaranteed non-blocking and zero-rollback (errors are logged safely).
     */
    void sendBookingConfirmedNotificationAsync(Booking booking);

    /**
     * Asynchronously sends a vehicle return HTML email notification to the configured admin.
     * Guaranteed non-blocking and zero-rollback (errors are logged safely).
     */
    void sendVehicleReturnedAdminNotificationAsync(Booking booking);
}
