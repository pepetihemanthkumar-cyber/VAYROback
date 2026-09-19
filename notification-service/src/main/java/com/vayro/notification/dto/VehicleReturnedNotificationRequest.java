package com.vayro.notification.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class VehicleReturnedNotificationRequest {

    private Long bookingId;

    @NotBlank(message = "Booking reference is required")
    private String bookingReference;

    @NotBlank(message = "Vehicle name is required")
    private String vehicleName;

    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private LocalDateTime pickupDateTime;
    private String pickupTime;
    private LocalDateTime returnDateTime;
    private String returnTime;
    private LocalDateTime returnedAt;
    private String returnTimestamp;
    private String vehicleStatus;
    private String bookingStatus;
    private String idempotencyKey;

    public VehicleReturnedNotificationRequest() {
    }

    public VehicleReturnedNotificationRequest(Long bookingId, String bookingReference, String vehicleName,
                                              String customerName, String customerEmail, LocalDateTime returnedAt,
                                              String returnTimestamp, String vehicleStatus, String idempotencyKey) {
        this.bookingId = bookingId;
        this.bookingReference = bookingReference;
        this.vehicleName = vehicleName;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.returnedAt = returnedAt;
        this.returnTimestamp = returnTimestamp;
        this.vehicleStatus = vehicleStatus;
        this.bookingStatus = "RETURNED";
        this.idempotencyKey = idempotencyKey;
    }

    public VehicleReturnedNotificationRequest(Long bookingId, String bookingReference, String vehicleName,
                                              String customerName, String customerEmail, String customerPhone,
                                              LocalDateTime pickupDateTime, String pickupTime,
                                              LocalDateTime returnDateTime, String returnTime,
                                              LocalDateTime returnedAt, String returnTimestamp,
                                              String vehicleStatus, String bookingStatus, String idempotencyKey) {
        this.bookingId = bookingId;
        this.bookingReference = bookingReference;
        this.vehicleName = vehicleName;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.pickupDateTime = pickupDateTime;
        this.pickupTime = pickupTime;
        this.returnDateTime = returnDateTime;
        this.returnTime = returnTime;
        this.returnedAt = returnedAt;
        this.returnTimestamp = returnTimestamp;
        this.vehicleStatus = vehicleStatus;
        this.bookingStatus = bookingStatus;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public LocalDateTime getPickupDateTime() {
        return pickupDateTime;
    }

    public void setPickupDateTime(LocalDateTime pickupDateTime) {
        this.pickupDateTime = pickupDateTime;
    }

    public String getPickupTime() {
        if (pickupTime != null && !pickupTime.isEmpty()) {
            return pickupTime;
        }
        if (pickupDateTime != null) {
            return pickupDateTime.toString().replace("T", " ");
        }
        return "N/A";
    }

    public void setPickupTime(String pickupTime) {
        this.pickupTime = pickupTime;
    }

    public LocalDateTime getReturnDateTime() {
        return returnDateTime;
    }

    public void setReturnDateTime(LocalDateTime returnDateTime) {
        this.returnDateTime = returnDateTime;
    }

    public String getReturnTime() {
        if (returnTime != null && !returnTime.isEmpty()) {
            return returnTime;
        }
        if (returnDateTime != null) {
            return returnDateTime.toString().replace("T", " ");
        }
        return "N/A";
    }

    public void setReturnTime(String returnTime) {
        this.returnTime = returnTime;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public String getReturnTimestamp() {
        if (returnTimestamp != null && !returnTimestamp.isEmpty()) {
            return returnTimestamp;
        }
        if (returnedAt != null) {
            return returnedAt.toString().replace("T", " ");
        }
        return LocalDateTime.now().toString().replace("T", " ");
    }

    public void setReturnTimestamp(String returnTimestamp) {
        this.returnTimestamp = returnTimestamp;
    }

    public String getVehicleStatus() {
        return vehicleStatus != null ? vehicleStatus : "AVAILABLE";
    }

    public void setVehicleStatus(String vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }

    public String getBookingStatus() {
        return bookingStatus != null ? bookingStatus : "RETURNED";
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
