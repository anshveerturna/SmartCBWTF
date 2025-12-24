package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable snapshot of Agreement at a point in time.
 * Used for historical accuracy in invoices, reports, and disputes.
 * 
 * Even if rates, terms, or status change later, the snapshot preserves
 * the exact state when the document was generated.
 */
@Entity
@Table(name = "agreement_snapshot", indexes = {
        @Index(name = "idx_agreement_snapshot_agreement", columnList = "agreement_id")
})
public class AgreementSnapshot {

    public enum SnapshotReason {
        INVOICE_GENERATED,
        CPCB_REPORT,
        EXPORT_JOB,
        DISPUTE_OPENED
    }

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "agreement_id", nullable = false)
    private UUID agreementId;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt = Instant.now();

    // Frozen agreement values
    @Column(name = "agreement_number", nullable = false)
    private String agreementNumber;

    // Frozen HCF values
    @Column(name = "hcf_id", nullable = false)
    private UUID hcfId;

    @Column(name = "hcf_name", nullable = false)
    private String hcfName;

    @Column(name = "hcf_gst")
    private String hcfGst;

    @Column(name = "hcf_pan")
    private String hcfPan;

    @Column(name = "hcf_address")
    private String hcfAddress;

    @Column(name = "hcf_beds")
    private Integer hcfBeds;

    // Frozen facility values
    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;

    @Column(name = "facility_name", nullable = false)
    private String facilityName;

    // Frozen billing
    @Column(name = "per_bed_per_day_rate", nullable = false)
    private BigDecimal perBedPerDayRate;

    // Frozen terms
    @Column(name = "terms_text", columnDefinition = "TEXT")
    private String termsText;

    // Frozen status
    @Column(nullable = false)
    private String status;

    @Column(name = "dues_status")
    private String duesStatus;

    // Why this snapshot was created
    @Column(name = "snapshot_reason", nullable = false)
    private String snapshotReason;

    @Column(name = "created_by")
    private UUID createdBy;

    // ---------- Getters and Setters ----------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(UUID agreementId) {
        this.agreementId = agreementId;
    }

    public Instant getSnapshotAt() {
        return snapshotAt;
    }

    public void setSnapshotAt(Instant snapshotAt) {
        this.snapshotAt = snapshotAt;
    }

    public String getAgreementNumber() {
        return agreementNumber;
    }

    public void setAgreementNumber(String agreementNumber) {
        this.agreementNumber = agreementNumber;
    }

    public UUID getHcfId() {
        return hcfId;
    }

    public void setHcfId(UUID hcfId) {
        this.hcfId = hcfId;
    }

    public String getHcfName() {
        return hcfName;
    }

    public void setHcfName(String hcfName) {
        this.hcfName = hcfName;
    }

    public String getHcfGst() {
        return hcfGst;
    }

    public void setHcfGst(String hcfGst) {
        this.hcfGst = hcfGst;
    }

    public String getHcfPan() {
        return hcfPan;
    }

    public void setHcfPan(String hcfPan) {
        this.hcfPan = hcfPan;
    }

    public String getHcfAddress() {
        return hcfAddress;
    }

    public void setHcfAddress(String hcfAddress) {
        this.hcfAddress = hcfAddress;
    }

    public Integer getHcfBeds() {
        return hcfBeds;
    }

    public void setHcfBeds(Integer hcfBeds) {
        this.hcfBeds = hcfBeds;
    }

    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public BigDecimal getPerBedPerDayRate() {
        return perBedPerDayRate;
    }

    public void setPerBedPerDayRate(BigDecimal perBedPerDayRate) {
        this.perBedPerDayRate = perBedPerDayRate;
    }

    public String getTermsText() {
        return termsText;
    }

    public void setTermsText(String termsText) {
        this.termsText = termsText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDuesStatus() {
        return duesStatus;
    }

    public void setDuesStatus(String duesStatus) {
        this.duesStatus = duesStatus;
    }

    public String getSnapshotReason() {
        return snapshotReason;
    }

    public void setSnapshotReason(String snapshotReason) {
        this.snapshotReason = snapshotReason;
    }

    public SnapshotReason getSnapshotReasonEnum() {
        return snapshotReason != null ? SnapshotReason.valueOf(snapshotReason) : null;
    }

    public void setSnapshotReasonEnum(SnapshotReason reason) {
        this.snapshotReason = reason != null ? reason.name() : null;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
