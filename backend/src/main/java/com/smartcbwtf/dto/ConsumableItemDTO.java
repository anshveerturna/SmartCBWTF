package com.smartcbwtf.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ConsumableItemDTO {
    private String id;
    private String consumableCode;
    private String name;
    private String description;
    private String hsnCode;
    private String unitOfMeasure;
    private String imageUrl;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String categoryId;
    private String categoryName;

    private BigDecimal activePrice;
    private BigDecimal activeGstRate;
    private LocalDate priceEffectiveFrom;

    private String referenceType;
    private BigDecimal referenceQuantity;
    private String referenceDisplayText;

    private List<PricingHistoryItem> pricingHistory;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
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

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getActivePrice() {
        return activePrice;
    }

    public void setActivePrice(BigDecimal activePrice) {
        this.activePrice = activePrice;
    }

    public BigDecimal getActiveGstRate() {
        return activeGstRate;
    }

    public void setActiveGstRate(BigDecimal activeGstRate) {
        this.activeGstRate = activeGstRate;
    }

    public LocalDate getPriceEffectiveFrom() {
        return priceEffectiveFrom;
    }

    public void setPriceEffectiveFrom(LocalDate priceEffectiveFrom) {
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

    public String getReferenceDisplayText() {
        return referenceDisplayText;
    }

    public void setReferenceDisplayText(String referenceDisplayText) {
        this.referenceDisplayText = referenceDisplayText;
    }

    public List<PricingHistoryItem> getPricingHistory() {
        return pricingHistory;
    }

    public void setPricingHistory(List<PricingHistoryItem> pricingHistory) {
        this.pricingHistory = pricingHistory;
    }

    public static class PricingHistoryItem {
        private String id;
        private BigDecimal pricePerUnit;
        private BigDecimal gstRate;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private boolean isActive;
        private LocalDateTime createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public BigDecimal getPricePerUnit() {
            return pricePerUnit;
        }

        public void setPricePerUnit(BigDecimal pricePerUnit) {
            this.pricePerUnit = pricePerUnit;
        }

        public BigDecimal getGstRate() {
            return gstRate;
        }

        public void setGstRate(BigDecimal gstRate) {
            this.gstRate = gstRate;
        }

        public LocalDate getEffectiveFrom() {
            return effectiveFrom;
        }

        public void setEffectiveFrom(LocalDate effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
        }

        public LocalDate getEffectiveTo() {
            return effectiveTo;
        }

        public void setEffectiveTo(LocalDate effectiveTo) {
            this.effectiveTo = effectiveTo;
        }

        public boolean isIsActive() {
            return isActive;
        }

        public void setIsActive(boolean isActive) {
            this.isActive = isActive;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}
