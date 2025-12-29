package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Billing Snapshot - Freezes commercial parameters at billing time.
 * 
 * IMMUTABLE: Never update or delete.
 * Guarantees historical billing correctness even if facility/agreement rates
 * change.
 */
@Entity
@Table(name = "billing_snapshot", indexes = {
        @Index(name = "idx_snapshot_facility", columnList = "facility_id, billing_month")
})
public class BillingSnapshot {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    // From Agreement
    @Column(name = "bed_count", nullable = false)
    private Integer bedCount;

    @Column(name = "base_grams_per_bed_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseGramsPerBedPerDay;

    @Column(name = "base_rate_per_bed_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRatePerBedPerDay;

    @Column(name = "agreement_version", nullable = false)
    private Integer agreementVersion = 1;

    // From Facility
    @Column(name = "excess_rate_per_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal excessRatePerKg;

    @Column(name = "excess_rate_effective_from", nullable = false)
    private LocalDate excessRateEffectiveFrom;

    // Integrity
    @Column(name = "snapshot_hash", nullable = false, length = 64)
    private String snapshotHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Constructor
    public BillingSnapshot() {
        this.id = UUID.randomUUID();
    }

    // Getters
    public UUID getId() {
        return id;
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

    public Integer getBedCount() {
        return bedCount;
    }

    public BigDecimal getBaseGramsPerBedPerDay() {
        return baseGramsPerBedPerDay;
    }

    public BigDecimal getBaseRatePerBedPerDay() {
        return baseRatePerBedPerDay;
    }

    public Integer getAgreementVersion() {
        return agreementVersion;
    }

    public BigDecimal getExcessRatePerKg() {
        return excessRatePerKg;
    }

    public LocalDate getExcessRateEffectiveFrom() {
        return excessRateEffectiveFrom;
    }

    public String getSnapshotHash() {
        return snapshotHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setAgreement(Agreement agreement) {
        this.agreement = agreement;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public void setBillingMonth(LocalDate billingMonth) {
        this.billingMonth = billingMonth;
    }

    public void setBedCount(Integer bedCount) {
        this.bedCount = bedCount;
    }

    public void setBaseGramsPerBedPerDay(BigDecimal baseGramsPerBedPerDay) {
        this.baseGramsPerBedPerDay = baseGramsPerBedPerDay;
    }

    public void setBaseRatePerBedPerDay(BigDecimal baseRatePerBedPerDay) {
        this.baseRatePerBedPerDay = baseRatePerBedPerDay;
    }

    public void setAgreementVersion(Integer agreementVersion) {
        this.agreementVersion = agreementVersion;
    }

    public void setExcessRatePerKg(BigDecimal excessRatePerKg) {
        this.excessRatePerKg = excessRatePerKg;
    }

    public void setExcessRateEffectiveFrom(LocalDate excessRateEffectiveFrom) {
        this.excessRateEffectiveFrom = excessRateEffectiveFrom;
    }

    public void setSnapshotHash(String snapshotHash) {
        this.snapshotHash = snapshotHash;
    }
}
