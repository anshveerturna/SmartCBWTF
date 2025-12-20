package com.smartcbwtf.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Feature flag per tenant.
 * Enables/disables specific features for individual facilities (tenants).
 */
@Entity
@Table(name = "tenant_feature_flag")
public class TenantFeatureFlag {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "feature_key", nullable = false, length = 100)
    private String featureKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private String config; // JSON configuration for the feature

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Well-known feature keys
    public static final String ADVANCED_ANALYTICS = "ADVANCED_ANALYTICS";
    public static final String ROUTE_OPTIMIZATION = "ROUTE_OPTIMIZATION";
    public static final String CPCB_REPORTING = "CPCB_REPORTING";
    public static final String INVOICE_AUTO_SEND = "INVOICE_AUTO_SEND";
    public static final String PAYMENT_GATEWAY = "PAYMENT_GATEWAY";
    public static final String ATTENDANCE_ENFORCEMENT = "ATTENDANCE_ENFORCEMENT";
    public static final String VEHICLE_TRACKING = "VEHICLE_TRACKING";
    public static final String AI_INSIGHTS = "AI_INSIGHTS";
    public static final String MULTI_VEHICLE = "MULTI_VEHICLE";
    public static final String HCF_SELF_SERVICE = "HCF_SELF_SERVICE";

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public void setFeatureKey(String featureKey) {
        this.featureKey = featureKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Alias for use with method references
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
