package com.smartcbwtf.dto.route;

import com.smartcbwtf.domain.Route;
import com.smartcbwtf.domain.enums.RouteStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full Route detail DTO with waypoints and assignment history.
 */
public record RouteDetailDTO(
        UUID id,
        String name,
        String description,
        String color,
        RouteStatus status,
        Boolean isActive,
        List<RouteWaypointDTO> waypoints,
        RouteAssignmentDTO currentAssignment,
        List<RouteAssignmentDTO> assignmentHistory,
        Instant createdAt,
        Instant updatedAt) {
    public static RouteDetailDTO from(Route route, List<RouteAssignmentDTO> history) {
        var current = route.getCurrentAssignment();
        return new RouteDetailDTO(
                route.getId(),
                route.getName(),
                route.getDescription(),
                route.getColor(),
                route.getStatus(),
                route.getIsActive(),
                route.getWaypoints().stream().map(RouteWaypointDTO::from).toList(),
                current != null ? RouteAssignmentDTO.from(current) : null,
                history,
                route.getCreatedAt(),
                route.getUpdatedAt());
    }
}
