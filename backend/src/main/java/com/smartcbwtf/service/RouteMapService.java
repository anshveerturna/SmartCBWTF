package com.smartcbwtf.service;

import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.Route;
import com.smartcbwtf.dto.route.*;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.RouteRepository;
import com.smartcbwtf.repository.RouteWaypointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for map-related route data.
 * Supports filtering for performance at scale.
 */
@Service
public class RouteMapService {

    private final RouteRepository routeRepository;
    private final RouteWaypointRepository waypointRepository;
    private final AgreementRepository agreementRepository;

    public RouteMapService(
            RouteRepository routeRepository,
            RouteWaypointRepository waypointRepository,
            AgreementRepository agreementRepository) {
        this.routeRepository = routeRepository;
        this.waypointRepository = waypointRepository;
        this.agreementRepository = agreementRepository;
    }

    /**
     * Get all map data for a facility.
     * Supports filtering by routeId and activeOnly for performance.
     */
    @Transactional(readOnly = true)
    public RouteMapDataDTO getMapData(UUID facilityId, UUID routeId, Boolean activeOnly) {
        // Get routes
        List<Route> routes;
        if (routeId != null) {
            routes = routeRepository.findByIdWithWaypoints(routeId, facilityId)
                    .map(List::of)
                    .orElse(List.of());
        } else if (Boolean.TRUE.equals(activeOnly)) {
            routes = routeRepository.findActiveRoutesWithWaypoints(facilityId);
        } else {
            routes = routeRepository.findAllRoutesWithWaypoints(facilityId);
        }

        // Build route DTOs
        List<RouteWithWaypointsDTO> routeDTOs = routes.stream()
                .map(RouteWithWaypointsDTO::from)
                .toList();

        // Build mapping of HCF ID to route IDs
        Map<UUID, List<UUID>> hcfToRoutes = new HashMap<>();
        for (Route route : routes) {
            route.getWaypoints().forEach(wp -> {
                hcfToRoutes.computeIfAbsent(wp.getHcf().getId(), k -> new ArrayList<>())
                        .add(route.getId());
            });
        }

        // Get all HCFs with active agreements for the facility
        List<Hcf> hcfs;
        if (Boolean.TRUE.equals(activeOnly) && !hcfToRoutes.isEmpty()) {
            // Only show HCFs in routes when activeOnly is true
            hcfs = agreementRepository.findHcfsByFacilityId(facilityId).stream()
                    .filter(hcf -> hcfToRoutes.containsKey(hcf.getId()))
                    .toList();
        } else {
            hcfs = agreementRepository.findHcfsByFacilityId(facilityId);
        }

        // Build HCF DTOs with route associations
        List<HcfGeoPointDTO> hcfDTOs = hcfs.stream()
                .filter(hcf -> hcf.getGpsLat() != null && hcf.getGpsLon() != null)
                .map(hcf -> new HcfGeoPointDTO(
                        hcf.getId(),
                        hcf.getCode(),
                        hcf.getName(),
                        hcf.getAddress(),
                        hcf.getGpsLat(),
                        hcf.getGpsLon(),
                        hcf.getStatus(),
                        hcfToRoutes.getOrDefault(hcf.getId(), List.of())))
                .toList();

        return new RouteMapDataDTO(hcfDTOs, routeDTOs);
    }

    /**
     * Get all HCF geo points for a facility.
     */
    @Transactional(readOnly = true)
    public List<HcfGeoPointDTO> getAllHcfGeoPoints(UUID facilityId) {
        return agreementRepository.findHcfsByFacilityId(facilityId).stream()
                .filter(hcf -> hcf.getGpsLat() != null && hcf.getGpsLon() != null)
                .map(hcf -> new HcfGeoPointDTO(
                        hcf.getId(),
                        hcf.getCode(),
                        hcf.getName(),
                        hcf.getAddress(),
                        hcf.getGpsLat(),
                        hcf.getGpsLon(),
                        hcf.getStatus(),
                        List.of()))
                .toList();
    }
}
