package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Vehicle entity - represents a waste collection vehicle owned by a CBWTF.
 * GPS tracking is managed through device binding, not direct configuration by
 * admins.
 */
@Entity
@Table(name = "vehicle", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "facility_id", "registration_number" })
})
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "registration_number", nullable = false, length = 20)
    private String registrationNumber;

    @Column(name = "vehicle_type", length = 50)
    private String vehicleType;

    @Column(name = "gps_device_id", length = 100)
    private String gpsDeviceId;

    @Column(name = "gps_vendor", length = 50)
    private String gpsVendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_driver_id")
    private AppUser assignedDriver;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "gps_status", nullable = false, length = 20)
    private String gpsStatus = "PENDING";

    @Column(name = "last_gps_at")
    private Instant lastGpsAt;

    @Column(name = "last_latitude", precision = 10, scale = 7)
    private BigDecimal lastLatitude;

    @Column(name = "last_longitude", precision = 10, scale = 7)
    private BigDecimal lastLongitude;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
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

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getGpsDeviceId() {
        return gpsDeviceId;
    }

    public void setGpsDeviceId(String gpsDeviceId) {
        this.gpsDeviceId = gpsDeviceId;
    }

    public String getGpsVendor() {
        return gpsVendor;
    }

    public void setGpsVendor(String gpsVendor) {
        this.gpsVendor = gpsVendor;
    }

    public AppUser getAssignedDriver() {
        return assignedDriver;
    }

    public void setAssignedDriver(AppUser assignedDriver) {
        this.assignedDriver = assignedDriver;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGpsStatus() {
        return gpsStatus;
    }

    public void setGpsStatus(String gpsStatus) {
        this.gpsStatus = gpsStatus;
    }

    public Instant getLastGpsAt() {
        return lastGpsAt;
    }

    public void setLastGpsAt(Instant lastGpsAt) {
        this.lastGpsAt = lastGpsAt;
    }

    public BigDecimal getLastLatitude() {
        return lastLatitude;
    }

    public void setLastLatitude(BigDecimal lastLatitude) {
        this.lastLatitude = lastLatitude;
    }

    public BigDecimal getLastLongitude() {
        return lastLongitude;
    }

    public void setLastLongitude(BigDecimal lastLongitude) {
        this.lastLongitude = lastLongitude;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
