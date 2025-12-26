package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * GPS tracking events for staff users (DRIVER, PLANT_OPERATOR).
 * 
 * IMMUTABILITY CONTRACT:
 * - This table is APPEND-ONLY
 * - No updates or deletes are permitted
 * - Used for audit trail and real-time tracking
 * - Idempotency enforced via clientEventId
 */
@Entity
@Table(name = "user_gps_event")
public class UserGpsEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_user_id", nullable = false, updatable = false)
    private AppUser staffUser;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false, updatable = false)
    private Facility facility;

    @Column(nullable = false, precision = 10, scale = 7, updatable = false)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7, updatable = false)
    private BigDecimal longitude;

    @Column(precision = 6, scale = 2, updatable = false)
    private BigDecimal speed;

    @Column(precision = 5, scale = 2, updatable = false)
    private BigDecimal heading;

    @Column(name = "accuracy_m", precision = 6, scale = 2, updatable = false)
    private BigDecimal accuracyM;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt = Instant.now();

    @Column(length = 20, updatable = false)
    private String source = "ANDROID_APP";

    @Column(name = "client_event_id", unique = true, nullable = false, updatable = false)
    private UUID clientEventId;

    // Getters
    public UUID getId() {
        return id;
    }

    public AppUser getStaffUser() {
        return staffUser;
    }

    public Facility getFacility() {
        return facility;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public BigDecimal getSpeed() {
        return speed;
    }

    public BigDecimal getHeading() {
        return heading;
    }

    public BigDecimal getAccuracyM() {
        return accuracyM;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getSource() {
        return source;
    }

    public UUID getClientEventId() {
        return clientEventId;
    }

    // Setters (only for creation - no updates allowed)
    public void setId(UUID id) {
        this.id = id;
    }

    public void setStaffUser(AppUser staffUser) {
        this.staffUser = staffUser;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public void setSpeed(BigDecimal speed) {
        this.speed = speed;
    }

    public void setHeading(BigDecimal heading) {
        this.heading = heading;
    }

    public void setAccuracyM(BigDecimal accuracyM) {
        this.accuracyM = accuracyM;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setClientEventId(UUID clientEventId) {
        this.clientEventId = clientEventId;
    }
}
