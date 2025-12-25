package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * GPS Ingestion Log - health monitoring for GPS data ingestion.
 * One row per facility+vendor, updated on each ingestion attempt.
 */
@Entity
@Table(name = "gps_ingestion_log", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "facility_id", "vendor" })
})
public class GpsIngestionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "vendor", nullable = false, length = 50)
    private String vendor;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    @Column(name = "last_failure_reason", columnDefinition = "TEXT")
    private String lastFailureReason;

    @Column(name = "success_count", nullable = false)
    private Long successCount = 0L;

    @Column(name = "failure_count", nullable = false)
    private Long failureCount = 0L;

    @Column(name = "events_ingested_today", nullable = false)
    private Long eventsIngestedToday = 0L;

    @Column(name = "last_event_count")
    private Integer lastEventCount = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Methods for recording success/failure
    public void recordSuccess(int eventCount) {
        this.lastSuccessAt = Instant.now();
        this.successCount++;
        this.lastEventCount = eventCount;
        this.eventsIngestedToday += eventCount;
    }

    public void recordFailure(String reason) {
        this.lastFailureAt = Instant.now();
        this.lastFailureReason = reason;
        this.failureCount++;
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

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    public Instant getLastFailureAt() {
        return lastFailureAt;
    }

    public String getLastFailureReason() {
        return lastFailureReason;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public Long getFailureCount() {
        return failureCount;
    }

    public Long getEventsIngestedToday() {
        return eventsIngestedToday;
    }

    public Integer getLastEventCount() {
        return lastEventCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
