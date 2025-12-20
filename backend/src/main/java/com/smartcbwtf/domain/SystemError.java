package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a system error or issue that needs attention.
 * Can be reported by users or auto-detected by the system.
 */
@Entity
@Table(name = "system_error")
public class SystemError {

    public enum Severity {
        CRITICAL, ERROR, WARNING, INFO
    }

    public enum Source {
        USER_REPORTED, AUTO_DETECTED, API_ERROR, MOBILE_APP
    }

    public enum Status {
        OPEN, IN_PROGRESS, RESOLVED, IGNORED
    }

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity = Severity.WARNING.name();

    @Column(name = "source", nullable = false, length = 30)
    private String source = Source.USER_REPORTED.name();

    @Column(name = "component", length = 50)
    private String component;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hcf_id")
    private Hcf hcf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private AppUser reportedBy;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "status", nullable = false, length = 20)
    private String status = Status.OPEN.name();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private AppUser resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Factory methods
    public static SystemError userReported(String title, String description, String component) {
        SystemError error = new SystemError();
        error.setTitle(title);
        error.setDescription(description);
        error.setComponent(component);
        error.setSeverityEnum(Severity.WARNING);
        error.setSourceEnum(Source.USER_REPORTED);
        return error;
    }

    public static SystemError autoDetected(String title, String description, String component, Severity severity) {
        SystemError error = new SystemError();
        error.setTitle(title);
        error.setDescription(description);
        error.setComponent(component);
        error.setSeverityEnum(severity);
        error.setSourceEnum(Source.AUTO_DETECTED);
        return error;
    }

    public static SystemError apiError(String title, String stackTrace, String component) {
        SystemError error = new SystemError();
        error.setTitle(title);
        error.setStackTrace(stackTrace);
        error.setComponent(component);
        error.setSeverityEnum(Severity.ERROR);
        error.setSourceEnum(Source.API_ERROR);
        return error;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Severity getSeverityEnum() {
        return Severity.valueOf(severity);
    }

    public void setSeverityEnum(Severity severity) {
        this.severity = severity.name();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Source getSourceEnum() {
        return Source.valueOf(source);
    }

    public void setSourceEnum(Source source) {
        this.source = source.name();
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public Hcf getHcf() {
        return hcf;
    }

    public void setHcf(Hcf hcf) {
        this.hcf = hcf;
    }

    public AppUser getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(AppUser reportedBy) {
        this.reportedBy = reportedBy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Status getStatusEnum() {
        return Status.valueOf(status);
    }

    public void setStatusEnum(Status status) {
        this.status = status.name();
    }

    public AppUser getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(AppUser resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
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
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
