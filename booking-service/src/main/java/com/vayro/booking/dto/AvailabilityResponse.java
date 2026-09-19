package com.vayro.booking.dto;

import java.time.LocalDateTime;

public class AvailabilityResponse {

    private String vehicleId;
    private boolean available;
    private LocalDateTime pickupDateTime;
    private LocalDateTime returnDateTime;
    private String message;

    public AvailabilityResponse() {
    }

    public AvailabilityResponse(String vehicleId, boolean available, String message) {
        this.vehicleId = vehicleId;
        this.available = available;
        this.message = message;
    }

    public AvailabilityResponse(String vehicleId, boolean available, LocalDateTime pickupDateTime,
                                LocalDateTime returnDateTime, String message) {
        this.vehicleId = vehicleId;
        this.available = available;
        this.pickupDateTime = pickupDateTime;
        this.returnDateTime = returnDateTime;
        this.message = message;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public LocalDateTime getPickupDateTime() {
        return pickupDateTime;
    }

    public void setPickupDateTime(LocalDateTime pickupDateTime) {
        this.pickupDateTime = pickupDateTime;
    }

    public LocalDateTime getReturnDateTime() {
        return returnDateTime;
    }

    public void setReturnDateTime(LocalDateTime returnDateTime) {
        this.returnDateTime = returnDateTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
