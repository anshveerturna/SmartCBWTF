package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.dto.mobile.MobileRouteDTO;
import com.smartcbwtf.dto.mobile.MobileWaypointDTO;
import com.smartcbwtf.repository.RouteAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.UUID;

/**
 * Controller for mobile-specific route operations.
 * Provides staff with their assigned route details.
 */
@RestController
@RequestMapping("/api/mobile")
public class MobileRouteController {

    private static final Logger log = LoggerFactory.getLogger(MobileRouteController.class);

    private final RouteAssignmentRepository assignmentRepository;

    public MobileRouteController(RouteAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * Get the currently authenticated staff member's assigned route.
     * Returns 404 if no route is assigned.
     * 
     * Security: Only DRIVER and PLANT_OPERATOR roles can access.
     * The response is filtered to only show the route assigned to THIS staff.
     */
    @GetMapping("/my-route")
    @PreAuthorize("hasAnyRole('DRIVER', 'PLANT_OPERATOR')")
    public ResponseEntity<MobileRouteDTO> getMyRoute() {
        UUID staffId = TenantContext.getUserId();

        if (staffId == null) {
            log.warn("No user ID in context for my-route request");
            return ResponseEntity.notFound().build();
        }

        var assignmentOpt = assignmentRepository.findActiveAssignmentByStaffIdWithRouteDetails(staffId);

        if (assignmentOpt.isEmpty()) {
            log.info("No active route assignment found for staff {}", staffId);
            return ResponseEntity.notFound().build();
        }

        var assignment = assignmentOpt.get();
        var route = assignment.getRoute();
        var facility = route.getFacility();

        // Build waypoints DTO list, sorted by sequence order
        var waypoints = route.getWaypoints().stream()
                .filter(w -> Boolean.TRUE.equals(w.getIsActive()))
                .sorted(Comparator.comparing(w -> w.getSequenceOrder()))
                .map(MobileWaypointDTO::from)
                .toList();

        var dto = new MobileRouteDTO(
                route.getId().toString(),
                route.getName(),
                route.getColor(),
                route.getCompletionDays(),
                facility.getName(),
                waypoints);

        log.info("Returning route '{}' with {} waypoints for staff {}",
                route.getName(), waypoints.size(), staffId);

        return ResponseEntity.ok(dto);
    }
}
