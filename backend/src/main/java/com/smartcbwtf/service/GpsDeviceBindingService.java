package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * GPS Device Binding Service - manages device-to-vehicle binding with audit
 * trail.
 * 
 * RULES:
 * - One device can only be bound to one vehicle at a time
 * - Rebinding requires explicit unbind first
 * - All operations are audited
 */
@Service
public class GpsDeviceBindingService {

    private static final Logger log = LoggerFactory.getLogger(GpsDeviceBindingService.class);

    private final VehicleRepository vehicleRepository;
    private final GpsDeviceBindingRepository bindingRepository;
    private final FacilityRepository facilityRepository;
    private final AppUserRepository userRepository;

    public GpsDeviceBindingService(
            VehicleRepository vehicleRepository,
            GpsDeviceBindingRepository bindingRepository,
            FacilityRepository facilityRepository,
            AppUserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.bindingRepository = bindingRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
    }

    /**
     * Bind a GPS device to a vehicle.
     * 
     * @param deviceId      Device ID (IMEI or other identifier)
     * @param vehicleId     Vehicle to bind to
     * @param vendor        GPS vendor name
     * @param performedById User performing the operation
     * @param notes         Optional notes
     * @throws IllegalStateException if device is already bound
     */
    @Transactional
    public void bindDevice(String deviceId, UUID vehicleId, String vendor,
            UUID performedById, String notes) {
        // Validate vehicle exists
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));

        // Check if device is already bound
        if (bindingRepository.isDeviceCurrentlyBound(deviceId)) {
            throw new IllegalStateException("Device " + deviceId + " is already bound to a vehicle. Unbind first.");
        }

        // Check if vehicle already has a device
        if (vehicle.getGpsDeviceId() != null && !vehicle.getGpsDeviceId().isEmpty()) {
            throw new IllegalStateException("Vehicle already has GPS device: " + vehicle.getGpsDeviceId());
        }

        // Update vehicle
        vehicle.setGpsDeviceId(deviceId);
        vehicle.setGpsVendor(vendor);
        vehicle.setGpsStatus("CONNECTED");
        vehicleRepository.save(vehicle);

        // Create audit record
        GpsDeviceBinding binding = GpsDeviceBinding.createBinding(
                deviceId, vehicle, vehicle.getFacility(), vendor,
                performedById != null ? userRepository.findById(performedById).orElse(null) : null,
                notes);
        bindingRepository.save(binding);

        log.info("Bound device {} to vehicle {} ({})", deviceId, vehicleId, vehicle.getRegistrationNumber());
    }

    /**
     * Unbind a GPS device from a vehicle.
     */
    @Transactional
    public void unbindDevice(String deviceId, UUID performedById, String notes) {
        Vehicle vehicle = vehicleRepository.findByGpsDeviceId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("No vehicle bound to device: " + deviceId));

        // Create audit record BEFORE unbinding
        GpsDeviceBinding binding = GpsDeviceBinding.createUnbinding(
                deviceId, vehicle, vehicle.getFacility(),
                performedById != null ? userRepository.findById(performedById).orElse(null) : null,
                notes);
        bindingRepository.save(binding);

        // Clear device from vehicle
        vehicle.setGpsDeviceId(null);
        vehicle.setGpsVendor(null);
        vehicle.setGpsStatus("PENDING");
        vehicleRepository.save(vehicle);

        log.info("Unbound device {} from vehicle {}", deviceId, vehicle.getRegistrationNumber());
    }

    /**
     * Reassign a device from one vehicle to another.
     * This is a convenience method that unbinds then binds.
     */
    @Transactional
    public void reassignDevice(String deviceId, UUID newVehicleId, String vendor,
            UUID performedById, String notes) {
        // Unbind from current vehicle if bound
        if (bindingRepository.isDeviceCurrentlyBound(deviceId)) {
            unbindDevice(deviceId, performedById, "Reassigning to vehicle: " + newVehicleId);
        }

        // Bind to new vehicle
        bindDevice(deviceId, newVehicleId, vendor, performedById, notes);
    }

    /**
     * Check if a device is currently bound.
     */
    public boolean isDeviceBound(String deviceId) {
        return bindingRepository.isDeviceCurrentlyBound(deviceId);
    }

    /**
     * Get the vehicle currently bound to a device.
     */
    public Vehicle getVehicleForDevice(String deviceId) {
        return vehicleRepository.findByGpsDeviceId(deviceId).orElse(null);
    }
}
