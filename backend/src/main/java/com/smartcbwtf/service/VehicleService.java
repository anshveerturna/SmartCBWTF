package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Vehicle Service - manages vehicles and provides GPS data for CBWTF admins.
 */
@Service
public class VehicleService {

    public static final int ONLINE_THRESHOLD_MINUTES = 15;

    private final VehicleRepository vehicleRepository;
    private final GpsEventRepository gpsEventRepository;
    private final FacilityRepository facilityRepository;
    private final AppUserRepository userRepository;

    public VehicleService(
            VehicleRepository vehicleRepository,
            GpsEventRepository gpsEventRepository,
            FacilityRepository facilityRepository,
            AppUserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.gpsEventRepository = gpsEventRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all vehicles for a facility.
     */
    public List<Vehicle> getVehicles(UUID facilityId) {
        return vehicleRepository.findByFacilityId(facilityId);
    }

    /**
     * Get active vehicles for a facility.
     */
    public List<Vehicle> getActiveVehicles(UUID facilityId) {
        return vehicleRepository.findByFacilityIdAndStatus(facilityId, "ACTIVE");
    }

    /**
     * Get vehicle by ID (with tenant check).
     */
    public Optional<Vehicle> getVehicle(UUID facilityId, UUID vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .filter(v -> v.getFacility().getId().equals(facilityId));
    }

    /**
     * Create a new vehicle.
     */
    @Transactional
    public Vehicle createVehicle(UUID facilityId, String registrationNumber,
            String vehicleType, UUID assignedDriverId) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));

        // Check for duplicate registration
        if (vehicleRepository.findByFacilityIdAndRegistrationNumber(facilityId, registrationNumber).isPresent()) {
            throw new IllegalArgumentException("Vehicle with registration " + registrationNumber + " already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setFacility(facility);
        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setVehicleType(vehicleType);
        vehicle.setStatus("ACTIVE");
        vehicle.setGpsStatus("PENDING");

        if (assignedDriverId != null) {
            userRepository.findById(assignedDriverId).ifPresent(vehicle::setAssignedDriver);
        }

        return vehicleRepository.save(vehicle);
    }

    /**
     * Get last known location for a vehicle.
     */
    public Optional<GpsEvent> getLastLocation(UUID vehicleId) {
        return gpsEventRepository.findLatestByVehicleId(vehicleId);
    }

    /**
     * Get live map data - all vehicles with their last positions.
     * "Live" = GPS update within last 15 minutes.
     */
    public List<VehicleLivePosition> getLiveMap(UUID facilityId) {
        List<Vehicle> vehicles = vehicleRepository.findByFacilityIdAndStatus(facilityId, "ACTIVE");
        Instant threshold = Instant.now().minus(ONLINE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);

        return vehicles.stream()
                .map(v -> {
                    boolean isOnline = v.getLastGpsAt() != null && v.getLastGpsAt().isAfter(threshold);
                    String effectiveStatus = isOnline ? "ONLINE" : (v.getGpsDeviceId() != null ? "OFFLINE" : "PENDING");

                    return new VehicleLivePosition(
                            v.getId(),
                            v.getRegistrationNumber(),
                            v.getVehicleType(),
                            v.getLastLatitude(),
                            v.getLastLongitude(),
                            v.getLastGpsAt(),
                            effectiveStatus,
                            v.getAssignedDriver() != null ? v.getAssignedDriver().getFullName() : null);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get recent GPS trail for a vehicle.
     */
    public List<GpsEvent> getGpsTrail(UUID vehicleId, int limit) {
        return gpsEventRepository.findRecentByVehicleId(vehicleId, PageRequest.of(0, limit));
    }

    /**
     * Count online vehicles.
     */
    public long countOnlineVehicles(UUID facilityId) {
        Instant threshold = Instant.now().minus(ONLINE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        return vehicleRepository.countOnlineVehicles(facilityId, threshold);
    }

    /**
     * Count total vehicles.
     */
    public long countTotalVehicles(UUID facilityId) {
        return vehicleRepository.countByFacilityIdAndStatus(facilityId, "ACTIVE");
    }

    /**
     * Vehicle live position DTO.
     */
    public record VehicleLivePosition(
            UUID id,
            String registrationNumber,
            String vehicleType,
            BigDecimal latitude,
            BigDecimal longitude,
            Instant lastGpsAt,
            String gpsStatus,
            String driverName) {
    }
}
