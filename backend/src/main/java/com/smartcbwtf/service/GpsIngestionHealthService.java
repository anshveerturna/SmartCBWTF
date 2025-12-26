package com.smartcbwtf.service;

import com.smartcbwtf.domain.GpsIngestionLog;
import com.smartcbwtf.repository.GpsIngestionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GPS Ingestion Health Service - provides observability into GPS data ingestion
 * status.
 * 
 * Used by SUPER_ADMIN and internal operations only.
 * READ-ONLY service - no mutations to ingestion logs allowed here.
 */
@Service
@Transactional(readOnly = true)
public class GpsIngestionHealthService {

    private static final Logger log = LoggerFactory.getLogger(GpsIngestionHealthService.class);

    // Health thresholds
    private static final int HEALTHY_THRESHOLD_MINUTES = 15;
    private static final int DEGRADED_THRESHOLD_MINUTES = 60;
    private static final int FAILURE_COUNT_WARNING = 3;

    private final GpsIngestionLogRepository ingestionLogRepository;

    public GpsIngestionHealthService(GpsIngestionLogRepository ingestionLogRepository) {
        this.ingestionLogRepository = ingestionLogRepository;
    }

    /**
     * Get health status for all facilities.
     */
    public List<IngestionHealthDTO> getAllHealth() {
        List<GpsIngestionLog> logs = ingestionLogRepository.findAll();
        return logs.stream()
                .map(this::toHealthDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get health status for a specific facility.
     */
    public List<IngestionHealthDTO> getHealthByFacility(UUID facilityId) {
        List<GpsIngestionLog> logs = ingestionLogRepository.findByFacilityId(facilityId);
        return logs.stream()
                .map(this::toHealthDTO)
                .collect(Collectors.toList());
    }

    /**
     * Compute derived health status based on timing and failure counts.
     */
    private HealthStatus computeHealthStatus(GpsIngestionLog log) {
        Instant now = Instant.now();
        Instant lastSuccess = log.getLastSuccessAt();
        Instant lastFailure = log.getLastFailureAt();
        Long failureCount = log.getFailureCount();

        // No success ever = DOWN
        if (lastSuccess == null) {
            return HealthStatus.DOWN;
        }

        long minutesSinceSuccess = ChronoUnit.MINUTES.between(lastSuccess, now);

        // Success within 15 minutes = HEALTHY (unless high failure rate)
        if (minutesSinceSuccess <= HEALTHY_THRESHOLD_MINUTES) {
            if (failureCount != null && failureCount >= FAILURE_COUNT_WARNING) {
                return HealthStatus.DEGRADED;
            }
            return HealthStatus.HEALTHY;
        }

        // Success within 1 hour but failures exist = DEGRADED
        if (minutesSinceSuccess <= DEGRADED_THRESHOLD_MINUTES) {
            return HealthStatus.DEGRADED;
        }

        // No success in over 1 hour = DOWN
        return HealthStatus.DOWN;
    }

    private IngestionHealthDTO toHealthDTO(GpsIngestionLog log) {
        return new IngestionHealthDTO(
                log.getId(),
                log.getFacility().getId(),
                log.getFacility().getName(),
                log.getVendor(),
                computeHealthStatus(log),
                log.getLastSuccessAt(),
                log.getLastFailureAt(),
                log.getFailureCount() != null ? log.getFailureCount() : 0L,
                log.getLastFailureReason(),
                log.getSuccessCount() != null ? log.getSuccessCount() : 0L,
                log.getUpdatedAt());
    }

    // ============ DTOs ============

    public enum HealthStatus {
        HEALTHY, // GPS data flowing normally
        DEGRADED, // Some issues but still receiving data
        DOWN // No data in over 1 hour
    }

    public record IngestionHealthDTO(
            UUID id,
            UUID facilityId,
            String facilityName,
            String vendor,
            HealthStatus status,
            Instant lastSuccessAt,
            Instant lastFailureAt,
            long failureCount,
            String lastFailureReason,
            long successCount,
            Instant lastUpdated) {
    }
}
