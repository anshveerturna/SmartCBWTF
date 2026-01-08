package com.smartcbwtf.dto.route;

import com.smartcbwtf.domain.Route;
import com.smartcbwtf.domain.enums.RouteStatus;

import java.util.List;
import java.util.UUID;

/**
 * Route DTO with embedded waypoint coordinates for map polyline display.
 */
public record RouteWithWaypointsDTO(
        UUID id,
        String name,
        String color,
        RouteStatus status,
        Boolean isActive,
        String assignedStaffName,
        List<WaypointCoordinate> coordinates) {
    public record WaypointCoordinate(
            Integer order,
            UUID hcfId,
            String hcfName,
            Double lat,
            Double lon) {
    }

    public static RouteWithWaypointsDTO from(Route route) {
        var currentAssignment = route.getCurrentAssignment();
        return new RouteWithWaypointsDTO(
                route.getId(),
                route.getName(),
                route.getColor(),
                route.getStatus(),
                route.getIsActive(),
                currentAssignment != null && currentAssignment.getStaff() != null
                        ? currentAssignment.getStaff().getName()
                        : null,
                route.getWaypoints().stream()
                        .map(w -> new WaypointCoordinate(
                                w.getSequenceOrder(),
                                w.getHcf().getId(),
                                w.getHcf().getName(),
                                w.getHcf().getGpsLat(),
                                w.getHcf().getGpsLon()))
                        .toList());
    }
}
