package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable snapshot of branding at document generation time.
 * Documents link to this to prevent retroactive appearance changes.
 */
@Entity
@Table(name = "branding_snapshot")
public class BrandingSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "logo_checksum", length = 64)
    private String logoChecksum;

    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    @Column(name = "footer_text", columnDefinition = "TEXT")
    private String footerText;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getLogoChecksum() {
        return logoChecksum;
    }

    public void setLogoChecksum(String logoChecksum) {
        this.logoChecksum = logoChecksum;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getFooterText() {
        return footerText;
    }

    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
