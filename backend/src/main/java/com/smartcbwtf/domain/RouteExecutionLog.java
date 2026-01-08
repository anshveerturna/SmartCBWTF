package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Tracks individual waypoint execution status within a route cycle.
 * Links attendance records to route waypoints for compliance tracking.
 */
@Entity
@Table(name = "route_execution_log")
public class RouteExecutionLog {

    public enum ExecutionStatus {
        PENDING,
        COMPLETED,
        MISSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private RouteCycleHistory cycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waypoint_id", nullable = false)
    private RouteWaypoint waypoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hcf_id", nullable = false)
    private Hcf hcf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id")
    private Attendance attendance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private AppUser staff;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(name = "visited_at")
    private Instant visitedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RouteCycleHistory getCycle() {
        return cycle;
    }

    public void setCycle(RouteCycleHistory cycle) {
        this.cycle = cycle;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public RouteWaypoint getWaypoint() {
        return waypoint;
    }

    public void setWaypoint(RouteWaypoint waypoint) {
        this.waypoint = waypoint;
    }

    public Hcf getHcf() {
        return hcf;
    }

    public void setHcf(Hcf hcf) {
        this.hcf = hcf;
    }

    public Attendance getAttendance() {
        return attendance;
    }

    public void setAttendance(Attendance attendance) {
        this.attendance = attendance;
    }

    public AppUser getStaff() {
        return staff;
    }

    public void setStaff(AppUser staff) {
        this.staff = staff;
    }

    public Integer getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(Integer sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Instant getVisitedAt() {
        return visitedAt;
    }

    public void setVisitedAt(Instant visitedAt) {
        this.visitedAt = visitedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Helper methods
    public void markCompleted(Attendance attendance) {
        this.attendance = attendance;
        this.staff = attendance.getDriver();
        this.visitedAt = attendance.getEventTs();
        this.status = ExecutionStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void markMissed() {
        this.status = ExecutionStatus.MISSED;
        this.updatedAt = Instant.now();
    }
}
