package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Bill - Operational billing calculation result.
 * 
 * Base calculations are IMMUTABLE after finalization.
 * Adjustments (concessions) can be applied by CBWTF_ADMIN only.
 * Each adjustment creates a BillVersion audit record.
 */
@Entity
@Table(name = "bill", indexes = {
        @Index(name = "idx_bill_facility", columnList = "facility_id, billing_month"),
        @Index(name = "idx_bill_status", columnList = "status")
})
public class Bill {

    /**
     * Bill status enum.
     * FINALIZED_WITH_ADJUSTMENT indicates bill is still final, just with a
     * concession applied.
     */
    public enum Status {
        DRAFT,
        FINALIZED,
        FINALIZED_WITH_ADJUSTMENT
    }

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id")
    private BillingSnapshot snapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    // Aggregated pickup data
    @Column(name = "pickup_weight_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal pickupWeightKg;

    @Column(name = "pickup_event_count", nullable = false)
    private Integer pickupEventCount;

    @Column(name = "pickup_event_hash", nullable = false, length = 64)
    private String pickupEventHash;

    // Calculated amounts (IMMUTABLE after finalization)
    @Column(name = "base_allowance_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal baseAllowanceKg;

    @Column(name = "excess_weight_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal excessWeightKg;

    @Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "excess_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal excessAmount;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "cgst", nullable = false, precision = 12, scale = 2)
    private BigDecimal cgst;

    @Column(name = "sgst", nullable = false, precision = 12, scale = 2)
    private BigDecimal sgst;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // Adjustment fields (for concessions)
    @Column(name = "adjustment_amount", precision = 12, scale = 2)
    private BigDecimal adjustmentAmount;

    @Column(name = "adjustment_reason", length = 500)
    private String adjustmentReason;

    @Column(name = "adjusted_by")
    private UUID adjustedBy;

    @Column(name = "adjusted_at")
    private Instant adjustedAt;

    @Column(name = "bill_version", nullable = false)
    private Integer billVersion = 1;

    @Column(name = "final_payable_amount", precision = 12, scale = 2)
    private BigDecimal finalPayableAmount;

    @Column(nullable = false, length = 30)
    private String status = Status.FINALIZED.name();

    // Billing model snapshot - frozen at bill creation time
    @Column(name = "billing_model", length = 20)
    private String billingModel;

    @Column(name = "snapshot_beds")
    private Integer snapshotBeds;

    @Column(name = "snapshot_monthly_charge", precision = 12, scale = 2)
    private BigDecimal snapshotMonthlyCharge;

    @Column(name = "snapshot_rate_per_bed", precision = 12, scale = 2)
    private BigDecimal snapshotRatePerBed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Constructor
    public Bill() {
        this.id = UUID.randomUUID();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public BillingSnapshot getSnapshot() {
        return snapshot;
    }

    public Agreement getAgreement() {
        return agreement;
    }

    public Facility getFacility() {
        return facility;
    }

    public LocalDate getBillingMonth() {
        return billingMonth;
    }

    public BigDecimal getPickupWeightKg() {
        return pickupWeightKg;
    }

    public Integer getPickupEventCount() {
        return pickupEventCount;
    }

    public String getPickupEventHash() {
        return pickupEventHash;
    }

    public BigDecimal getBaseAllowanceKg() {
        return baseAllowanceKg;
    }

    public BigDecimal getExcessWeightKg() {
        return excessWeightKg;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public BigDecimal getExcessAmount() {
        return excessAmount;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getCgst() {
        return cgst;
    }

    public BigDecimal getSgst() {
        return sgst;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setSnapshot(BillingSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public void setAgreement(Agreement agreement) {
        this.agreement = agreement;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public void setBillingMonth(LocalDate billingMonth) {
        this.billingMonth = billingMonth;
    }

    public void setPickupWeightKg(BigDecimal pickupWeightKg) {
        this.pickupWeightKg = pickupWeightKg;
    }

    public void setPickupEventCount(Integer pickupEventCount) {
        this.pickupEventCount = pickupEventCount;
    }

    public void setPickupEventHash(String pickupEventHash) {
        this.pickupEventHash = pickupEventHash;
    }

    public void setBaseAllowanceKg(BigDecimal baseAllowanceKg) {
        this.baseAllowanceKg = baseAllowanceKg;
    }

    public void setExcessWeightKg(BigDecimal excessWeightKg) {
        this.excessWeightKg = excessWeightKg;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public void setExcessAmount(BigDecimal excessAmount) {
        this.excessAmount = excessAmount;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public void setCgst(BigDecimal cgst) {
        this.cgst = cgst;
    }

    public void setSgst(BigDecimal sgst) {
        this.sgst = sgst;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    // Billing model snapshot getters/setters
    public String getBillingModel() {
        return billingModel;
    }

    public void setBillingModel(String billingModel) {
        this.billingModel = billingModel;
    }

    public Integer getSnapshotBeds() {
        return snapshotBeds;
    }

    public void setSnapshotBeds(Integer snapshotBeds) {
        this.snapshotBeds = snapshotBeds;
    }

    public BigDecimal getSnapshotMonthlyCharge() {
        return snapshotMonthlyCharge;
    }

    public void setSnapshotMonthlyCharge(BigDecimal snapshotMonthlyCharge) {
        this.snapshotMonthlyCharge = snapshotMonthlyCharge;
    }

    public BigDecimal getSnapshotRatePerBed() {
        return snapshotRatePerBed;
    }

    public void setSnapshotRatePerBed(BigDecimal snapshotRatePerBed) {
        this.snapshotRatePerBed = snapshotRatePerBed;
    }

    // Adjustment getters/setters
    public BigDecimal getAdjustmentAmount() {
        return adjustmentAmount;
    }

    public void setAdjustmentAmount(BigDecimal adjustmentAmount) {
        this.adjustmentAmount = adjustmentAmount;
    }

    public String getAdjustmentReason() {
        return adjustmentReason;
    }

    public void setAdjustmentReason(String adjustmentReason) {
        this.adjustmentReason = adjustmentReason;
    }

    public UUID getAdjustedBy() {
        return adjustedBy;
    }

    public void setAdjustedBy(UUID adjustedBy) {
        this.adjustedBy = adjustedBy;
    }

    public Instant getAdjustedAt() {
        return adjustedAt;
    }

    public void setAdjustedAt(Instant adjustedAt) {
        this.adjustedAt = adjustedAt;
    }

    public Integer getBillVersion() {
        return billVersion;
    }

    public void setBillVersion(Integer billVersion) {
        this.billVersion = billVersion;
    }

    public BigDecimal getFinalPayableAmount() {
        return finalPayableAmount;
    }

    public void setFinalPayableAmount(BigDecimal finalPayableAmount) {
        this.finalPayableAmount = finalPayableAmount;
    }

    /**
     * Check if this bill has an adjustment applied.
     */
    public boolean hasAdjustment() {
        return adjustmentAmount != null && adjustmentAmount.compareTo(BigDecimal.ZERO) != 0;
    }
}
