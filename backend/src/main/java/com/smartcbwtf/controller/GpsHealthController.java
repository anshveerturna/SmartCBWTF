package com.smartcbwtf.controller;

import com.smartcbwtf.service.GpsIngestionHealthService;
import com.smartcbwtf.service.GpsIngestionHealthService.HealthStatus;
import com.smartcbwtf.service.GpsIngestionHealthService.IngestionHealthDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * GPS Health Controller - provides observability into GPS ingestion status.
 * 
 * ACCESS: SUPER_ADMIN only. READ-ONLY.
 * CBWTF admins cannot see or access this API.
 */
@RestController
@RequestMapping("/api/internal/gps/health")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class GpsHealthController {

    private static final Logger log = LoggerFactory.getLogger(GpsHealthController.class);

    private final GpsIngestionHealthService healthService;

    public GpsHealthController(GpsIngestionHealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * Get ingestion health status for all facilities.
     * Returns derived HEALTHY/DEGRADED/DOWN status based on timing rules.
     */
    @GetMapping
    public ResponseEntity<HealthSummaryResponse> getAllHealth() {
        log.debug("GET /api/internal/gps/health - fetching all facility health");

        List<IngestionHealthDTO> healthList = healthService.getAllHealth();

        long healthy = healthList.stream().filter(h -> h.status() == HealthStatus.HEALTHY).count();
        long degraded = healthList.stream().filter(h -> h.status() == HealthStatus.DEGRADED).count();
        long down = healthList.stream().filter(h -> h.status() == HealthStatus.DOWN).count();

        return ResponseEntity.ok(new HealthSummaryResponse(
                healthList,
                healthList.size(),
                (int) healthy,
                (int) degraded,
                (int) down,
                Instant.now()));
    }

    /**
     * Get ingestion health status for a specific facility.
     */
    @GetMapping("/{facilityId}")
    public ResponseEntity<FacilityHealthResponse> getHealthByFacility(@PathVariable UUID facilityId) {
        log.debug("GET /api/internal/gps/health/{} - fetching facility health", facilityId);

        List<IngestionHealthDTO> healthList = healthService.getHealthByFacility(facilityId);

        if (healthList.isEmpty()) {
            return ResponseEntity.ok(new FacilityHealthResponse(
                    facilityId,
                    null,
                    List.of(),
                    HealthStatus.DOWN,
                    "No ingestion logs found for this facility",
                    Instant.now()));
        }

        // Derive overall facility status (worst of all vendors)
        HealthStatus overallStatus = healthList.stream()
                .map(IngestionHealthDTO::status)
                .reduce(HealthStatus.HEALTHY, (a, b) -> {
                    if (a == HealthStatus.DOWN || b == HealthStatus.DOWN)
                        return HealthStatus.DOWN;
                    if (a == HealthStatus.DEGRADED || b == HealthStatus.DEGRADED)
                        return HealthStatus.DEGRADED;
                    return HealthStatus.HEALTHY;
                });

        String facilityName = healthList.isEmpty() ? null : healthList.get(0).facilityName();

        return ResponseEntity.ok(new FacilityHealthResponse(
                facilityId,
                facilityName,
                healthList,
                overallStatus,
                null,
                Instant.now()));
    }

    // ============ Response DTOs ============

    public record HealthSummaryResponse(
            List<IngestionHealthDTO> facilities,
            int totalFacilities,
            int healthyCount,
            int degradedCount,
            int downCount,
            Instant timestamp) {
    }

    public record FacilityHealthResponse(
            UUID facilityId,
            String facilityName,
            List<IngestionHealthDTO> vendors,
            HealthStatus overallStatus,
            String message,
            Instant timestamp) {
    }
}
