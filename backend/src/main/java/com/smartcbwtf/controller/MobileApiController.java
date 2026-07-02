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
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
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

        return privateMeResponse(new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                facility.getId(),
                facility.getCode(),
                facility.getName()));
    }

    private static ResponseEntity<MeResponse> privateMeResponse(MeResponse body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    /**
     * Mark attendance at HCF location.
     * Validates geofence, agreement status, and cooldown.
     */
    @PostMapping("/attendance/mark")
    public ResponseEntity<AttendanceSyncResponse> markAttendance(@Valid @RequestBody AttendanceSyncRequest request) {
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
    public ResponseEntity<GpsPingResponse> pushGpsEvents(@Valid @RequestBody GpsPingRequest request) {
        UUID userId = TenantContext.getUserId();
        UUID facilityId = TenantContext.getTenantId();

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalStateException("Facility not found"));

        int successCount = 0;
        int duplicateCount = 0;
        List<UUID> successIds = new ArrayList<>();
        Set<UUID> existingClientEventIds = new HashSet<>(gpsEventRepository.findExistingClientEventIds(
                request.events().stream()
                        .map(GpsEventItem::clientEventId)
                        .distinct()
                        .toList()));
        Set<UUID> acceptedClientEventIds = new HashSet<>();
        BigDecimal lastLat = null;
        BigDecimal lastLon = null;
        Instant lastRecordedAt = null;

        for (GpsEventItem event : request.events()) {
            UUID clientEventId = event.clientEventId();
            if (existingClientEventIds.contains(clientEventId) || !acceptedClientEventIds.add(clientEventId)) {
                duplicateCount++;
                successIds.add(clientEventId);
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
            gpsEvent.setClientEventId(clientEventId);
            gpsEvent.setSource("ANDROID_APP");

            try {
                gpsEventRepository.save(gpsEvent);
                successCount++;
                successIds.add(clientEventId);
            } catch (DataIntegrityViolationException e) {
                duplicateCount++;
                successIds.add(clientEventId);
                existingClientEventIds.add(clientEventId);
                continue;
            }

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

    public record GpsPingRequest(@NotEmpty @Size(max = 500) List<@Valid GpsEventItem> events) {
    }

    public record GpsEventItem(
            @NotNull UUID clientEventId,
            @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,
            @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude,
            @DecimalMin(value = "0.0") BigDecimal speed,
            @DecimalMin(value = "0.0") @DecimalMax(value = "360.0") BigDecimal heading,
            @DecimalMin(value = "0.0") BigDecimal accuracyM,
            @NotNull @PastOrPresent Instant recordedAt) {
    }

    public record GpsPingResponse(
            int totalReceived,
            int successCount,
            int duplicateCount,
            List<UUID> successIds) {
    }
}
