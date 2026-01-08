package com.smartcbwtf.dto.mobile;

import java.util.List;

/**
 * DTO for mobile app - staff's assigned route with waypoints.
 */
public record MobileRouteDTO(
        String routeId,
        String routeName,
        String routeColor,
        Integer completionDays,
        String facilityName,
        List<MobileWaypointDTO> waypoints) {
}
