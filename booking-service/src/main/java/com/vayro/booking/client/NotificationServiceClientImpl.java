package com.vayro.booking.client;

import com.vayro.booking.entity.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationServiceClientImpl implements NotificationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceClientImpl.class);
    private static final String NOTIFICATION_SERVICE_BASE = "http://NOTIFICATION-SERVICE";

    private final RestClient restClient;

    public NotificationServiceClientImpl(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(NOTIFICATION_SERVICE_BASE).build();
    }

    @Override
    @Async
    public void sendBookingConfirmedNotificationAsync(Booking booking) {
        if (booking == null) {
            log.warn("Cannot send booking confirmation: booking entity is null.");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("bookingId", booking.getId());
            payload.put("bookingReference", booking.getBookingReference());
            payload.put("vehicleName", booking.getVehicleName());
            payload.put("customerName", booking.getCustomerName());
            payload.put("customerEmail", booking.getCustomerEmail());
            payload.put("customerPhone", booking.getCustomerPhone());
            payload.put("pickupDateTime", booking.getPickupDateTime());
            payload.put("returnDateTime", booking.getReturnDateTime());
            payload.put("pickupLocation", booking.getPickupLocation());
            payload.put("rentalDays", booking.getRentalDays());
            payload.put("baseAmount", booking.getBaseAmount());
            payload.put("addOnsAmount", booking.getAddOnsAmount());
            payload.put("taxAmount", booking.getTaxAmount());
            payload.put("discountAmount", booking.getDiscountAmount());
            payload.put("totalAmount", booking.getTotalAmount());
            payload.put("paymentMethod", booking.getPaymentMethod());
            payload.put("paymentReference", booking.getPaymentReference() != null ? booking.getPaymentReference() : ("PAY-SIM-" + booking.getBookingReference()));
            payload.put("idempotencyKey", "BOOKING_CONFIRMED:" + booking.getBookingReference());

            restClient.post()
                    .uri("/api/notifications/booking-confirmed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Dispatched booking confirmation email notification request for reference: {} to recipient email: {}",
                    booking.getBookingReference(), booking.getCustomerEmail());
        } catch (Exception e) {
            // Failure must NOT impact booking transaction
            log.warn("Notification Service dispatch failed for booking {}: {}", booking.getBookingReference(), e.getMessage());
        }
    }

    @Override
    @Async
    public void sendVehicleReturnedAdminNotificationAsync(Booking booking) {
        if (booking == null) {
            log.warn("Cannot send vehicle return notification: booking entity is null.");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("bookingId", booking.getId());
            payload.put("bookingReference", booking.getBookingReference());
            payload.put("vehicleName", booking.getVehicleName());
            payload.put("customerName", booking.getCustomerName());
            payload.put("customerEmail", booking.getCustomerEmail());
            payload.put("customerPhone", booking.getCustomerPhone());
            payload.put("pickupDateTime", booking.getPickupDateTime());
            payload.put("returnDateTime", booking.getReturnDateTime());
            payload.put("returnedAt", booking.getReturnedAt());
            payload.put("vehicleStatus", "AVAILABLE");
            payload.put("bookingStatus", booking.getStatus() != null ? booking.getStatus().name() : "RETURNED");
            payload.put("idempotencyKey", "VEHICLE_RETURNED:" + booking.getBookingReference());

            restClient.post()
                    .uri("/api/notifications/vehicle-returned")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Dispatched vehicle return admin email notification request for reference: {}", booking.getBookingReference());
        } catch (Exception e) {
            // Failure must NOT impact return transaction
            log.warn("Notification Service dispatch failed for vehicle return {}: {}", booking.getBookingReference(), e.getMessage());
        }
    }
}
