package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * BillVersion - Immutable audit trail for bill adjustments.
 * 
 * Each time a bill is adjusted (concession applied), a new BillVersion record
 * is created.
 * This provides complete history of all adjustments made to a bill.
 * 
 * IMMUTABLE: NEVER update or delete.
 */
@Entity
@Table(name = "bill_version", indexes = {
        @Index(name = "idx_bill_version_bill", columnList = "bill_id"),
        @Index(name = "idx_bill_version_adjusted_at", columnList = "adjusted_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_bill_version", columnNames = { "bill_id", "version" })
})
public class BillVersion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "original_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalTotal;

    @Column(name = "adjustment_amount", precision = 12, scale = 2)
    private BigDecimal adjustmentAmount;

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "adjustment_reason", length = 500)
    private String adjustmentReason;

    @Column(name = "adjusted_by", nullable = false)
    private UUID adjustedBy;

    @Column(name = "adjusted_at", nullable = false)
    private Instant adjustedAt;

    // Constructor
    public BillVersion() {
        this.id = UUID.randomUUID();
    }

    /**
     * Factory method to create a new bill version from an adjustment.
     */
    public static BillVersion fromAdjustment(
            Bill bill,
            BigDecimal adjustmentAmount,
            String reason,
            UUID adjustedBy) {
        BillVersion version = new BillVersion();
        version.setBill(bill);
        version.setVersion(bill.getBillVersion());
        version.setOriginalTotal(bill.getTotalAmount());
        version.setAdjustmentAmount(adjustmentAmount);
        version.setFinalAmount(bill.getTotalAmount().add(adjustmentAmount)); // adjustmentAmount is negative
        version.setAdjustmentReason(reason);
        version.setAdjustedBy(adjustedBy);
        version.setAdjustedAt(Instant.now());
        return version;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public Bill getBill() {
        return bill;
    }

    public Integer getVersion() {
        return version;
    }

    public BigDecimal getOriginalTotal() {
        return originalTotal;
    }

    public BigDecimal getAdjustmentAmount() {
        return adjustmentAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public String getAdjustmentReason() {
        return adjustmentReason;
    }

    public UUID getAdjustedBy() {
        return adjustedBy;
    }

    public Instant getAdjustedAt() {
        return adjustedAt;
    }

    // Setters
    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public void setOriginalTotal(BigDecimal originalTotal) {
        this.originalTotal = originalTotal;
    }

    public void setAdjustmentAmount(BigDecimal adjustmentAmount) {
        this.adjustmentAmount = adjustmentAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public void setAdjustmentReason(String adjustmentReason) {
        this.adjustmentReason = adjustmentReason;
    }

    public void setAdjustedBy(UUID adjustedBy) {
        this.adjustedBy = adjustedBy;
    }

    public void setAdjustedAt(Instant adjustedAt) {
        this.adjustedAt = adjustedAt;
    }
}
