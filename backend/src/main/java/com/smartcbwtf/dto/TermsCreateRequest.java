package com.smartcbwtf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating new Terms & Conditions
 */
public class TermsCreateRequest {
    @NotBlank(message = "Version is required")
    private String version;

    @NotBlank(message = "Terms text (HTML) is required")
    private String textHtml;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private Boolean setActive = false;

    private UUID createdByUserId;

    // Getters and setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getTextHtml() { return textHtml; }
    public void setTextHtml(String textHtml) { this.textHtml = textHtml; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public Boolean getSetActive() { return setActive; }
    public void setSetActive(Boolean setActive) { this.setActive = setActive; }

    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }
}
