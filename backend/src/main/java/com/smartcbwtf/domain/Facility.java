package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "facility")
public class Facility {

    // Subscription plan enum
    public enum Plan {
        BASIC, PRO, ENTERPRISE, TRIAL
    }

    // Subscription status enum
    public enum Status {
        ACTIVE, TRIAL, EXPIRED, SUSPENDED, CANCELLED
    }

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
    private String ownerName;
    private Double gpsLat;
    private Double gpsLon;

    @Column(name = "geofence_radius_m")
    private Integer geofenceRadiusM;

    // Business registration fields (V13 migration)
    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "aadhar_number", length = 20)
    private String aadharNumber;

    // Subscription fields (V8 migration)
    @Column(name = "subscription_plan")
    private String subscriptionPlan;

    @Column(name = "subscription_status")
    private String subscriptionStatus;

    @Column(name = "subscription_expires_at")
    private Instant subscriptionExpiresAt;

    @Column(name = "onboarded_at")
    private Instant onboardedAt;

    @Column(name = "onboarded_by")
    private UUID onboardedBy;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // Global billing settings (V29 migration)
    @Column(name = "excess_rate_per_kg", nullable = false)
    private java.math.BigDecimal excessRatePerKg = new java.math.BigDecimal("50.00");

    @Column(name = "excess_rate_effective_from", nullable = false)
    private java.time.LocalDate excessRateEffectiveFrom = java.time.LocalDate.now();

    // Basic getters and setters
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

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
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

    public Integer getGeofenceRadiusM() {
        return geofenceRadiusM;
    }

    public void setGeofenceRadiusM(Integer geofenceRadiusM) {
        this.geofenceRadiusM = geofenceRadiusM;
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

    // Subscription getters/setters with enum support
    public String getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(String subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    public Plan getSubscriptionPlanEnum() {
        return subscriptionPlan != null ? Plan.valueOf(subscriptionPlan) : null;
    }

    public void setSubscriptionPlanEnum(Plan plan) {
        this.subscriptionPlan = plan != null ? plan.name() : null;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public Status getSubscriptionStatusEnum() {
        return subscriptionStatus != null ? Status.valueOf(subscriptionStatus) : null;
    }

    public void setSubscriptionStatusEnum(Status status) {
        this.subscriptionStatus = status != null ? status.name() : null;
    }

    public Instant getSubscriptionExpiresAt() {
        return subscriptionExpiresAt;
    }

    public void setSubscriptionExpiresAt(Instant subscriptionExpiresAt) {
        this.subscriptionExpiresAt = subscriptionExpiresAt;
    }

    public Instant getOnboardedAt() {
        return onboardedAt;
    }

    public void setOnboardedAt(Instant onboardedAt) {
        this.onboardedAt = onboardedAt;
    }

    public UUID getOnboardedBy() {
        return onboardedBy;
    }

    public void setOnboardedBy(UUID onboardedBy) {
        this.onboardedBy = onboardedBy;
    }

    // Business registration getters/setters
    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }

    public String getAadharNumber() {
        return aadharNumber;
    }

    public void setAadharNumber(String aadharNumber) {
        this.aadharNumber = aadharNumber;
    }

    // Helper methods
    public boolean isSubscriptionActive() {
        if (subscriptionStatus == null)
            return false;
        Status status = Status.valueOf(subscriptionStatus);
        return status == Status.ACTIVE || status == Status.TRIAL;
    }

    public boolean isExpired() {
        return subscriptionExpiresAt != null && Instant.now().isAfter(subscriptionExpiresAt);
    }

    // Global billing getters/setters
    public java.math.BigDecimal getExcessRatePerKg() {
        return excessRatePerKg;
    }

    public void setExcessRatePerKg(java.math.BigDecimal excessRatePerKg) {
        this.excessRatePerKg = excessRatePerKg;
    }

    public java.time.LocalDate getExcessRateEffectiveFrom() {
        return excessRateEffectiveFrom;
    }

    public void setExcessRateEffectiveFrom(java.time.LocalDate excessRateEffectiveFrom) {
        this.excessRateEffectiveFrom = excessRateEffectiveFrom;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
