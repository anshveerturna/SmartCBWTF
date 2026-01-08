package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * RouteWaypoint entity - represents an ordered HCF stop within a route.
 * Sequence order must be contiguous (1, 2, 3, ... N).
 */
@Entity
@Table(name = "route_waypoint", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "route_id", "sequence_order" }),
        @UniqueConstraint(columnNames = { "route_id", "hcf_id" })
})
public class RouteWaypoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hcf_id", nullable = false)
    private Hcf hcf;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "estimated_stop_minutes")
    private Integer estimatedStopMinutes = 15;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
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

    public Hcf getHcf() {
        return hcf;
    }

    public void setHcf(Hcf hcf) {
        this.hcf = hcf;
    }

    public Integer getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(Integer sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public Integer getEstimatedStopMinutes() {
        return estimatedStopMinutes;
    }

    public void setEstimatedStopMinutes(Integer estimatedStopMinutes) {
        this.estimatedStopMinutes = estimatedStopMinutes;
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
}
