package com.vayro.vehicles.dto;

import com.vayro.vehicles.entity.VehicleStatus;
import jakarta.validation.constraints.NotNull;

public class VehicleStatusRequest {

    @NotNull(message = "Status is required")
    private VehicleStatus status;

    public VehicleStatusRequest() {
    }

    public VehicleStatusRequest(VehicleStatus status) {
        this.status = status;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }
}
