package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.GpsEvent;
import com.smartcbwtf.domain.Vehicle;
import com.smartcbwtf.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Vehicle Controller - CBWTF Admin APIs for vehicle and GPS data.
 * All endpoints are tenant-scoped via TenantContext.
 */
@RestController
@RequestMapping("/api/cbwtf/vehicles")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * GET /api/cbwtf/vehicles - List all vehicles for the CBWTF.
     */
    @GetMapping
    public ResponseEntity<List<VehicleDTO>> getVehicles() {
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            return ResponseEntity.status(403).build();
        }

        List<VehicleDTO> vehicles = vehicleService.getActiveVehicles(facilityId).stream()
                .map(this::toDTO)
                .toList();

        return ResponseEntity.ok(vehicles);
    }

    /**
     * GET /api/cbwtf/vehicles/{id} - Get vehicle details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<VehicleDTO> getVehicle(@PathVariable UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            return ResponseEntity.status(403).build();
        }

        return vehicleService.getVehicle(facilityId, id)
                .map(v -> ResponseEntity.ok(toDTO(v)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/cbwtf/vehicles/{id}/last-location - Get last GPS position.
     */
    @GetMapping("/{id}/last-location")
    public ResponseEntity<GpsLocationDTO> getLastLocation(@PathVariable UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            return ResponseEntity.status(403).build();
        }

        // Verify vehicle belongs to facility
        Vehicle vehicle = vehicleService.getVehicle(facilityId, id).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.notFound().build();
        }

        return vehicleService.getLastLocation(id)
                .map(e -> ResponseEntity.ok(toLocationDTO(e)))
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * GET /api/cbwtf/vehicles/live-map - Get all vehicles with last positions for
     * map.
     */
    @GetMapping("/live-map")
    public ResponseEntity<LiveMapDTO> getLiveMap() {
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            return ResponseEntity.status(403).build();
        }

        List<VehicleService.VehicleLivePosition> positions = vehicleService.getLiveMap(facilityId);
        long onlineCount = positions.stream().filter(p -> "ONLINE".equals(p.gpsStatus())).count();
        long totalCount = positions.size();

        LiveMapDTO response = new LiveMapDTO(
                positions.stream().map(this::toLivePositionDTO).toList(),
                onlineCount,
                totalCount,
                Instant.now());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/cbwtf/vehicles/{id}/trail - Get GPS trail/history.
     */
    @GetMapping("/{id}/trail")
    public ResponseEntity<List<GpsLocationDTO>> getGpsTrail(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "50") int limit) {
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            return ResponseEntity.status(403).build();
        }

        // Verify vehicle belongs to facility
        Vehicle vehicle = vehicleService.getVehicle(facilityId, id).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.notFound().build();
        }

        List<GpsLocationDTO> trail = vehicleService.getGpsTrail(id, Math.min(limit, 100))
                .stream()
                .map(this::toLocationDTO)
                .toList();

        return ResponseEntity.ok(trail);
    }

    // DTO conversion methods
    private VehicleDTO toDTO(Vehicle v) {
        return new VehicleDTO(
                v.getId(),
                v.getRegistrationNumber(),
                v.getVehicleType(),
                v.getGpsStatus(),
                v.getLastGpsAt(),
                v.getLastLatitude(),
                v.getLastLongitude(),
                v.getAssignedDriver() != null ? v.getAssignedDriver().getFullName() : null,
                v.getStatus());
    }

    private GpsLocationDTO toLocationDTO(GpsEvent e) {
        return new GpsLocationDTO(
                e.getLatitude(),
                e.getLongitude(),
                e.getSpeed(),
                e.getHeading(),
                e.getRecordedAt());
    }

    private LivePositionDTO toLivePositionDTO(VehicleService.VehicleLivePosition p) {
        return new LivePositionDTO(
                p.id(),
                p.registrationNumber(),
                p.vehicleType(),
                p.latitude(),
                p.longitude(),
                p.lastGpsAt(),
                p.gpsStatus(),
                p.driverName());
    }

    // DTOs
    public record VehicleDTO(
            UUID id,
            String registrationNumber,
            String vehicleType,
            String gpsStatus,
            Instant lastGpsAt,
            BigDecimal lastLatitude,
            BigDecimal lastLongitude,
            String driverName,
            String status) {
    }

    public record GpsLocationDTO(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal speed,
            BigDecimal heading,
            Instant recordedAt) {
    }

    public record LivePositionDTO(
            UUID id,
            String registrationNumber,
            String vehicleType,
            BigDecimal latitude,
            BigDecimal longitude,
            Instant lastGpsAt,
            String gpsStatus,
            String driverName) {
    }

    public record LiveMapDTO(
            List<LivePositionDTO> vehicles,
            long onlineCount,
            long totalCount,
            Instant timestamp) {
    }
}
