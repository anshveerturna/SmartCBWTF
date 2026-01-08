package com.smartcbwtf.service;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.Attendance;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.AttendanceSyncItem;
import com.smartcbwtf.dto.AttendanceSyncItemResult;
import com.smartcbwtf.dto.AttendanceSyncRequest;
import com.smartcbwtf.dto.AttendanceSyncResponse;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.AttendanceRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final HcfRepository hcfRepository;
    private final AppUserRepository appUserRepository;
    private final FacilityRepository facilityRepository;
    private final AuditLogService auditLogService;
    private final FeatureGuardService featureGuardService;
    private final AgreementGuardService agreementGuard;
    private final RouteExecutionService routeExecutionService;

    @Value("${app.attendance.geofence-radius-m:50}")
    private double geofenceRadiusM;

    @Value("${app.attendance.cooldown-minutes:5}")
    private int cooldownMinutes;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            HcfRepository hcfRepository,
            AppUserRepository appUserRepository,
            FacilityRepository facilityRepository,
            AuditLogService auditLogService,
            FeatureGuardService featureGuardService,
            AgreementGuardService agreementGuard,
            RouteExecutionService routeExecutionService) {
        this.attendanceRepository = attendanceRepository;
        this.hcfRepository = hcfRepository;
        this.appUserRepository = appUserRepository;
        this.facilityRepository = facilityRepository;
        this.auditLogService = auditLogService;
        this.featureGuardService = featureGuardService;
        this.agreementGuard = agreementGuard;
        this.routeExecutionService = routeExecutionService;
    }

    @Transactional
    public AttendanceSyncResponse sync(AttendanceSyncRequest request, UUID driverId, UUID facilityId) {
        // Feature flag enforcement at service layer (MANDATORY)
        featureGuardService.assertEnabled(facilityId, FeatureGuardService.ATTENDANCE_ENFORCEMENT);

        AttendanceSyncResponse response = new AttendanceSyncResponse();
        response.setTotalReceived(request.getEvents().size());
        response.setResults(new ArrayList<>());
        response.setSuccessIds(new ArrayList<>());

        int successCount = 0;
        int failureCount = 0;

        for (AttendanceSyncItem item : request.getEvents()) {
            AttendanceSyncItemResult result = processItem(item, driverId, facilityId);
            response.getResults().add(result);
            if (result.isSuccess()) {
                successCount++;
                response.getSuccessIds().add(item.getClientEventId());
            } else {
                failureCount++;
            }
        }

        response.setSuccessCount(successCount);
        response.setFailureCount(failureCount);
        return response;
    }

    private AttendanceSyncItemResult processItem(AttendanceSyncItem item, UUID driverId, UUID facilityId) {
        UUID clientEventId = item.getClientEventId();

        // Idempotency check
        if (attendanceRepository.existsByClientEventId(clientEventId)) {
            return AttendanceSyncItemResult.success(clientEventId); // Already processed
        }

        // Validate driver exists
        Optional<AppUser> driverOpt = appUserRepository.findById(driverId);
        if (driverOpt.isEmpty()) {
            return AttendanceSyncItemResult.error(clientEventId, "DRIVER_NOT_FOUND", "Driver user not found");
        }
        AppUser driver = driverOpt.get();

        // Validate HCF exists
        Optional<Hcf> hcfOpt = hcfRepository.findById(item.getHcfId());
        if (hcfOpt.isEmpty()) {
            return AttendanceSyncItemResult.error(clientEventId, "HCF_NOT_FOUND", "Healthcare Facility not found");
        }
        Hcf hcf = hcfOpt.get();

        // AGREEMENT CHECK: Staff can only mark attendance for HCFs with ACTIVE
        // agreement
        try {
            agreementGuard.getActiveAgreement(hcf.getId(), facilityId);
        } catch (Exception e) {
            return AttendanceSyncItemResult.error(
                    clientEventId,
                    "AGREEMENT_NOT_ACTIVE",
                    "No active agreement exists for this HCF");
        }

        // Geofence validation
        double distance = haversineMeters(
                item.getGpsLat(), item.getGpsLon(),
                hcf.getGpsLat(), hcf.getGpsLon());
        if (distance > geofenceRadiusM) {
            return AttendanceSyncItemResult.error(
                    clientEventId,
                    "OUT_OF_GEOFENCE",
                    String.format("Location is %.1fm from HCF, exceeds %.0fm limit", distance, geofenceRadiusM));
        }

        // Cooldown validation (server-side enforcement)
        Instant cooldownStart = item.getEventTs().minus(Duration.ofMinutes(cooldownMinutes));
        if (attendanceRepository.existsByDriverIdAndEventTsAfter(driverId, cooldownStart)) {
            Optional<Attendance> lastAttendance = attendanceRepository.findLatestByDriverId(driverId);
            if (lastAttendance.isPresent()) {
                long remainingMs = Duration.between(
                        Instant.now(),
                        lastAttendance.get().getEventTs().plus(Duration.ofMinutes(cooldownMinutes))).toMillis();
                if (remainingMs > 0) {
                    return AttendanceSyncItemResult.cooldownError(clientEventId, remainingMs);
                }
            }
        }

        // Create and save attendance record
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalStateException("Facility not found"));

        Attendance attendance = new Attendance();
        attendance.setDriver(driver);
        attendance.setHcf(hcf);
        attendance.setFacility(facility); // Denormalized for efficient queries
        attendance.setEventTs(item.getEventTs());
        attendance.setGpsLat(item.getGpsLat());
        attendance.setGpsLon(item.getGpsLon());
        attendance.setGpsAccuracyM(item.getGpsAccuracyM());
        attendance.setAppDeviceId(item.getAppDeviceId());
        attendance.setDistanceFromHcfM(distance);
        attendance.setClientEventId(clientEventId);

        attendance = attendanceRepository.save(attendance);

        // Update route execution logs if staff is assigned to routes with this HCF
        try {
            routeExecutionService.onAttendanceMarked(attendance);
        } catch (Exception e) {
            // Log but don't fail attendance - route tracking is secondary
            // This prevents circular dependency issues at startup
        }

        // Audit log
        auditLogService.log(
                "ATTENDANCE",
                attendance.getId(),
                "MARKED",
                driverId,
                String.format("{\"hcfId\":\"%s\",\"hcfName\":\"%s\",\"distanceM\":%.1f}",
                        hcf.getId(), hcf.getName(), distance));

        return AttendanceSyncItemResult.success(clientEventId);
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371000.0; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }
}
