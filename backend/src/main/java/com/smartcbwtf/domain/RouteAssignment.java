package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * RouteAssignment entity - represents staff/vehicle assignment to a route.
 * Assignments are detachable and replaceable. History is preserved.
 * Only one active assignment per route at a time.
 */
@Entity
@Table(name = "route_assignment")
public class RouteAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private AppUser staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "assigned_from", nullable = false)
    private LocalDate assignedFrom;

    @Column(name = "assigned_to")
    private LocalDate assignedTo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (assignedFrom == null) {
            assignedFrom = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

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

    public AppUser getStaff() {
        return staff;
    }

    public void setStaff(AppUser staff) {
        this.staff = staff;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public LocalDate getAssignedFrom() {
        return assignedFrom;
    }

    public void setAssignedFrom(LocalDate assignedFrom) {
        this.assignedFrom = assignedFrom;
    }

    public LocalDate getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(LocalDate assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Helper method to end this assignment
    public void endAssignment() {
        this.isActive = false;
        this.assignedTo = LocalDate.now();
    }
}
