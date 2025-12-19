package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.service.SubscriptionService;
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

    public ConfigController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * Get mobile app configuration for the current tenant.
     * Used by Android app to fetch feature flags and thresholds.
     */
    @GetMapping("/mobile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MobileConfigResponse> getMobileConfig() {
        UUID tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            // SuperAdmin or unauthenticated - return minimal config
            return ResponseEntity.ok(new MobileConfigResponse(
                    "UNKNOWN",
                    true,
                    Map.of(),
                    Map.of()));
        }

        boolean isActive = subscriptionService.isActive(tenantId);
        Map<String, Boolean> features = subscriptionService.getEnabledFeatures(tenantId);

        // Default thresholds (could be per-tenant in future)
        Map<String, Object> thresholds = Map.of(
                "maxPhotoSizeMb", 5,
                "locationAccuracyMeters", 50,
                "syncIntervalMinutes", 15,
                "offlineModeMaxDays", 7);

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
