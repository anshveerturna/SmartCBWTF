package com.smartcbwtf.controller;

import com.smartcbwtf.domain.SubscriptionAudit;
import com.smartcbwtf.repository.SubscriptionAuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static com.smartcbwtf.util.PaginationUtils.pageRequest;

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
    private static final int MAX_AUDIT_FILTER_LENGTH = 80;

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

        Pageable pageable = pageRequest(page, size, 50, Sort.by("createdAt").descending());
        String normalizedEntityType = normalizeFilter(entityType, "entityType");
        String normalizedAction = normalizeFilter(action, "action");
        validateDateRange(from, to);
        Page<SubscriptionAudit> audits = auditRepository.searchAuditLogs(normalizedEntityType, normalizedAction,
                actorId, from, to, pageable);

        return privateResponse(audits.map(AuditLogDTO::from));
    }

    /**
     * Get recent audit logs (last 50).
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentAuditLogs() {
        Pageable pageable = PageRequest.of(0, 50, Sort.by("createdAt").descending());
        Page<SubscriptionAudit> audits = auditRepository.findAll(pageable);
        return privateResponse(audits.map(AuditLogDTO::from));
    }

    /**
     * Get audit logs for SuperAdmin actions only.
     */
    @GetMapping("/superadmin-actions")
    public ResponseEntity<Page<AuditLogDTO>> getSuperAdminActions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {

        Pageable pageable = pageRequest(page, size, 50, Sort.by("createdAt").descending());
        Page<SubscriptionAudit> audits = auditRepository.findByPerformedByRoleOrderByCreatedAtDesc(
                "SUPER_ADMIN", pageable);
        return privateResponse(audits.map(AuditLogDTO::from));
    }

    private static <T> ResponseEntity<T> privateResponse(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    private static String normalizeFilter(String value, String label) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > MAX_AUDIT_FILTER_LENGTH) {
            throw new IllegalArgumentException(label + " must be " + MAX_AUDIT_FILTER_LENGTH
                    + " characters or fewer");
        }
        for (int i = 0; i < normalized.length(); i++) {
            if (Character.isISOControl(normalized.charAt(i))) {
                throw new IllegalArgumentException(label + " contains unsupported control characters");
            }
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static void validateDateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
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
