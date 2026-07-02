package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.dto.mobile.MobileRouteDTO;
import com.smartcbwtf.dto.mobile.MobileWaypointDTO;
import com.smartcbwtf.repository.AttendanceRepository;
import com.smartcbwtf.repository.RouteAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Controller for mobile-specific route operations.
 * Provides staff with their assigned route details.
 */
@RestController
@RequestMapping("/api/mobile")
public class MobileRouteController {

    private static final Logger log = LoggerFactory.getLogger(MobileRouteController.class);

    private final RouteAssignmentRepository assignmentRepository;
    private final AttendanceRepository attendanceRepository;

    public MobileRouteController(RouteAssignmentRepository assignmentRepository,
            AttendanceRepository attendanceRepository) {
        this.assignmentRepository = assignmentRepository;
        this.attendanceRepository = attendanceRepository;
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
        UUID tenantId = TenantContext.getTenantId();

        if (staffId == null || tenantId == null) {
            log.warn("Missing tenant context for my-route request");
            return ResponseEntity.notFound().build();
        }

        var assignmentOpt = assignmentRepository.findActiveAssignmentByStaffIdWithRouteDetails(staffId);

        if (assignmentOpt.isEmpty()) {
            log.info("No active route assignment found for staff {}", staffId);
            return ResponseEntity.notFound().build();
        }

        var assignment = assignmentOpt.get();
        var route = assignment.getRoute();
        if (route == null || route.getFacility() == null || !tenantId.equals(route.getFacility().getId())) {
            log.warn("Route assignment for staff {} is outside tenant {}", staffId, tenantId);
            return ResponseEntity.notFound().build();
        }

        var facility = route.getFacility();

        // Determine the current cycle's time range
        LocalDate cycleStart = route.getCycleStartDate() != null ? route.getCycleStartDate() : LocalDate.now();
        int completionDays = route.getCompletionDays() != null ? route.getCompletionDays() : 1;
        LocalDate cycleEnd = cycleStart.plusDays(completionDays);

        ZoneId zone = ZoneId.systemDefault();
        Instant cycleStartInstant = cycleStart.atStartOfDay(zone).toInstant();
        Instant cycleEndInstant = cycleEnd.atStartOfDay(zone).toInstant();

        // Collect all HCF IDs from waypoints
        var activeWaypoints = route.getWaypoints().stream()
                .filter(w -> Boolean.TRUE.equals(w.getIsActive()))
                .sorted(Comparator.comparing(w -> w.getSequenceOrder()))
                .toList();

        // Get set of HCF IDs that have attendance marked within this cycle
        Set<UUID> attendedHcfIds = new HashSet<>();
        for (var waypoint : activeWaypoints) {
            UUID hcfId = waypoint.getHcf().getId();
            var attendances = attendanceRepository.findByHcfIdAndEventTsBetween(hcfId, cycleStartInstant,
                    cycleEndInstant);
            // Check if any attendance was by this staff member
            boolean attended = attendances.stream()
                    .anyMatch(a -> a.getDriver() != null && staffId.equals(a.getDriver().getId()));
            if (attended) {
                attendedHcfIds.add(hcfId);
            }
        }

        // Build waypoints DTO list with attendance status
        var waypoints = activeWaypoints.stream()
                .map(w -> MobileWaypointDTO.from(w, attendedHcfIds.contains(w.getHcf().getId())))
                .toList();

        var dto = new MobileRouteDTO(
                route.getId().toString(),
                route.getName(),
                route.getColor(),
                route.getCompletionDays(),
                facility.getName(),
                waypoints);

        log.info("Returning route '{}' with {} waypoints ({} attended) for staff {}",
                route.getName(), waypoints.size(), attendedHcfIds.size(), staffId);

        return ResponseEntity.ok(dto);
    }
}
