package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Records the history of route execution cycles.
 * Each cycle represents one completion period (e.g., a day for daily routes).
 */
@Entity
@Table(name = "route_cycle_history")
public class RouteCycleHistory {

    public enum CycleStatus {
        IN_PROGRESS,
        COMPLETED,
        INCOMPLETE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private AppUser staff;

    @Column(name = "cycle_number", nullable = false)
    private Integer cycleNumber;

    @Column(name = "cycle_start", nullable = false)
    private LocalDate cycleStart;

    @Column(name = "cycle_end", nullable = false)
    private LocalDate cycleEnd;

    @Column(name = "total_waypoints", nullable = false)
    private Integer totalWaypoints;

    @Column(name = "completed_waypoints", nullable = false)
    private Integer completedWaypoints = 0;

    @Column(name = "missed_waypoints", nullable = false)
    private Integer missedWaypoints = 0;

    @Column(name = "completion_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal completionPercentage = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CycleStatus status = CycleStatus.IN_PROGRESS;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    private List<RouteExecutionLog> executionLogs = new ArrayList<>();

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

    public Integer getCycleNumber() {
        return cycleNumber;
    }

    public void setCycleNumber(Integer cycleNumber) {
        this.cycleNumber = cycleNumber;
    }

    public LocalDate getCycleStart() {
        return cycleStart;
    }

    public void setCycleStart(LocalDate cycleStart) {
        this.cycleStart = cycleStart;
    }

    public LocalDate getCycleEnd() {
        return cycleEnd;
    }

    public void setCycleEnd(LocalDate cycleEnd) {
        this.cycleEnd = cycleEnd;
    }

    public Integer getTotalWaypoints() {
        return totalWaypoints;
    }

    public void setTotalWaypoints(Integer totalWaypoints) {
        this.totalWaypoints = totalWaypoints;
    }

    public Integer getCompletedWaypoints() {
        return completedWaypoints;
    }

    public void setCompletedWaypoints(Integer completedWaypoints) {
        this.completedWaypoints = completedWaypoints;
    }

    public Integer getMissedWaypoints() {
        return missedWaypoints;
    }

    public void setMissedWaypoints(Integer missedWaypoints) {
        this.missedWaypoints = missedWaypoints;
    }

    public BigDecimal getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(BigDecimal completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public CycleStatus getStatus() {
        return status;
    }

    public void setStatus(CycleStatus status) {
        this.status = status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<RouteExecutionLog> getExecutionLogs() {
        return executionLogs;
    }

    public void setExecutionLogs(List<RouteExecutionLog> executionLogs) {
        this.executionLogs = executionLogs;
    }

    // Helper methods
    public void recalculateStats() {
        long completed = executionLogs.stream()
                .filter(log -> log.getStatus() == RouteExecutionLog.ExecutionStatus.COMPLETED)
                .count();
        long missed = executionLogs.stream()
                .filter(log -> log.getStatus() == RouteExecutionLog.ExecutionStatus.MISSED)
                .count();

        this.completedWaypoints = (int) completed;
        this.missedWaypoints = (int) missed;

        if (totalWaypoints > 0) {
            this.completionPercentage = BigDecimal.valueOf(completed * 100.0 / totalWaypoints)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    public boolean isComplete() {
        return completedWaypoints.equals(totalWaypoints);
    }

    public boolean isOverdue() {
        return LocalDate.now().isAfter(cycleEnd) && status == CycleStatus.IN_PROGRESS;
    }
}
