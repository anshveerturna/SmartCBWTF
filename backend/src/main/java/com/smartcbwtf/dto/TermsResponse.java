package com.smartcbwtf.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for Terms & Conditions
 */
public class TermsResponse {
    private UUID id;
    private UUID facilityId;
    private String facilityName;
    private String version;
    private LocalDate effectiveFrom;
    private String textHtml;
    private boolean active;

    public TermsResponse() {}

    public TermsResponse(String version, LocalDate effectiveFrom, String textHtml) {
        this.version = version;
        this.effectiveFrom = effectiveFrom;
        this.textHtml = textHtml;
    }

    public TermsResponse(UUID id, UUID facilityId, String facilityName, String version, 
                         LocalDate effectiveFrom, String textHtml, boolean active) {
        this.id = id;
        this.facilityId = facilityId;
        this.facilityName = facilityName;
        this.version = version;
        this.effectiveFrom = effectiveFrom;
        this.textHtml = textHtml;
        this.active = active;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getFacilityId() { return facilityId; }
    public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    
    public String getTextHtml() { return textHtml; }
    public void setTextHtml(String textHtml) { this.textHtml = textHtml; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
