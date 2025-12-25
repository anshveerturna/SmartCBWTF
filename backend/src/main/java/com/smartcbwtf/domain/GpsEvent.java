package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * GPS Event entity - APPEND-ONLY, IMMUTABLE.
 * 
 * CRITICAL: This entity is for legal/audit purposes.
 * NO UPDATES, NO DELETES - EVER.
 * 
 * Each record represents a GPS position report from a vehicle.
 */
@Entity
@Table(name = "gps_event", indexes = {
        @Index(name = "idx_gps_vehicle_time", columnList = "vehicle_id, recorded_at DESC"),
        @Index(name = "idx_gps_received", columnList = "received_at DESC")
})
public class GpsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "speed", precision = 6, scale = 2)
    private BigDecimal speed;

    @Column(name = "heading", precision = 5, scale = 2)
    private BigDecimal heading;

    @Column(name = "altitude", precision = 8, scale = 2)
    private BigDecimal altitude;

    @Column(name = "accuracy", precision = 6, scale = 2)
    private BigDecimal accuracy;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }

    // Static factory for immutability emphasis
    public static GpsEvent create(Vehicle vehicle, BigDecimal latitude, BigDecimal longitude,
            BigDecimal speed, Instant recordedAt, String source) {
        GpsEvent event = new GpsEvent();
        event.vehicle = vehicle;
        event.latitude = latitude;
        event.longitude = longitude;
        event.speed = speed;
        event.recordedAt = recordedAt;
        event.source = source;
        event.receivedAt = Instant.now();
        return event;
    }

    // Getters only - no setters to emphasize immutability after creation
    public UUID getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
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

    public BigDecimal getAltitude() {
        return altitude;
    }

    public BigDecimal getAccuracy() {
        return accuracy;
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

    public String getRawPayload() {
        return rawPayload;
    }

    // Builder-style setters for initial creation
    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
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

    public void setAltitude(BigDecimal altitude) {
        this.altitude = altitude;
    }

    public void setAccuracy(BigDecimal accuracy) {
        this.accuracy = accuracy;
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

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }
}
