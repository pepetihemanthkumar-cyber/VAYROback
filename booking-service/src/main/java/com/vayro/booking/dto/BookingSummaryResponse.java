package com.vayro.booking.dto;

import com.vayro.booking.entity.Booking;
import com.vayro.booking.entity.BookingStatus;
import com.vayro.booking.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingSummaryResponse {

    private Long id;
    private String bookingReference;
    private String vehicleId;
    private String vehicleName;
    private String userId;
    private String customerName;
    private LocalDateTime pickupDateTime;
    private LocalDateTime returnDateTime;
    private Integer rentalDays;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private String pickupLocation;
    private String returnLocation;
    private LocalDateTime createdAt;

    public BookingSummaryResponse() {
    }

    public static BookingSummaryResponse fromEntity(Booking b) {
        if (b == null) return null;
        BookingSummaryResponse res = new BookingSummaryResponse();
        res.id = b.getId();
        res.bookingReference = b.getBookingReference();
        res.vehicleId = b.getVehicleId();
        res.vehicleName = b.getVehicleName();
        res.userId = b.getUserId();
        res.customerName = b.getCustomerName();
        res.pickupDateTime = b.getPickupDateTime();
        res.returnDateTime = b.getReturnDateTime();
        res.rentalDays = b.getRentalDays();
        res.totalAmount = b.getTotalAmount();
        res.status = b.getStatus();
        res.paymentStatus = b.getPaymentStatus();
        res.pickupLocation = b.getPickupLocation();
        res.returnLocation = b.getReturnLocation();
        res.createdAt = b.getCreatedAt();
        return res;
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

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
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

    public Integer getRentalDays() {
        return rentalDays;
    }

    public void setRentalDays(Integer rentalDays) {
        this.rentalDays = rentalDays;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getReturnLocation() {
        return returnLocation;
    }

    public void setReturnLocation(String returnLocation) {
        this.returnLocation = returnLocation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
