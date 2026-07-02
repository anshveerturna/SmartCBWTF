package com.smartcbwtf.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "consumable_quantity_reference")
public class ConsumableQuantityReference {

    public enum ReferenceType {
        PER_100_BEDS_PER_YEAR,
        PER_MONTH,
        FIXED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumable_item_id", nullable = false, unique = true)
    private ConsumableItem consumableItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30)
    private ReferenceType referenceType;

    @Column(name = "reference_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal referenceQuantity;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ConsumableItem getConsumableItem() {
        return consumableItem;
    }

    public void setConsumableItem(ConsumableItem consumableItem) {
        this.consumableItem = consumableItem;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(ReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public BigDecimal getReferenceQuantity() {
        return referenceQuantity;
    }

    public void setReferenceQuantity(BigDecimal referenceQuantity) {
        this.referenceQuantity = referenceQuantity;
    }
}
