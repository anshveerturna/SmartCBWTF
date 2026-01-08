package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores alerts for incomplete routes and missed waypoints.
 * Enables CBWTF admins to track compliance issues.
 */
@Entity
@Table(name = "route_alert")
public class RouteAlert {

    public enum AlertType {
        ROUTE_INCOMPLETE,
        WAYPOINT_MISSED,
        ROUTE_NOT_STARTED
    }

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private RouteCycleHistory cycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private AppUser staff;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity = Severity.WARNING;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "missed_hcf_count", nullable = false)
    private Integer missedHcfCount = 0;

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private AppUser resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public RouteCycleHistory getCycle() {
        return cycle;
    }

    public void setCycle(RouteCycleHistory cycle) {
        this.cycle = cycle;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public AppUser getStaff() {
        return staff;
    }

    public void setStaff(AppUser staff) {
        this.staff = staff;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType alertType) {
        this.alertType = alertType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getMissedHcfCount() {
        return missedHcfCount;
    }

    public void setMissedHcfCount(Integer missedHcfCount) {
        this.missedHcfCount = missedHcfCount;
    }

    public Boolean getIsResolved() {
        return isResolved;
    }

    public void setIsResolved(Boolean isResolved) {
        this.isResolved = isResolved;
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

    // Helper methods
    public void resolve(AppUser resolvedBy, String notes) {
        this.isResolved = true;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = Instant.now();
        this.resolutionNotes = notes;
    }
}
