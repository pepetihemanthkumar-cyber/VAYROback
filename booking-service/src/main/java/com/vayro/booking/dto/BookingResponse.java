package com.vayro.booking.dto;

import com.vayro.booking.entity.Booking;
import com.vayro.booking.entity.BookingStatus;
import com.vayro.booking.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingResponse {

    private Long id;
    private String bookingReference;
    private String vehicleId;
    private String vehicleName;
    private String userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private LocalDateTime pickupDateTime;
    private LocalDateTime returnDateTime;
    private Integer rentalDays;
    private BigDecimal pricePerDay;
    private BigDecimal baseAmount;
    private BigDecimal addOnsAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private PriceBreakdownResponse priceBreakdown;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private String paymentReference;
    private String paymentOrderId;
    private String pickupLocation;
    private String returnLocation;
    private String notes;
    private LocalDateTime returnedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BookingResponse() {
    }

    public static BookingResponse fromEntity(Booking b) {
        if (b == null) return null;
        BookingResponse res = new BookingResponse();
        res.id = b.getId();
        res.bookingReference = b.getBookingReference();
        res.vehicleId = b.getVehicleId();
        res.vehicleName = b.getVehicleName();
        res.userId = b.getUserId();
        res.customerName = b.getCustomerName();
        res.customerEmail = b.getCustomerEmail();
        res.customerPhone = b.getCustomerPhone();
        res.pickupDateTime = b.getPickupDateTime();
        res.returnDateTime = b.getReturnDateTime();
        res.rentalDays = b.getRentalDays();
        res.pricePerDay = b.getPricePerDay();
        res.baseAmount = b.getBaseAmount();
        res.addOnsAmount = b.getAddOnsAmount();
        res.discountAmount = b.getDiscountAmount();
        res.taxAmount = b.getTaxAmount();
        res.totalAmount = b.getTotalAmount();
        res.status = b.getStatus();
        res.paymentStatus = b.getPaymentStatus();
        res.paymentMethod = b.getPaymentMethod();
        res.paymentReference = b.getPaymentReference();
        res.paymentOrderId = b.getPaymentOrderId();
        res.pickupLocation = b.getPickupLocation();
        res.returnLocation = b.getReturnLocation();
        res.notes = b.getNotes();
        res.returnedAt = b.getReturnedAt();
        res.createdAt = b.getCreatedAt();
        res.updatedAt = b.getUpdatedAt();

        res.priceBreakdown = new PriceBreakdownResponse(
                b.getPricePerDay(),
                b.getRentalDays(),
                b.getBaseAmount(),
                b.getAddOnsAmount(),
                b.getDiscountAmount(),
                b.getTaxAmount(),
                b.getTotalAmount(),
                BigDecimal.valueOf(0.18) // standard default breakdown info
        );

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

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getAddOnsAmount() {
        return addOnsAmount;
    }

    public void setAddOnsAmount(BigDecimal addOnsAmount) {
        this.addOnsAmount = addOnsAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PriceBreakdownResponse getPriceBreakdown() {
        return priceBreakdown;
    }

    public void setPriceBreakdown(PriceBreakdownResponse priceBreakdown) {
        this.priceBreakdown = priceBreakdown;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getPaymentOrderId() {
        return paymentOrderId;
    }

    public void setPaymentOrderId(String paymentOrderId) {
        this.paymentOrderId = paymentOrderId;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
