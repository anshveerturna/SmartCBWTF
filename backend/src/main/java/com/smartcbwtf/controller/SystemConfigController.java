package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.SystemConfig;
import com.smartcbwtf.domain.SystemConfigAudit;
import com.smartcbwtf.service.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for System Configuration management.
 * All endpoints require SUPER_ADMIN role except mobile config.
 */
@RestController
@RequestMapping("/api")
public class SystemConfigController {

    private final SystemConfigService configService;

    public SystemConfigController(SystemConfigService configService) {
        this.configService = configService;
    }

    // ========== ADMIN ENDPOINTS (SUPER_ADMIN ONLY) ==========

    /**
     * Get all configurations grouped by category.
     */
    @GetMapping("/admin/system-config")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, List<SystemConfigDTO>>> getAllConfigs() {
        Map<String, List<SystemConfig>> grouped = configService.getAllGroupedByCategory();

        Map<String, List<SystemConfigDTO>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<SystemConfig>> entry : grouped.entrySet()) {
            result.put(entry.getKey(),
                    entry.getValue().stream().map(this::toDTO).collect(Collectors.toList()));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get configurations for a specific category.
     */
    @GetMapping("/admin/system-config/category/{category}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<SystemConfigDTO>> getByCategory(@PathVariable("category") String category) {
        List<SystemConfig> configs = configService.getByCategory(category.toUpperCase());
        return ResponseEntity.ok(configs.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    /**
     * Get a single configuration by key.
     */
    @GetMapping("/admin/system-config/key/{key}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SystemConfigDTO> getByKey(@PathVariable("key") String key) {
        return configService.getByKey(key)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a single configuration.
     */
    @PutMapping("/admin/system-config/key/{key}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SystemConfigDTO> updateConfig(
            @PathVariable("key") String key,
            @Valid @RequestBody UpdateConfigRequest request,
            HttpServletRequest httpRequest) {

        TenantContext.TenantInfo info = TenantContext.get();
        UUID userId = info != null ? info.userId() : null;
        String ipAddress = getClientIp(httpRequest);

        try {
            SystemConfig updated = configService.updateConfig(
                    key, request.value(), userId, request.reason(), ipAddress);
            return ResponseEntity.ok(toDTO(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Bulk update configurations for a category.
     */
    @PutMapping("/admin/system-config/category/{category}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BulkUpdateResponse> bulkUpdateCategory(
            @PathVariable("category") String category,
            @Valid @RequestBody BulkUpdateRequest request,
            HttpServletRequest httpRequest) {

        TenantContext.TenantInfo info = TenantContext.get();
        UUID userId = info != null ? info.userId() : null;
        String ipAddress = getClientIp(httpRequest);

        try {
            List<SystemConfig> updated = configService.bulkUpdateCategory(
                    category.toUpperCase(), request.updates(), userId, request.reason(), ipAddress);
            return ResponseEntity.ok(new BulkUpdateResponse(
                    updated.size(),
                    "Successfully updated " + updated.size() + " configuration(s)"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new BulkUpdateResponse(0, e.getMessage()));
        }
    }

    /**
     * Get audit history for a configuration key.
     */
    @GetMapping("/admin/system-config/key/{key}/audit")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<ConfigAuditDTO>> getAuditHistory(@PathVariable("key") String key) {
        List<SystemConfigAudit> audits = configService.getAuditHistory(key);
        return ResponseEntity.ok(audits.stream().map(this::toAuditDTO).collect(Collectors.toList()));
    }

    /**
     * Get recent configuration changes.
     */
    @GetMapping("/admin/system-config/audit/recent")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<ConfigAuditDTO>> getRecentChanges() {
        List<SystemConfigAudit> audits = configService.getRecentChanges();
        return ResponseEntity.ok(audits.stream().map(this::toAuditDTO).collect(Collectors.toList()));
    }

    /**
     * Refresh configuration cache.
     */
    @PostMapping("/admin/system-config/refresh-cache")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> refreshCache() {
        configService.refreshCache();
        return ResponseEntity.ok(Map.of("message", "Cache refreshed successfully"));
    }

    // NOTE: Mobile config endpoint is in ConfigController

    // ========== DTOs ==========

    private SystemConfigDTO toDTO(SystemConfig config) {
        return new SystemConfigDTO(
                config.getId(),
                config.getConfigKey(),
                config.isSensitive() ? "********" : config.getConfigValue(),
                config.getValueType(),
                config.getCategory(),
                config.getDisplayName(),
                config.getDescription(),
                config.isSensitive(),
                config.isRequiresConfirmation(),
                config.isReadonly(),
                config.getValidationRules(),
                config.getVersion(),
                config.getUpdatedBy() != null ? config.getUpdatedBy().getName() : null,
                config.getUpdatedAt() != null ? config.getUpdatedAt().toString() : null);
    }

    private ConfigAuditDTO toAuditDTO(SystemConfigAudit audit) {
        return new ConfigAuditDTO(
                audit.getId(),
                audit.getConfigKey(),
                audit.getOldValue(),
                audit.getNewValue(),
                audit.getChangedBy() != null ? audit.getChangedBy().getName() : null,
                audit.getChangedAt().toString(),
                audit.getReason(),
                audit.getIpAddress());
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ========== Record Types ==========

    public record UpdateConfigRequest(
            @NotBlank String value,
            String reason) {
    }

    public record BulkUpdateRequest(
            Map<String, String> updates,
            String reason) {
    }

    public record BulkUpdateResponse(int updatedCount, String message) {
    }

    public record SystemConfigDTO(
            UUID id,
            String key,
            String value,
            String valueType,
            String category,
            String displayName,
            String description,
            boolean isSensitive,
            boolean requiresConfirmation,
            boolean isReadonly,
            Map<String, Object> validationRules,
            int version,
            String updatedBy,
            String updatedAt) {
    }

    public record ConfigAuditDTO(
            UUID id,
            String key,
            String oldValue,
            String newValue,
            String changedBy,
            String changedAt,
            String reason,
            String ipAddress) {
    }
}
