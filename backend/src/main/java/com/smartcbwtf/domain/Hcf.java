package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Hcf {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String contactEmail;
    private String contactPhone;
    private Integer numberOfBeds;

    private String pincode;
    private String state;

    @Column(nullable = false)
    private Double gpsLat;

    @Column(nullable = false)
    private Double gpsLon;

    @Column(nullable = false)
    private String status; // PENDING_APPROVAL / ACTIVE / REJECTED

    // Registration GPS (captured at registration time)
    private Double registrationGpsLat;
    private Double registrationGpsLon;
    private Double registrationGpsAccuracy;

    // Additional registration fields
    private String doctorName;
    private String panNo;
    private String gstNo;
    private String aadharNo;
    private BigDecimal monthlyCharges;
    private Boolean bedded;
    private String pcbAuthorizationNo;

    @Column(columnDefinition = "TEXT")
    private String otherNotes;

    // Ownership type: OWNED or RENTED
    @Column(name = "ownership_type", nullable = false)
    private String ownershipType = "OWNED";

    // Rent agreement document URL (required if RENTED)
    @Column(name = "rent_agreement_url", length = 500)
    private String rentAgreementUrl;

    // Identity fingerprint for anti-fraud detection
    @Column(name = "identity_hash", length = 64)
    private String identityHash;

    // Billing model - IMMUTABLE after approval
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_model", length = 20)
    private BillingModel billingModel;

    // Approval workflow
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Bed Access Category - Regulatory classification for portal eligibility
    @Enumerated(EnumType.STRING)
    @Column(name = "bed_access_category", length = 20)
    private HcfBedAccessCategory bedAccessCategory;

    @Column(name = "portal_access_enabled")
    private boolean portalAccessEnabled = false;

    // Snapshot of category at approval time - for audit trail
    @Enumerated(EnumType.STRING)
    @Column(name = "approved_bed_access_category", length = 20)
    private HcfBedAccessCategory approvedBedAccessCategory;

    @ManyToOne
    @JoinColumn(name = "registered_by_user_id")
    private AppUser registeredByUser;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    // getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Integer getNumberOfBeds() {
        return numberOfBeds;
    }

    public void setNumberOfBeds(Integer numberOfBeds) {
        this.numberOfBeds = numberOfBeds;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Double getGpsLat() {
        return gpsLat;
    }

    public void setGpsLat(Double gpsLat) {
        this.gpsLat = gpsLat;
    }

    public Double getGpsLon() {
        return gpsLon;
    }

    public void setGpsLon(Double gpsLon) {
        this.gpsLon = gpsLon;
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // New registration fields
    public Double getRegistrationGpsLat() {
        return registrationGpsLat;
    }

    public void setRegistrationGpsLat(Double registrationGpsLat) {
        this.registrationGpsLat = registrationGpsLat;
    }

    public Double getRegistrationGpsLon() {
        return registrationGpsLon;
    }

    public void setRegistrationGpsLon(Double registrationGpsLon) {
        this.registrationGpsLon = registrationGpsLon;
    }

    public Double getRegistrationGpsAccuracy() {
        return registrationGpsAccuracy;
    }

    public void setRegistrationGpsAccuracy(Double registrationGpsAccuracy) {
        this.registrationGpsAccuracy = registrationGpsAccuracy;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getPanNo() {
        return panNo;
    }

    public void setPanNo(String panNo) {
        this.panNo = panNo;
    }

    public String getGstNo() {
        return gstNo;
    }

    public void setGstNo(String gstNo) {
        this.gstNo = gstNo;
    }

    public String getAadharNo() {
        return aadharNo;
    }

    public void setAadharNo(String aadharNo) {
        this.aadharNo = aadharNo;
    }

    public BigDecimal getMonthlyCharges() {
        return monthlyCharges;
    }

    public void setMonthlyCharges(BigDecimal monthlyCharges) {
        this.monthlyCharges = monthlyCharges;
    }

    public Boolean getBedded() {
        return bedded;
    }

    public void setBedded(Boolean bedded) {
        this.bedded = bedded;
    }

    public String getPcbAuthorizationNo() {
        return pcbAuthorizationNo;
    }

    public void setPcbAuthorizationNo(String pcbAuthorizationNo) {
        this.pcbAuthorizationNo = pcbAuthorizationNo;
    }

    public String getOtherNotes() {
        return otherNotes;
    }

    public void setOtherNotes(String otherNotes) {
        this.otherNotes = otherNotes;
    }

    public AppUser getRegisteredByUser() {
        return registeredByUser;
    }

    public void setRegisteredByUser(AppUser registeredByUser) {
        this.registeredByUser = registeredByUser;
    }

    public String getIdentityHash() {
        return identityHash;
    }

    public void setIdentityHash(String identityHash) {
        this.identityHash = identityHash;
    }

    public String getOwnershipType() {
        return ownershipType;
    }

    public void setOwnershipType(String ownershipType) {
        this.ownershipType = ownershipType;
    }

    public String getRentAgreementUrl() {
        return rentAgreementUrl;
    }

    public void setRentAgreementUrl(String rentAgreementUrl) {
        this.rentAgreementUrl = rentAgreementUrl;
    }

    // Billing model getters/setters
    public BillingModel getBillingModel() {
        return billingModel;
    }

    public void setBillingModel(BillingModel billingModel) {
        this.billingModel = billingModel;
    }

    // Approval workflow getters/setters
    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
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

    // Bed Access Category getters/setters
    public HcfBedAccessCategory getBedAccessCategory() {
        return bedAccessCategory;
    }

    public void setBedAccessCategory(HcfBedAccessCategory bedAccessCategory) {
        this.bedAccessCategory = bedAccessCategory;
    }

    public boolean isPortalAccessEnabled() {
        return portalAccessEnabled;
    }

    public void setPortalAccessEnabled(boolean portalAccessEnabled) {
        this.portalAccessEnabled = portalAccessEnabled;
    }

    public HcfBedAccessCategory getApprovedBedAccessCategory() {
        return approvedBedAccessCategory;
    }

    public void setApprovedBedAccessCategory(HcfBedAccessCategory approvedBedAccessCategory) {
        this.approvedBedAccessCategory = approvedBedAccessCategory;
    }

    /**
     * Check if this HCF is eligible for portal access.
     * SINGLE source of truth for portal eligibility.
     */
    public boolean isPortalEligible() {
        return bedAccessCategory != null && bedAccessCategory.isPortalEligible();
    }

    /**
     * Recalculate bed access category based on current bed count.
     */
    public void recalculateBedAccessCategory() {
        this.bedAccessCategory = HcfBedAccessCategory.calculate(numberOfBeds, bedded);
        this.portalAccessEnabled = this.bedAccessCategory.isPortalEligible();
    }

    /**
     * Snapshot the current category at approval time.
     */
    public void snapshotCategoryOnApproval() {
        this.approvedBedAccessCategory = this.bedAccessCategory;
    }
}
