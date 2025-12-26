package com.smartcbwtf.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * GPS Vendor Integration - stores vendor-specific configuration per CBWTF
 * facility.
 * Managed by support team, not exposed to CBWTF admins.
 * 
 * SECURITY: credentials field contains sensitive auth data and must never
 * be exposed to CBWTF admin APIs.
 */
@Entity
@Table(name = "gps_vendor_integration", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "facility_id", "vendor" })
})
public class GpsVendorIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "vendor", nullable = false, length = 50)
    private String vendor;

    @Column(name = "integration_type", nullable = false, length = 20)
    private String integrationType; // WEBHOOK, POLLING

    @Column(name = "auth_type", length = 20)
    private String authType; // API_KEY, BASIC, OAUTH, NONE

    /**
     * Vendor credentials stored as JSONB.
     * Structure varies by vendor and auth_type.
     * Example: {"api_key": "...", "secret": "..."}
     * 
     * SECURITY: This field must NEVER be exposed to CBWTF admin APIs.
     */
    @Type(JsonType.class)
    @Column(name = "credentials", columnDefinition = "jsonb")
    private Map<String, Object> credentials;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "polling_interval_seconds")
    private Integer pollingIntervalSeconds = 60;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

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

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getIntegrationType() {
        return integrationType;
    }

    public void setIntegrationType(String integrationType) {
        this.integrationType = integrationType;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public Map<String, Object> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, Object> credentials) {
        this.credentials = credentials;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public Integer getPollingIntervalSeconds() {
        return pollingIntervalSeconds;
    }

    public void setPollingIntervalSeconds(Integer pollingIntervalSeconds) {
        this.pollingIntervalSeconds = pollingIntervalSeconds;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
