package com.smartcbwtf.dto.route;

import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO for route alerts.
 */
public record RouteAlertDTO(
        String alertId,
        String routeId,
        String routeName,
        String routeColor,
        String cycleId,
        int cycleNumber,
        LocalDate cycleStart,
        LocalDate cycleEnd,
        String alertType,
        String severity,
        String title,
        String message,
        int missedHcfCount,
        String staffId,
        String staffName,
        boolean isResolved,
        String resolvedByName,
        Instant resolvedAt,
        String resolutionNotes,
        Instant createdAt) {
}
