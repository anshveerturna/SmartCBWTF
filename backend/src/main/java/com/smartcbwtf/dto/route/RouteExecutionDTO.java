package com.smartcbwtf.dto.route;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for current route execution state including active cycle and waypoint
 * statuses.
 */
public record RouteExecutionDTO(
        String routeId,
        String routeName,
        Integer completionDays,
        RouteCycleDTO activeCycle,
        List<ExecutionLogDTO> executionLogs) {
    public record RouteCycleDTO(
            String cycleId,
            int cycleNumber,
            LocalDate cycleStart,
            LocalDate cycleEnd,
            int totalWaypoints,
            int completedWaypoints,
            int missedWaypoints,
            BigDecimal completionPercentage,
            String status,
            String staffName,
            Instant completedAt) {
    }

    public record ExecutionLogDTO(
            String logId,
            String waypointId,
            String hcfId,
            String hcfName,
            String hcfCode,
            int sequenceOrder,
            String status,
            Instant visitedAt,
            String staffName) {
    }
}
