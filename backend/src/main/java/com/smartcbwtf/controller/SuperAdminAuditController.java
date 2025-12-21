package com.smartcbwtf.controller;

import com.smartcbwtf.domain.SubscriptionAudit;
import com.smartcbwtf.repository.SubscriptionAuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * SuperAdmin audit log viewer.
 * READ-ONLY - No mutation endpoints.
 * 
 * Displays immutable system audit records for SuperAdmin activity.
 */
@RestController
@RequestMapping("/api/superadmin/audit-logs")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminAuditController {

    private final SubscriptionAuditRepository auditRepository;

    public SuperAdminAuditController(SubscriptionAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * List audit logs with filtering and pagination.
     * 
     * @param entityType Filter by entity type (USER, FACILITY, etc.)
     * @param action     Filter by action type
     * @param actorId    Filter by performer
     * @param from       Start date filter
     * @param to         End date filter
     * @param page       Page number
     * @param size       Page size
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogs(
            @RequestParam(name = "entityType", required = false) String entityType,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "actorId", required = false) UUID actorId,
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "to", required = false) Instant to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SubscriptionAudit> audits;

        // Apply filters in priority order
        if (actorId != null) {
            audits = auditRepository.findByPerformedByOrderByCreatedAtDesc(actorId, pageable);
        } else if (action != null && !action.isBlank()) {
            audits = auditRepository.findByAction(action.toUpperCase(), pageable);
        } else if (entityType != null && !entityType.isBlank()) {
            audits = auditRepository.findByEntityTypeOrderByCreatedAtDesc(entityType.toUpperCase(), pageable);
        } else {
            audits = auditRepository.findAll(pageable);
        }

        return ResponseEntity.ok(audits.map(AuditLogDTO::from));
    }

    /**
     * Get recent audit logs (last 50).
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentAuditLogs() {
        Pageable pageable = PageRequest.of(0, 50, Sort.by("createdAt").descending());
        Page<SubscriptionAudit> audits = auditRepository.findAll(pageable);
        return ResponseEntity.ok(audits.map(AuditLogDTO::from));
    }

    /**
     * Get audit logs for SuperAdmin actions only.
     */
    @GetMapping("/superadmin-actions")
    public ResponseEntity<Page<AuditLogDTO>> getSuperAdminActions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SubscriptionAudit> audits = auditRepository.findByPerformedByRoleOrderByCreatedAtDesc(
                "SUPER_ADMIN", pageable);
        return ResponseEntity.ok(audits.map(AuditLogDTO::from));
    }

    // DTO
    public record AuditLogDTO(
            UUID id,
            String entityType,
            UUID entityId,
            String action,
            String oldValue,
            String newValue,
            UUID actorId,
            String actorUsername,
            String actorRole,
            String notes,
            Instant createdAt) {

        public static AuditLogDTO from(SubscriptionAudit audit) {
            return new AuditLogDTO(
                    audit.getId(),
                    audit.getEntityType(),
                    audit.getEntityId(),
                    audit.getAction(),
                    audit.getOldValue(),
                    audit.getNewValue(),
                    audit.getPerformedBy(),
                    audit.getPerformedByUsername(),
                    audit.getPerformedByRole(),
                    audit.getNotes(),
                    audit.getCreatedAt());
        }
    }
}
