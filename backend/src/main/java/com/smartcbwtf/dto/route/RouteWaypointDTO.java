package com.smartcbwtf.dto.route;

import com.smartcbwtf.domain.RouteWaypoint;

import java.util.UUID;

/**
 * Waypoint DTO with HCF details.
 */
public record RouteWaypointDTO(
        UUID id,
        Integer sequenceOrder,
        Integer estimatedStopMinutes,
        UUID hcfId,
        String hcfCode,
        String hcfName,
        String hcfAddress,
        Double gpsLat,
        Double gpsLon) {
    public static RouteWaypointDTO from(RouteWaypoint waypoint) {
        var hcf = waypoint.getHcf();
        return new RouteWaypointDTO(
                waypoint.getId(),
                waypoint.getSequenceOrder(),
                waypoint.getEstimatedStopMinutes(),
                hcf.getId(),
                hcf.getCode(),
                hcf.getName(),
                hcf.getAddress(),
                hcf.getGpsLat(),
                hcf.getGpsLon());
    }
}
