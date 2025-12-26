package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Records driver attendance at HCF locations.
 * Geofence-validated on submission; cooldown enforced per driver-HCF pair.
 */
@Entity
public class Attendance {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "driver_user_id", nullable = false)
    private AppUser driver;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hcf_id", nullable = false)
    private Hcf hcf;

    // Denormalized for efficient tenant-scoped queries
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @Column(name = "event_ts", nullable = false)
    private Instant eventTs;

    @Column(name = "gps_lat", nullable = false)
    private Double gpsLat;

    @Column(name = "gps_lon", nullable = false)
    private Double gpsLon;

    @Column(name = "gps_accuracy_m")
    private Double gpsAccuracyM;

    @Column(name = "app_device_id")
    private String appDeviceId;

    @Column(name = "distance_from_hcf_m", nullable = false)
    private Double distanceFromHcfM;

    /** Client-generated UUID for idempotency. */
    @Column(name = "client_event_id", unique = true, nullable = false)
    private UUID clientEventId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AppUser getDriver() {
        return driver;
    }

    public void setDriver(AppUser driver) {
        this.driver = driver;
    }

    public Hcf getHcf() {
        return hcf;
    }

    public void setHcf(Hcf hcf) {
        this.hcf = hcf;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public Instant getEventTs() {
        return eventTs;
    }

    public void setEventTs(Instant eventTs) {
        this.eventTs = eventTs;
    }

    public Double getGpsLat() {
        return gpsLat;
    }

    public void setGpsLat(Double gpsLat) {
        this.gpsLat = gpsLat;
    }

    public Double getGpsLon() {
        return gpsLon;
    }

    public void setGpsLon(Double gpsLon) {
        this.gpsLon = gpsLon;
    }

    public Double getGpsAccuracyM() {
        return gpsAccuracyM;
    }

    public void setGpsAccuracyM(Double gpsAccuracyM) {
        this.gpsAccuracyM = gpsAccuracyM;
    }

    public String getAppDeviceId() {
        return appDeviceId;
    }

    public void setAppDeviceId(String appDeviceId) {
        this.appDeviceId = appDeviceId;
    }

    public Double getDistanceFromHcfM() {
        return distanceFromHcfM;
    }

    public void setDistanceFromHcfM(Double distanceFromHcfM) {
        this.distanceFromHcfM = distanceFromHcfM;
    }

    public UUID getClientEventId() {
        return clientEventId;
    }

    public void setClientEventId(UUID clientEventId) {
        this.clientEventId = clientEventId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
