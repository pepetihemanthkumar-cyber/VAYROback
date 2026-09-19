package com.vayro.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingConfirmedNotificationRequest {

    private Long bookingId;

    @NotBlank(message = "Booking reference is required")
    private String bookingReference;

    @NotBlank(message = "Vehicle name is required")
    private String vehicleName;

    private String customerName;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Invalid customer email address")
    private String customerEmail;

    private String customerPhone;

    private LocalDateTime pickupDateTime;
    private String pickupTime;

    private LocalDateTime returnDateTime;
    private String returnTime;

    private String pickupLocation;
    private Integer rentalDays;
    private BigDecimal baseAmount;
    private BigDecimal addOnsAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal securityDeposit;
    private String paymentMethod;
    private String paymentReference;
    private String idempotencyKey;

    public BookingConfirmedNotificationRequest() {
    }

    public BookingConfirmedNotificationRequest(Long bookingId, String bookingReference, String vehicleName,
                                                String customerName, String customerEmail, String customerPhone,
                                                LocalDateTime pickupDateTime, String pickupTime,
                                                LocalDateTime returnDateTime, String returnTime,
                                                String pickupLocation, Integer rentalDays,
                                                BigDecimal totalAmount, BigDecimal securityDeposit,
                                                String idempotencyKey) {
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
        this.pickupLocation = pickupLocation;
        this.rentalDays = rentalDays;
        this.baseAmount = totalAmount;
        this.totalAmount = totalAmount;
        this.securityDeposit = securityDeposit;
        this.idempotencyKey = idempotencyKey;
    }

    public BookingConfirmedNotificationRequest(Long bookingId, String bookingReference, String vehicleName,
                                                String customerName, String customerEmail, String customerPhone,
                                                LocalDateTime pickupDateTime, String pickupTime,
                                                LocalDateTime returnDateTime, String returnTime,
                                                String pickupLocation, Integer rentalDays,
                                                BigDecimal baseAmount, BigDecimal addOnsAmount,
                                                BigDecimal taxAmount, BigDecimal discountAmount,
                                                BigDecimal totalAmount, BigDecimal securityDeposit,
                                                String paymentMethod, String paymentReference,
                                                String idempotencyKey) {
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
        this.pickupLocation = pickupLocation;
        this.rentalDays = rentalDays;
        this.baseAmount = baseAmount;
        this.addOnsAmount = addOnsAmount;
        this.taxAmount = taxAmount;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.securityDeposit = securityDeposit;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
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

    public String getPickupLocation() {
        return pickupLocation != null ? pickupLocation : "VAYRO Hub";
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public Integer getRentalDays() {
        return rentalDays != null ? rentalDays : 1;
    }

    public void setRentalDays(Integer rentalDays) {
        this.rentalDays = rentalDays;
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

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getSecurityDeposit() {
        return securityDeposit;
    }

    public void setSecurityDeposit(BigDecimal securityDeposit) {
        this.securityDeposit = securityDeposit;
    }

    public String getPaymentMethod() {
        return paymentMethod != null ? paymentMethod : "SIMULATED_CARD";
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference != null ? paymentReference : ("PAY-SIM-" + bookingReference);
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
