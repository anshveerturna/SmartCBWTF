package com.smartcbwtf.dto.mobile;

import com.smartcbwtf.domain.RouteWaypoint;

/**
 * DTO for mobile app - waypoint (HCF) in staff's assigned route.
 */
public record MobileWaypointDTO(
        String waypointId,
        Integer sequenceOrder,
        String hcfId,
        String hcfCode,
        String hcfName,
        String hcfAddress,
        Double gpsLat,
        Double gpsLon,
        Boolean attendanceMarked) {

    public static MobileWaypointDTO from(RouteWaypoint waypoint, boolean attendanceMarked) {
        var hcf = waypoint.getHcf();
        return new MobileWaypointDTO(
                waypoint.getId().toString(),
                waypoint.getSequenceOrder(),
                hcf.getId().toString(),
                hcf.getCode(),
                hcf.getName(),
                hcf.getAddress(),
                hcf.getGpsLat(),
                hcf.getGpsLon(),
                attendanceMarked);
    }
}
