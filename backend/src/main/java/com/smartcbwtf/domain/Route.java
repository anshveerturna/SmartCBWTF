package com.smartcbwtf.domain;

import com.smartcbwtf.domain.enums.RouteStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Route entity - represents a waste collection route owned by a CBWTF.
 * Routes are first-class entities, independent of staff lifecycle.
 */
@Entity
@Table(name = "route", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "facility_id", "name" })
})
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 7)
    private String color = "#3B82F6";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RouteStatus status = RouteStatus.DRAFT;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "completion_days")
    private Integer completionDays = 1;

    @Column(name = "cycle_start_date")
    private java.time.LocalDate cycleStartDate;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    private List<RouteWaypoint> waypoints = new ArrayList<>();

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
    @OrderBy("createdAt DESC")
    private List<RouteAssignment> assignments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        updateIsActive();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        updateIsActive();
    }

    private void updateIsActive() {
        this.isActive = (this.status == RouteStatus.ACTIVE);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public RouteStatus getStatus() {
        return status;
    }

    public void setStatus(RouteStatus status) {
        this.status = status;
        updateIsActive();
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public List<RouteWaypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<RouteWaypoint> waypoints) {
        this.waypoints = waypoints;
    }

    public List<RouteAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<RouteAssignment> assignments) {
        this.assignments = assignments;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Helper method to get current active assignment
    public RouteAssignment getCurrentAssignment() {
        return assignments.stream()
                .filter(RouteAssignment::getIsActive)
                .findFirst()
                .orElse(null);
    }

    public Integer getCompletionDays() {
        return completionDays;
    }

    public void setCompletionDays(Integer completionDays) {
        this.completionDays = completionDays;
    }

    public java.time.LocalDate getCycleStartDate() {
        return cycleStartDate;
    }

    public void setCycleStartDate(java.time.LocalDate cycleStartDate) {
        this.cycleStartDate = cycleStartDate;
    }
}
