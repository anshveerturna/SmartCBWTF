package com.smartcbwtf.dto.settings;

import java.util.Set;
import java.util.UUID;

/**
 * DTO for email template.
 */
public class EmailTemplateDTO {
    private UUID id;
    private String templateCode;
    private String subjectTemplate;
    private String bodyTemplate;
    private Integer version;
    private Boolean isActive;
    private Set<String> requiredPlaceholders;
    private Set<String> availablePlaceholders;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public void setSubjectTemplate(String subjectTemplate) {
        this.subjectTemplate = subjectTemplate;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Set<String> getRequiredPlaceholders() {
        return requiredPlaceholders;
    }

    public void setRequiredPlaceholders(Set<String> requiredPlaceholders) {
        this.requiredPlaceholders = requiredPlaceholders;
    }

    public Set<String> getAvailablePlaceholders() {
        return availablePlaceholders;
    }

    public void setAvailablePlaceholders(Set<String> availablePlaceholders) {
        this.availablePlaceholders = availablePlaceholders;
    }
}
