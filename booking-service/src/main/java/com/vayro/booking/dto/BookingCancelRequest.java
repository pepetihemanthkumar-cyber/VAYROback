package com.vayro.booking.dto;

public class BookingCancelRequest {

    private String reason;

    public BookingCancelRequest() {
    }

    public BookingCancelRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
