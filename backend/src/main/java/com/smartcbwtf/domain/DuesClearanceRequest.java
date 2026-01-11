package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Dues Clearance Request - Workflow entity for HCF report access authorization.
 * 
 * Workflow:
 * 1. HCF Admin requests report access (status = PENDING)
 * 2. CBWTF Admin verifies dues offline, submits to management (status =
 * SUBMITTED)
 * 3. Top Management approves/rejects (status = APPROVED/REJECTED)
 * 4. If approved, report access is granted (reportsAccessGrantedAt set)
 * 
 * Security:
 * - Reports visible only if status = APPROVED AND reportsAccessRevokedAt IS
 * NULL
 */
@Entity
@Table(name = "dues_clearance_request", indexes = {
        @Index(name = "idx_dues_clearance_hcf", columnList = "hcf_id"),
        @Index(name = "idx_dues_clearance_facility", columnList = "facility_id"),
        @Index(name = "idx_dues_clearance_status", columnList = "management_status")
})
public class DuesClearanceRequest {

    public enum Status {
        PENDING, // HCF requested, awaiting CBWTF
        SUBMITTED, // CBWTF submitted to management
        APPROVED, // Management approved
        REJECTED // Management rejected
    }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hcf_id")
    private Hcf hcf;

    @ManyToOne(optional = false)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    // Request lifecycle
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "request_notes")
    private String requestNotes;

    // Granular Access Control
    @Column(name = "request_month")
    private Integer requestMonth;

    @Column(name = "request_year")
    private Integer requestYear;

    // CBWTF verification step
    @Column(name = "cbwtf_submitted_at")
    private Instant cbwtfSubmittedAt;

    @Column(name = "cbwtf_submitted_by")
    private UUID cbwtfSubmittedBy;

    @Column(name = "amount_cleared", precision = 12, scale = 2)
    private BigDecimal amountCleared;

    @Column(name = "outstanding_dues", precision = 12, scale = 2)
    private BigDecimal outstandingDues;

    @Column(name = "cbwtf_notes")
    private String cbwtfNotes;

    // Top Management approval
    @Column(name = "management_status", nullable = false)
    private String managementStatus = Status.PENDING.name();

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // Report access control
    @Column(name = "reports_access_granted_at")
    private Instant reportsAccessGrantedAt;

    @Column(name = "reports_access_revoked_at")
    private Instant reportsAccessRevokedAt;

    @Column(name = "revocation_reason")
    private String revocationReason;

    // Audit
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Hcf getHcf() {
        return hcf;
    }

    public void setHcf(Hcf hcf) {
        this.hcf = hcf;
    }

    public Agreement getAgreement() {
        return agreement;
    }

    public void setAgreement(Agreement agreement) {
        this.agreement = agreement;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(UUID requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getRequestNotes() {
        return requestNotes;
    }

    public void setRequestNotes(String requestNotes) {
        this.requestNotes = requestNotes;
    }

    public Integer getRequestMonth() {
        return requestMonth;
    }

    public void setRequestMonth(Integer requestMonth) {
        this.requestMonth = requestMonth;
    }

    public Integer getRequestYear() {
        return requestYear;
    }

    public void setRequestYear(Integer requestYear) {
        this.requestYear = requestYear;
    }

    public Instant getCbwtfSubmittedAt() {
        return cbwtfSubmittedAt;
    }

    public void setCbwtfSubmittedAt(Instant cbwtfSubmittedAt) {
        this.cbwtfSubmittedAt = cbwtfSubmittedAt;
    }

    public UUID getCbwtfSubmittedBy() {
        return cbwtfSubmittedBy;
    }

    public void setCbwtfSubmittedBy(UUID cbwtfSubmittedBy) {
        this.cbwtfSubmittedBy = cbwtfSubmittedBy;
    }

    public BigDecimal getAmountCleared() {
        return amountCleared;
    }

    public void setAmountCleared(BigDecimal amountCleared) {
        this.amountCleared = amountCleared;
    }

    public BigDecimal getOutstandingDues() {
        return outstandingDues;
    }

    public void setOutstandingDues(BigDecimal outstandingDues) {
        this.outstandingDues = outstandingDues;
    }

    public String getCbwtfNotes() {
        return cbwtfNotes;
    }

    public void setCbwtfNotes(String cbwtfNotes) {
        this.cbwtfNotes = cbwtfNotes;
    }

    public String getManagementStatus() {
        return managementStatus;
    }

    public void setManagementStatus(String managementStatus) {
        this.managementStatus = managementStatus;
        this.updatedAt = Instant.now();
    }

    public Status getManagementStatusEnum() {
        return managementStatus != null ? Status.valueOf(managementStatus) : null;
    }

    public void setManagementStatusEnum(Status status) {
        this.managementStatus = status != null ? status.name() : null;
        this.updatedAt = Instant.now();
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getReportsAccessGrantedAt() {
        return reportsAccessGrantedAt;
    }

    public void setReportsAccessGrantedAt(Instant reportsAccessGrantedAt) {
        this.reportsAccessGrantedAt = reportsAccessGrantedAt;
    }

    public Instant getReportsAccessRevokedAt() {
        return reportsAccessRevokedAt;
    }

    public void setReportsAccessRevokedAt(Instant reportsAccessRevokedAt) {
        this.reportsAccessRevokedAt = reportsAccessRevokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

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

    // Business methods
    public boolean isApproved() {
        return Status.APPROVED.name().equals(managementStatus);
    }

    public boolean hasReportAccess() {
        return isApproved() &&
                reportsAccessGrantedAt != null &&
                reportsAccessRevokedAt == null;
    }

    public void grantReportAccess() {
        if (!isApproved()) {
            throw new IllegalStateException("Cannot grant access - request not approved");
        }
        this.reportsAccessGrantedAt = Instant.now();
        this.reportsAccessRevokedAt = null;
        this.updatedAt = Instant.now();
    }

    public void revokeReportAccess(String reason) {
        this.reportsAccessRevokedAt = Instant.now();
        this.revocationReason = reason;
        this.updatedAt = Instant.now();
    }
}
