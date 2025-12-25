package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * GPS Device Binding History - audit trail for device-to-vehicle bindings.
 * Records all BIND and UNBIND operations for compliance and troubleshooting.
 */
@Entity
@Table(name = "gps_device_binding", indexes = {
        @Index(name = "idx_binding_device", columnList = "device_id, performed_at DESC"),
        @Index(name = "idx_binding_vehicle", columnList = "vehicle_id, performed_at DESC")
})
public class GpsDeviceBinding {

    public static final String ACTION_BOUND = "BOUND";
    public static final String ACTION_UNBOUND = "UNBOUND";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "vendor", length = 50)
    private String vendor;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private AppUser performedBy;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (performedAt == null) {
            performedAt = Instant.now();
        }
    }

    // Factory methods
    public static GpsDeviceBinding createBinding(String deviceId, Vehicle vehicle,
            Facility facility, String vendor,
            AppUser performedBy, String notes) {
        GpsDeviceBinding binding = new GpsDeviceBinding();
        binding.deviceId = deviceId;
        binding.vehicle = vehicle;
        binding.facility = facility;
        binding.vendor = vendor;
        binding.action = ACTION_BOUND;
        binding.performedBy = performedBy;
        binding.notes = notes;
        return binding;
    }

    public static GpsDeviceBinding createUnbinding(String deviceId, Vehicle vehicle,
            Facility facility, AppUser performedBy,
            String notes) {
        GpsDeviceBinding binding = new GpsDeviceBinding();
        binding.deviceId = deviceId;
        binding.vehicle = vehicle;
        binding.facility = facility;
        binding.action = ACTION_UNBOUND;
        binding.performedBy = performedBy;
        binding.notes = notes;
        return binding;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Facility getFacility() {
        return facility;
    }

    public String getVendor() {
        return vendor;
    }

    public String getAction() {
        return action;
    }

    public AppUser getPerformedBy() {
        return performedBy;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public String getNotes() {
        return notes;
    }

    // Setters
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setPerformedBy(AppUser performedBy) {
        this.performedBy = performedBy;
    }

    public void setPerformedAt(Instant performedAt) {
        this.performedAt = performedAt;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
