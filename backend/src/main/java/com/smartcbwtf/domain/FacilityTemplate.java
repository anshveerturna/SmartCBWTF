package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Facility-specific agreement PDF/HTML template.
 * Each facility can have multiple template versions, with one active at a time.
 */
@Entity
@Table(name = "facility_template")
public class FacilityTemplate {
    
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(nullable = false)
    private String name;

    @Column(name = "template_type", nullable = false)
    private String templateType; // HTML or PDF

    @Column(name = "content_location", nullable = false)
    private String contentLocation; // Path or URL to template file

    @Column(name = "variables_schema", columnDefinition = "jsonb")
    private String variablesSchema; // JSON schema of template variables

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private Boolean active = false;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Facility getFacility() { return facility; }
    public void setFacility(Facility facility) { this.facility = facility; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }

    public String getContentLocation() { return contentLocation; }
    public void setContentLocation(String contentLocation) { this.contentLocation = contentLocation; }

    public String getVariablesSchema() { return variablesSchema; }
    public void setVariablesSchema(String variablesSchema) { this.variablesSchema = variablesSchema; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
