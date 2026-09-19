package com.vayro.booking.dto;

import java.math.BigDecimal;

public class PriceBreakdownResponse {

    private BigDecimal pricePerDay;
    private Integer rentalDays;
    private BigDecimal baseAmount;
    private BigDecimal addOnsAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal taxRate;

    public PriceBreakdownResponse() {
    }

    public PriceBreakdownResponse(BigDecimal pricePerDay, Integer rentalDays, BigDecimal baseAmount,
                                  BigDecimal addOnsAmount, BigDecimal discountAmount, BigDecimal taxAmount,
                                  BigDecimal totalAmount, BigDecimal taxRate) {
        this.pricePerDay = pricePerDay;
        this.rentalDays = rentalDays;
        this.baseAmount = baseAmount;
        this.addOnsAmount = addOnsAmount;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.taxRate = taxRate;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public Integer getRentalDays() {
        return rentalDays;
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

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }
}
