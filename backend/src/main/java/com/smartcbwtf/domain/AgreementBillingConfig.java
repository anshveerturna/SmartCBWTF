package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Agreement-scoped billing configuration.
 * 
 * Business Rules:
 * - Base waste allowance: 270 grams per bed per day (included in base rate)
 * - Excess waste above allowance is charged per kg
 * - Only ONE active config per agreement at a time (effective_to IS NULL)
 * - New config auto-expires previous
 * - Cannot modify if agreement is EXPIRED/TERMINATED
 * - All changes audit logged
 */
@Entity
@Table(name = "agreement_billing_config", indexes = {
        @Index(name = "idx_billing_config_agreement", columnList = "agreement_id"),
        @Index(name = "idx_billing_config_active", columnList = "agreement_id, effective_to")
})
public class AgreementBillingConfig {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private Agreement agreement;

    /**
     * Base waste allowance in grams per bed per day.
     * Default: 270 grams (industry standard).
     */
    @Column(name = "base_grams_per_bed_per_day", nullable = false)
    private Integer baseGramsPerBedPerDay = 270;

    /**
     * Base rate charged per bed per day (in currency units).
     * This covers waste up to baseGramsPerBedPerDay.
     */
    @Column(name = "base_rate_per_bed_per_day", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseRatePerBedPerDay;

    /**
     * Date from which this config is effective (inclusive).
     */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /**
     * Date until which this config is effective (inclusive).
     * NULL means currently active.
     */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /**
     * User who created this config.
     */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // ---------- Business Methods ----------

    /**
     * Check if this config is currently active.
     */
    public boolean isActive() {
        return effectiveTo == null;
    }

    /**
     * Expire this config as of today.
     */
    public void expire(LocalDate asOfDate) {
        this.effectiveTo = asOfDate;
    }

    // ---------- Getters and Setters ----------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Agreement getAgreement() {
        return agreement;
    }

    public void setAgreement(Agreement agreement) {
        this.agreement = agreement;
    }

    public Integer getBaseGramsPerBedPerDay() {
        return baseGramsPerBedPerDay;
    }

    public void setBaseGramsPerBedPerDay(Integer baseGramsPerBedPerDay) {
        this.baseGramsPerBedPerDay = baseGramsPerBedPerDay;
    }

    public BigDecimal getBaseRatePerBedPerDay() {
        return baseRatePerBedPerDay;
    }

    public void setBaseRatePerBedPerDay(BigDecimal baseRatePerBedPerDay) {
        this.baseRatePerBedPerDay = baseRatePerBedPerDay;
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

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
