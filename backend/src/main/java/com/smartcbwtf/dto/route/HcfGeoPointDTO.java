package com.smartcbwtf.dto.route;

import java.util.List;
import java.util.UUID;

/**
 * DTO for map data - HCF geo points.
 */
public record HcfGeoPointDTO(
        UUID id,
        String code,
        String name,
        String address,
        Double gpsLat,
        Double gpsLon,
        String status,
        List<UUID> routeIds // Routes that include this HCF
) {
}
