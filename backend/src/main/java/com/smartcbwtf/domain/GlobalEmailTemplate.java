package com.smartcbwtf.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Global email template entity - SuperAdmin managed.
 * CBWTF admins cannot view or modify these templates.
 * Templates are versioned and auditable.
 */
@Entity
@Table(name = "global_email_template", uniqueConstraints = @UniqueConstraint(columnNames = { "template_code",
        "version" }))
public class GlobalEmailTemplate {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "template_code", nullable = false, length = 50)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.smartcbwtf.domain.enums.TemplateCategory category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String subject;

    @Column(name = "body_html", nullable = false, columnDefinition = "TEXT")
    private String bodyHtml;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "required_placeholders")
    private String[] requiredPlaceholders = {};

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "optional_placeholders")
    private String[] optionalPlaceholders = {};

    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // Getters and Setters
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

    public com.smartcbwtf.domain.enums.TemplateCategory getCategory() {
        return category;
    }

    public void setCategory(com.smartcbwtf.domain.enums.TemplateCategory category) {
        this.category = category;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public String[] getRequiredPlaceholders() {
        return requiredPlaceholders;
    }

    public void setRequiredPlaceholders(String[] requiredPlaceholders) {
        this.requiredPlaceholders = requiredPlaceholders;
    }

    public String[] getOptionalPlaceholders() {
        return optionalPlaceholders;
    }

    public void setOptionalPlaceholders(String[] optionalPlaceholders) {
        this.optionalPlaceholders = optionalPlaceholders;
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

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
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

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Get all allowed placeholders (required + optional).
     */
    public List<String> getAllowedPlaceholders() {
        var list = new java.util.ArrayList<String>();
        if (requiredPlaceholders != null)
            list.addAll(java.util.Arrays.asList(requiredPlaceholders));
        if (optionalPlaceholders != null)
            list.addAll(java.util.Arrays.asList(optionalPlaceholders));
        return list;
    }
}
