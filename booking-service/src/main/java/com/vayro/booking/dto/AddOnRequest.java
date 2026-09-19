package com.vayro.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AddOnRequest {

    @NotBlank(message = "Add-on name is required")
    private String name;

    @NotNull(message = "Add-on quantity must be at least 1")
    @Min(value = 1, message = "Add-on quantity must be at least 1")
    private Integer quantity = 1;

    @NotNull(message = "Add-on unit price is required")
    @DecimalMin(value = "0.00", message = "Add-on unit price cannot be negative")
    private BigDecimal unitPrice;

    public AddOnRequest() {
        this.quantity = 1;
    }

    public AddOnRequest(String name, Integer quantity, BigDecimal unitPrice) {
        this.name = name;
        this.quantity = quantity != null ? quantity : 1;
        this.unitPrice = unitPrice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setPrice(BigDecimal price) {
        if (this.unitPrice == null) {
            this.unitPrice = price;
        }
    }
}
