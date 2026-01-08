package com.smartcbwtf.dto.route;

import java.util.List;

/**
 * Complete map data DTO containing all HCFs and routes for map display.
 */
public record RouteMapDataDTO(
        List<HcfGeoPointDTO> hcfs,
        List<RouteWithWaypointsDTO> routes) {
}
