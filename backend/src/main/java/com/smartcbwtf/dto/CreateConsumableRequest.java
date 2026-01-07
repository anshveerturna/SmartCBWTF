package com.smartcbwtf.dto;

import java.math.BigDecimal;

public class CreateConsumableRequest {
    private String categoryId;
    private String consumableCode;
    private String name;
    private String description;
    private String hsnCode;
    private String unitOfMeasure;
    private BigDecimal initialPrice;
    private BigDecimal gstRate;
    private String priceEffectiveFrom;
    private String referenceType;
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
