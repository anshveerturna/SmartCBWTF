package com.smartcbwtf.dto.route;

import com.smartcbwtf.domain.Route;
import com.smartcbwtf.domain.enums.RouteStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Basic Route DTO for list views.
 */
public record RouteDTO(
                UUID id,
                String name,
                String description,
                String color,
                RouteStatus status,
                Boolean isActive,
                Integer waypointCount,
                String assignedStaffName,
                UUID assignedStaffId,
                Integer completionDays,
                LocalDate cycleStartDate,
                Instant createdAt,
                Instant updatedAt) {
        public static RouteDTO from(Route route) {
                var currentAssignment = route.getCurrentAssignment();
                return new RouteDTO(
                                route.getId(),
                                route.getName(),
                                route.getDescription(),
                                route.getColor(),
                                route.getStatus(),
                                route.getIsActive(),
                                route.getWaypoints().size(),
                                currentAssignment != null && currentAssignment.getStaff() != null
                                                ? currentAssignment.getStaff().getName()
                                                : null,
                                currentAssignment != null && currentAssignment.getStaff() != null
                                                ? currentAssignment.getStaff().getId()
                                                : null,
                                route.getCompletionDays(),
                                route.getCycleStartDate(),
                                route.getCreatedAt(),
                                route.getUpdatedAt());
        }
}
