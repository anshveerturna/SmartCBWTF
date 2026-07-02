package com.smartcbwtf.service;

import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.Route;
import com.smartcbwtf.domain.RouteWaypoint;
import com.smartcbwtf.dto.route.RouteWaypointDTO;
import com.smartcbwtf.dto.route.SetWaypointsRequest;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.RouteRepository;
import com.smartcbwtf.repository.RouteWaypointRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing route waypoints.
 * Enforces contiguous ordering (1, 2, 3, ..., N).
 */
@Service
public class RouteWaypointService {

    private static final Logger log = LoggerFactory.getLogger(RouteWaypointService.class);

    private final RouteRepository routeRepository;
    private final RouteWaypointRepository waypointRepository;
    private final HcfRepository hcfRepository;

    public RouteWaypointService(
            RouteRepository routeRepository,
            RouteWaypointRepository waypointRepository,
            HcfRepository hcfRepository) {
        this.routeRepository = routeRepository;
        this.waypointRepository = waypointRepository;
        this.hcfRepository = hcfRepository;
    }

    /**
     * Replace all waypoints for a route with new ordered list.
     * Validates contiguous ordering and HCF existence.
     */
    @Transactional
    public List<RouteWaypointDTO> setWaypoints(UUID routeId, UUID facilityId, SetWaypointsRequest request) {
        Route route = routeRepository.findByIdAndFacilityId(routeId, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found: " + routeId));

        List<UUID> hcfIds = request.hcfIds();

        // Validate no duplicates
        Set<UUID> uniqueIds = new HashSet<>(hcfIds);
        if (uniqueIds.size() != hcfIds.size()) {
            throw new IllegalArgumentException("Duplicate HCF IDs are not allowed in a route");
        }

        // Validate all HCFs are actively linked to this facility before changing the route.
        List<Hcf> hcfs = hcfRepository.findActiveByFacilityIdAndIdIn(facilityId, hcfIds);
        if (hcfs.size() != hcfIds.size()) {
            throw new EntityNotFoundException("One or more HCFs are not active for this facility");
        }

        Map<UUID, Hcf> hcfMap = new HashMap<>();
        for (Hcf hcf : hcfs) {
            hcfMap.put(hcf.getId(), hcf);
        }

        // Delete existing waypoints
        waypointRepository.deleteAllByRouteId(routeId);

        // Create new waypoints with contiguous order
        List<RouteWaypoint> newWaypoints = new ArrayList<>();
        for (int i = 0; i < hcfIds.size(); i++) {
            UUID hcfId = hcfIds.get(i);
            Hcf hcf = hcfMap.get(hcfId);

            RouteWaypoint waypoint = new RouteWaypoint();
            waypoint.setRoute(route);
            waypoint.setHcf(hcf);
            waypoint.setSequenceOrder(i + 1); // 1-indexed
            waypoint.setIsActive(true);

            newWaypoints.add(waypoint);
        }

        List<RouteWaypoint> saved = waypointRepository.saveAll(newWaypoints);
        log.info("Set {} waypoints for route '{}' (id: {})", saved.size(), route.getName(), routeId);

        return saved.stream()
                .map(RouteWaypointDTO::from)
                .toList();
    }

    /**
     * Get waypoints for a route.
     */
    @Transactional(readOnly = true)
    public List<RouteWaypointDTO> getWaypoints(UUID routeId) {
        return waypointRepository.findByRouteIdWithHcf(routeId)
                .stream()
                .map(RouteWaypointDTO::from)
                .toList();
    }

    /**
     * Check if an HCF is used in any route.
     */
    @Transactional(readOnly = true)
    public boolean isHcfInAnyRoute(UUID hcfId) {
        return waypointRepository.existsByHcfId(hcfId);
    }
}
