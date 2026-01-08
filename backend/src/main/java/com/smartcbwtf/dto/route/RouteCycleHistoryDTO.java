package com.smartcbwtf.dto.route;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO for route cycle history entries.
 */
public record RouteCycleHistoryDTO(
        String cycleId,
        String routeId,
        String routeName,
        int cycleNumber,
        LocalDate cycleStart,
        LocalDate cycleEnd,
        int totalWaypoints,
        int completedWaypoints,
        int missedWaypoints,
        BigDecimal completionPercentage,
        String status,
        String staffId,
        String staffName,
        Instant completedAt,
        Instant createdAt) {
}
