package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * QR Authorization - Legal authorization instrument for waste movement.
 * 
 * Key invariants:
 * - Every QR MUST be bound to exactly ONE Agreement
 * - QR validity is time-bound and category-bound
 * - QR becomes invalid if Agreement is not ACTIVE
 * - No QR can be reused across agreements, categories, or validity windows
 */
@Entity
@Table(name = "qr_authorization", indexes = {
        @Index(name = "idx_qr_agreement", columnList = "agreement_id"),
        @Index(name = "idx_qr_facility_status", columnList = "facility_id, status"),
        @Index(name = "idx_qr_hcf", columnList = "hcf_id"),
        @Index(name = "idx_qr_status", columnList = "status")
})
public class QrAuthorization {

    // Status enum - QR lifecycle
    public enum Status {
        ACTIVE, // Valid & usable
        USED, // Picked up but not yet verified
        VERIFIED, // Verified at CBWTF
        EXPIRED, // Validity elapsed
        REVOKED, // Manually or automatically revoked
        BLOCKED // Agreement disputed
    }

    // Waste category enum
    public enum WasteCategory {
        YELLOW, // Infectious waste
        RED, // Contaminated recyclables
        BLUE, // Glassware waste
        WHITE // Sharps waste
    }

    @Id
    private UUID id;

    // Agreement binding (mandatory)
    @ManyToOne(optional = false)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hcf_id")
    private Hcf hcf;

    @ManyToOne(optional = false)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    // Category and validity
    @Column(name = "waste_category", nullable = false)
    private String wasteCategory;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    // Status lifecycle
    @Column(nullable = false)
    private String status = Status.ACTIVE.name();

    // Signed payload stored for reference
    @Column(name = "qr_payload", columnDefinition = "TEXT", nullable = false)
    private String qrPayload;

    @Column(nullable = false)
    private String checksum;

    // Audit fields
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Lifecycle timestamps
    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    // Pickup traceability
    @Column(name = "pickup_event_id")
    private UUID pickupEventId;

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

    public String getWasteCategory() {
        return wasteCategory;
    }

    public void setWasteCategory(String wasteCategory) {
        this.wasteCategory = wasteCategory;
    }

    public WasteCategory getWasteCategoryEnum() {
        return wasteCategory != null ? WasteCategory.valueOf(wasteCategory) : null;
    }

    public void setWasteCategoryEnum(WasteCategory category) {
        this.wasteCategory = category != null ? category.name() : null;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public void setValidTo(Instant validTo) {
        this.validTo = validTo;
    }

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

    public boolean isUsable() {
        return isActive() && Instant.now().isAfter(validFrom) && Instant.now().isBefore(validTo);
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
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

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public UUID getPickupEventId() {
        return pickupEventId;
    }

    public void setPickupEventId(UUID pickupEventId) {
        this.pickupEventId = pickupEventId;
    }
}
