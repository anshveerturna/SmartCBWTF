package com.smartcbwtf.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateConsumableRequest {
    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    private static final String OPTIONAL_HSN_PATTERN = "^$|^[A-Za-z0-9.-]+$";
    private static final String REFERENCE_TYPE_PATTERN = "^$|PER_100_BEDS_PER_YEAR|PER_MONTH|FIXED";

    @NotBlank(message = "Category is required")
    @Pattern(regexp = UUID_PATTERN, message = "Category ID must be a valid UUID")
    private String categoryId;

    @NotBlank(message = "Consumable code is required")
    @Size(max = 50, message = "Consumable code must be 50 characters or less")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Consumable code contains invalid characters")
    private String consumableCode;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be 200 characters or less")
    private String name;

    @Size(max = 2000, message = "Description must be 2000 characters or less")
    private String description;

    @Size(max = 20, message = "HSN code must be 20 characters or less")
    @Pattern(regexp = OPTIONAL_HSN_PATTERN, message = "HSN code contains invalid characters")
    private String hsnCode;

    @NotBlank(message = "Unit of measure is required")
    @Size(max = 50, message = "Unit of measure must be 50 characters or less")
    private String unitOfMeasure;

    @DecimalMin(value = "0.00", message = "Initial price cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Initial price must fit 10 digits and 2 decimals")
    private BigDecimal initialPrice;

    @DecimalMin(value = "0.00", message = "GST rate cannot be negative")
    @DecimalMax(value = "100.00", message = "GST rate cannot exceed 100")
    @Digits(integer = 3, fraction = 2, message = "GST rate must fit 3 digits and 2 decimals")
    private BigDecimal gstRate;

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Price effective date must be yyyy-MM-dd")
    private String priceEffectiveFrom;

    @Pattern(regexp = REFERENCE_TYPE_PATTERN, message = "Reference type must be PER_100_BEDS_PER_YEAR, PER_MONTH, or FIXED")
    private String referenceType;

    @DecimalMin(value = "0.00", inclusive = false, message = "Reference quantity must be positive")
    @Digits(integer = 8, fraction = 2, message = "Reference quantity must fit 8 digits and 2 decimals")
    private BigDecimal referenceQuantity;

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getConsumableCode() {
        return consumableCode;
    }

    public void setConsumableCode(String consumableCode) {
        this.consumableCode = consumableCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHsnCode() {
        return hsnCode;
    }

    public void setHsnCode(String hsnCode) {
        this.hsnCode = hsnCode;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getInitialPrice() {
        return initialPrice;
    }

    public void setInitialPrice(BigDecimal initialPrice) {
        this.initialPrice = initialPrice;
    }

    public BigDecimal getGstRate() {
        return gstRate;
    }

    public void setGstRate(BigDecimal gstRate) {
        this.gstRate = gstRate;
    }

    public String getPriceEffectiveFrom() {
        return priceEffectiveFrom;
    }

    public void setPriceEffectiveFrom(String priceEffectiveFrom) {
        this.priceEffectiveFrom = priceEffectiveFrom;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public BigDecimal getReferenceQuantity() {
        return referenceQuantity;
    }

    public void setReferenceQuantity(BigDecimal referenceQuantity) {
        this.referenceQuantity = referenceQuantity;
    }
}
