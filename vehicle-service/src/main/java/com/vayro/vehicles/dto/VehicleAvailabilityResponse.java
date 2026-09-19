package com.vayro.vehicles.dto;

import com.vayro.vehicles.entity.VehicleStatus;

public class VehicleAvailabilityResponse {

    private String vehicleId;
    private VehicleStatus status;
    private boolean available;
    private String message;

    public VehicleAvailabilityResponse() {
    }

    public VehicleAvailabilityResponse(String vehicleId, VehicleStatus status, boolean available, String message) {
        this.vehicleId = vehicleId;
        this.status = status;
        this.available = available;
        this.message = message;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
