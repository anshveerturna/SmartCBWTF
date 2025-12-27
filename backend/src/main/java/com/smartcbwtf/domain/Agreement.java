package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Agreement entity - First-class ownership object linking HCF to CBWTF.
 * 
 * Key invariants:
 * - Only ONE ACTIVE agreement per HCF globally (enforced by DB unique index)
 * - No new agreement if previous is ACTIVE
 * - No new agreement if dues_status != CLEAR
 */
@Entity
@Table(name = "agreement", indexes = {
        @Index(name = "idx_agreement_facility_status", columnList = "facility_id, status"),
        @Index(name = "idx_agreement_dues_status", columnList = "dues_status")
})
public class Agreement {

    // Status enum - Agreement lifecycle
    public enum Status {
        ACTIVE, // Contract in force
        EXPIRED, // End date passed
        TERMINATED, // Manually ended early
        DISPUTED // Legal dispute open
    }

    // Dues status enum - Payment tracking
    public enum DuesStatus {
        CLEAR, // No outstanding payments
        PENDING, // Invoices unpaid
        DISPUTED // Payment dispute
    }

    @Id
    @GeneratedValue
    private UUID id;

    // Human-readable code: AGR-2024-00001 (immutable after creation)
    @Column(name = "agreement_number", nullable = false, unique = true)
    private String agreementNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hcf_id")
    private Hcf hcf;

    @ManyToOne(optional = false)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    // Status with enum support
    @Column(nullable = false)
    private String status;

    @Column(name = "dues_status")
    private String duesStatus = DuesStatus.CLEAR.name();

    // Contract period
    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    // Billing
    @Column(nullable = false)
    private BigDecimal perBedPerDayRate;

    // Terms
    private String termsText;
    private String pdfUrl;

    // Terms acceptance
    private Boolean termsAccepted = false;
    private String termsVersion;
    private Instant termsAcceptedAt;

    @ManyToOne
    @JoinColumn(name = "terms_accepted_by")
    private AppUser termsAcceptedBy;

    // Template tracking
    @ManyToOne
    @JoinColumn(name = "template_id")
    private FacilityTemplate template;
    private String templateVersion;

    // Termination fields
    @Column(name = "termination_reason")
    private String terminationReason;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @Column(name = "terminated_by")
    private UUID terminatedBy;

    // Audit timestamps
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by")
    private UUID createdBy;

    // Version for renewal tracking (V31 migration)
    @Column(nullable = false)
    private Integer version = 1;

    // ---------- Getters and Setters ----------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAgreementNumber() {
        return agreementNumber;
    }

    public void setAgreementNumber(String agreementNumber) {
        this.agreementNumber = agreementNumber;
    }

    public Hcf getHcf() {
        return hcf;
    }

    public void setHcf(Hcf hcf) {
        this.hcf = hcf;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    // Status with enum support
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Status getStatusEnum() {
        return status != null ? Status.valueOf(status) : null;
    }

    public void setStatusEnum(Status status) {
        this.status = status != null ? status.name() : null;
    }

    public boolean isActive() {
        return Status.ACTIVE.name().equals(status);
    }

    // Dues status with enum support
    public String getDuesStatus() {
        return duesStatus;
    }

    public void setDuesStatus(String duesStatus) {
        this.duesStatus = duesStatus;
    }

    public DuesStatus getDuesStatusEnum() {
        return duesStatus != null ? DuesStatus.valueOf(duesStatus) : null;
    }

    public void setDuesStatusEnum(DuesStatus duesStatus) {
        this.duesStatus = duesStatus != null ? duesStatus.name() : null;
    }

    public boolean isDuesClear() {
        return DuesStatus.CLEAR.name().equals(duesStatus);
    }

    // Contract period
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    // Billing
    public BigDecimal getPerBedPerDayRate() {
        return perBedPerDayRate;
    }

    public void setPerBedPerDayRate(BigDecimal perBedPerDayRate) {
        this.perBedPerDayRate = perBedPerDayRate;
    }

    // Terms
    public String getTermsText() {
        return termsText;
    }

    public void setTermsText(String termsText) {
        this.termsText = termsText;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public Boolean getTermsAccepted() {
        return termsAccepted;
    }

    public void setTermsAccepted(Boolean termsAccepted) {
        this.termsAccepted = termsAccepted;
    }

    public String getTermsVersion() {
        return termsVersion;
    }

    public void setTermsVersion(String termsVersion) {
        this.termsVersion = termsVersion;
    }

    public Instant getTermsAcceptedAt() {
        return termsAcceptedAt;
    }

    public void setTermsAcceptedAt(Instant termsAcceptedAt) {
        this.termsAcceptedAt = termsAcceptedAt;
    }

    public AppUser getTermsAcceptedBy() {
        return termsAcceptedBy;
    }

    public void setTermsAcceptedBy(AppUser termsAcceptedBy) {
        this.termsAcceptedBy = termsAcceptedBy;
    }

    // Template
    public FacilityTemplate getTemplate() {
        return template;
    }

    public void setTemplate(FacilityTemplate template) {
        this.template = template;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }

    // Termination
    public String getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

    public Instant getTerminatedAt() {
        return terminatedAt;
    }

    public void setTerminatedAt(Instant terminatedAt) {
        this.terminatedAt = terminatedAt;
    }

    public UUID getTerminatedBy() {
        return terminatedBy;
    }

    public void setTerminatedBy(UUID terminatedBy) {
        this.terminatedBy = terminatedBy;
    }

    // Audit
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
