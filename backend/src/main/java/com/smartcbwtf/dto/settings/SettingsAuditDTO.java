package com.smartcbwtf.dto.settings;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for settings audit log entries.
 */
public record SettingsAuditDTO(
        UUID id,
        String section,
        String settingKey,
        String oldValue,
        String newValue,
        UUID changedBy,
        String changedByUsername,
        Instant changedAt,
        String ipAddress) {
}
