package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.UserGpsEvent;
import com.smartcbwtf.dto.AttendanceSyncRequest;
import com.smartcbwtf.dto.AttendanceSyncResponse;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.UserGpsEventRepository;
import com.smartcbwtf.service.AttendanceService;
import com.smartcbwtf.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Mobile API Controller for Android App.
 * 
 * ACCESS: DRIVER, PLANT_OPERATOR roles only
 * All operations use facility_id from JWT (never from request body).
 */
@RestController
@RequestMapping("/api/mobile")
@PreAuthorize("hasAnyRole('DRIVER', 'PLANT_OPERATOR')")
public class MobileApiController {

    private static final Logger log = LoggerFactory.getLogger(MobileApiController.class);

    private final AttendanceService attendanceService;
    private final UserGpsEventRepository gpsEventRepository;
    private final AppUserRepository appUserRepository;
    private final FacilityRepository facilityRepository;
    private final AuditLogService auditLogService;

    public MobileApiController(
            AttendanceService attendanceService,
            UserGpsEventRepository gpsEventRepository,
            AppUserRepository appUserRepository,
            FacilityRepository facilityRepository,
            AuditLogService auditLogService) {
        this.attendanceService = attendanceService;
        this.gpsEventRepository = gpsEventRepository;
        this.appUserRepository = appUserRepository;
        this.facilityRepository = facilityRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Get current user profile.
     */
    @GetMapping("/me")
    public ResponseEntity<MeResponse> getCurrentUser() {
        UUID userId = TenantContext.getUserId();
        UUID facilityId = TenantContext.getTenantId();

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalStateException("Facility not found"));

        return ResponseEntity.ok(new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                facility.getId(),
                facility.getCode(),
                facility.getName()));
    }

    /**
     * Mark attendance at HCF location.
     * Validates geofence, agreement status, and cooldown.
     */
    @PostMapping("/attendance/mark")
    public ResponseEntity<AttendanceSyncResponse> markAttendance(@RequestBody AttendanceSyncRequest request) {
        UUID userId = TenantContext.getUserId();
        UUID facilityId = TenantContext.getTenantId();

        log.debug("Attendance mark request from user {} with {} events", userId, request.getEvents().size());

        AttendanceSyncResponse response = attendanceService.sync(request, userId, facilityId);

        return ResponseEntity.ok(response);
    }

    /**
     * Push GPS location updates (batch support for offline sync).
     * Uses idempotency key per event.
     */
    @PostMapping("/gps/ping")
    public ResponseEntity<GpsPingResponse> pushGpsEvents(@RequestBody GpsPingRequest request) {
        UUID userId = TenantContext.getUserId();
        UUID facilityId = TenantContext.getTenantId();

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalStateException("Facility not found"));

        int successCount = 0;
        int duplicateCount = 0;
        List<UUID> successIds = new ArrayList<>();
        BigDecimal lastLat = null;
        BigDecimal lastLon = null;
        Instant lastRecordedAt = null;

        for (GpsEventItem event : request.events()) {
            // Idempotency check
            if (gpsEventRepository.existsByClientEventId(event.clientEventId())) {
                duplicateCount++;
                successIds.add(event.clientEventId());
                continue;
            }

            UserGpsEvent gpsEvent = new UserGpsEvent();
            gpsEvent.setStaffUser(user);
            gpsEvent.setFacility(facility);
            gpsEvent.setLatitude(event.latitude());
            gpsEvent.setLongitude(event.longitude());
            gpsEvent.setSpeed(event.speed());
            gpsEvent.setHeading(event.heading());
            gpsEvent.setAccuracyM(event.accuracyM());
            gpsEvent.setRecordedAt(event.recordedAt());
            gpsEvent.setReceivedAt(Instant.now());
            gpsEvent.setClientEventId(event.clientEventId());
            gpsEvent.setSource("ANDROID_APP");

            gpsEventRepository.save(gpsEvent);
            successCount++;
            successIds.add(event.clientEventId());

            // Track latest for user update
            if (lastRecordedAt == null || event.recordedAt().isAfter(lastRecordedAt)) {
                lastRecordedAt = event.recordedAt();
                lastLat = event.latitude();
                lastLon = event.longitude();
            }
        }

        // Update user's last known position
        if (lastLat != null && lastLon != null) {
            user.updateGpsPosition(lastLat, lastLon);
            appUserRepository.save(user);
        }

        // Audit (batch)
        if (successCount > 0) {
            auditLogService.log(
                    "GPS",
                    userId,
                    "GPS_PING_RECEIVED",
                    userId,
                    String.format("{\"count\":%d,\"duplicates\":%d}", successCount, duplicateCount));
        }

        log.debug("GPS ping: {} new, {} duplicates for user {}", successCount, duplicateCount, userId);

        return ResponseEntity.ok(new GpsPingResponse(
                request.events().size(),
                successCount,
                duplicateCount,
                successIds));
    }

    // ============ DTOs ============

    public record MeResponse(
            UUID id,
            String username,
            String fullName,
            String role,
            UUID facilityId,
            String facilityCode,
            String facilityName) {
    }

    public record GpsPingRequest(List<GpsEventItem> events) {
    }

    public record GpsEventItem(
            UUID clientEventId,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal speed,
            BigDecimal heading,
            BigDecimal accuracyM,
            Instant recordedAt) {
    }

    public record GpsPingResponse(
            int totalReceived,
            int successCount,
            int duplicateCount,
            List<UUID> successIds) {
    }
}
