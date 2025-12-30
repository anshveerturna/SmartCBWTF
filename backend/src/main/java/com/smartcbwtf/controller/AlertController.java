package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Alert;
import com.smartcbwtf.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Alert Controller.
 * Manages portal-visible alerts for CBWTF admins.
 */
@RestController
@RequestMapping("/api/cbwtf/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * List alerts with pagination and optional category filter.
     */
    @GetMapping
    public ResponseEntity<Page<AlertDTO>> listAlerts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "category", required = false) String category) {

        UUID facilityId = TenantContext.getTenantId();
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Alert> alerts;
        if (category != null && !category.isEmpty() && !category.equalsIgnoreCase("ALL")) {
            alerts = alertService.getAlertsByCategory(facilityId, category, pageable);
        } else {
            alerts = alertService.getAlerts(facilityId, pageable);
        }

        return ResponseEntity.ok(alerts.map(AlertDTO::from));
    }

    /**
     * Get unread alert count.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        UUID facilityId = TenantContext.getTenantId();
        long count = alertService.getUnreadCount(facilityId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark single alert as read.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        boolean success = alertService.markAsRead(id, facilityId);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Mark all alerts as read.
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead() {
        UUID facilityId = TenantContext.getTenantId();
        int count = alertService.markAllAsRead(facilityId);
        return ResponseEntity.ok(Map.of("marked", count));
    }

    /**
     * Alert DTO for API response.
     */
    public record AlertDTO(
            UUID id,
            String severity,
            String type,
            String title,
            String message,
            String relatedEntityType,
            UUID relatedEntityId,
            boolean isRead,
            String createdAt) {
        public static AlertDTO from(Alert a) {
            return new AlertDTO(
                    a.getId(),
                    a.getSeverity().name(),
                    a.getType().name(),
                    a.getTitle(),
                    a.getMessage(),
                    a.getRelatedEntityType(),
                    a.getRelatedEntityId(),
                    a.getIsRead(),
                    a.getCreatedAt().toString());
        }
    }
}
