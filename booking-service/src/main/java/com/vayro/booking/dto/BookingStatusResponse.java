package com.vayro.booking.dto;

import com.vayro.booking.entity.BookingStatus;
import com.vayro.booking.entity.PaymentStatus;

public class BookingStatusResponse {

    private Long id;
    private String bookingReference;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private String message;

    public BookingStatusResponse() {
    }

    public BookingStatusResponse(Long id, String bookingReference, BookingStatus status, PaymentStatus paymentStatus, String message) {
        this.id = id;
        this.bookingReference = bookingReference;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
