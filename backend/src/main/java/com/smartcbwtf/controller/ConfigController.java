package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.service.SubscriptionService;
import com.smartcbwtf.service.SystemConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Configuration endpoints for mobile apps and clients.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

        private final SubscriptionService subscriptionService;
        private final SystemConfigService systemConfigService;

        public ConfigController(SubscriptionService subscriptionService, SystemConfigService systemConfigService) {
                this.subscriptionService = subscriptionService;
                this.systemConfigService = systemConfigService;
        }

        /**
         * Get mobile app configuration for the current tenant.
         * Used by Android app to fetch feature flags and thresholds.
         */
        @GetMapping("/mobile")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<MobileConfigResponse> getMobileConfig() {
                UUID tenantId = TenantContext.getTenantId();

                // Get operational thresholds from system config
                Map<String, Object> thresholds = new HashMap<>();
                thresholds.put("geofenceRadiusMeters",
                                systemConfigService.getInt("operational.default_geofence_radius_meters", 100));
                thresholds.put("locationUpdateIntervalMinutes",
                                systemConfigService.getInt("operational.location_update_interval_minutes", 5));
                thresholds.put("attendanceDistanceToleranceMeters",
                                systemConfigService.getInt("operational.attendance_distance_tolerance_meters", 50));
                thresholds.put("maxVerificationDelayMinutes",
                                systemConfigService.getInt("operational.max_verification_delay_minutes", 30));
                thresholds.put("weightMismatchTolerancePercent",
                                systemConfigService.getInt("operational.weight_mismatch_tolerance_percent", 5));
                thresholds.put("blueWasteMinPercentage",
                                systemConfigService.getInt("operational.blue_waste_min_percentage", 55));

                // Platform info
                thresholds.put("platformName", systemConfigService.getString("platform.name", "SmartCBWTF"));
                thresholds.put("supportEmail",
                                systemConfigService.getString("platform.support_email", "support@smartcbwtf.com"));
                thresholds.put("supportPhone",
                                systemConfigService.getString("platform.support_phone", "+91-1800-XXX-XXXX"));

                // Safety controls
                thresholds.put("androidSyncDisabled",
                                systemConfigService.getBoolean("safety.disable_android_sync", false));
                thresholds.put("qrVerificationDisabled",
                                systemConfigService.getBoolean("safety.disable_qr_verification", false));

                // GPS tracking controls (for Android app)
                thresholds.put("gpsEnabled", systemConfigService.getBoolean("gps.enabled", true));
                thresholds.put("gpsPingIntervalMinutes",
                                systemConfigService.getInt("gps.ping_interval_minutes", 5));
                thresholds.put("gpsRequireForeground",
                                systemConfigService.getBoolean("gps.require_foreground", true));

                if (tenantId == null) {
                        // SuperAdmin or unauthenticated - return config without tenant-specific data
                        return ResponseEntity.ok(new MobileConfigResponse(
                                        "UNKNOWN",
                                        true,
                                        Map.of(),
                                        thresholds));
                }

                boolean isActive = subscriptionService.isActive(tenantId);
                Map<String, Boolean> features = subscriptionService.getEnabledFeatures(tenantId);

                String status = isActive ? "ACTIVE" : "INACTIVE";

                return ResponseEntity.ok(new MobileConfigResponse(
                                status,
                                isActive,
                                features,
                                thresholds));
        }

        /**
         * Response DTO for mobile config.
         */
        public record MobileConfigResponse(
                        String subscriptionStatus,
                        boolean active,
                        Map<String, Boolean> features,
                        Map<String, Object> thresholds) {
        }
}
