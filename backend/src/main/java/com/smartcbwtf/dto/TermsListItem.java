package com.smartcbwtf.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for listing Terms versions
 */
public class TermsListItem {
    private UUID id;
    private String version;
    private LocalDate effectiveFrom;
    private Boolean active;
    private Instant createdAt;

    public TermsListItem() {}

    public TermsListItem(UUID id, String version, LocalDate effectiveFrom, Boolean active, Instant createdAt) {
        this.id = id;
        this.version = version;
        this.effectiveFrom = effectiveFrom;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
