package com.smartcbwtf.dto.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for audit log entries.
 */
public record TenantAuditDTO(
        UUID id,
        String action,
        String oldValue,
        String newValue,
        String performedBy,
        String performedByRole,
        Instant performedAt,
        String notes) {
    public static TenantAuditDTO from(com.smartcbwtf.domain.SubscriptionAudit audit) {
        return new TenantAuditDTO(
                audit.getId(),
                audit.getAction(),
                audit.getOldValue(),
                audit.getNewValue(),
                audit.getPerformedByUsername(),
                audit.getPerformedByRole(),
                audit.getCreatedAt(),
                audit.getNotes());
    }
}
