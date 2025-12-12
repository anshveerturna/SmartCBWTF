package com.smartcbwtf.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for listing Template versions
 */
public class TemplateListItem {
    private UUID id;
    private String name;
    private String templateType;
    private String version;
    private Boolean active;
    private Instant createdAt;

    public TemplateListItem() {}

    public TemplateListItem(UUID id, String name, String templateType, String version, Boolean active, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.templateType = templateType;
        this.version = version;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
