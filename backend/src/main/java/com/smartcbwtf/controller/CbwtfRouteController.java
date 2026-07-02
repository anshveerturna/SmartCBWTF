package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.enums.RouteStatus;
import com.smartcbwtf.dto.route.*;
import com.smartcbwtf.service.RouteAssignmentService;
import com.smartcbwtf.service.RouteMapService;
import com.smartcbwtf.service.RouteService;
import com.smartcbwtf.service.RouteWaypointService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for Route Planning operations.
 * All endpoints are scoped to CBWTF_ADMIN role.
 */
@RestController
@RequestMapping("/api/cbwtf/routes")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfRouteController {

    private static final Logger log = LoggerFactory.getLogger(CbwtfRouteController.class);
    private static final int MAX_STATUS_LENGTH = 40;
    private static final int MAX_ALERT_RESOLUTION_NOTES_LENGTH = 1000;
    private static final int DEFAULT_ALERT_LIMIT = 100;

    private final RouteService routeService;
    private final RouteWaypointService waypointService;
    private final RouteAssignmentService assignmentService;
    private final RouteMapService mapService;

    public CbwtfRouteController(
            RouteService routeService,
            RouteWaypointService waypointService,
            RouteAssignmentService assignmentService,
            RouteMapService mapService) {
        this.routeService = routeService;
        this.waypointService = waypointService;
        this.assignmentService = assignmentService;
        this.mapService = mapService;
    }

    private UUID getFacilityId() {
        return TenantContext.getTenantId();
    }

    // =============================================
    // Route CRUD
    // =============================================

    @PostMapping
    public ResponseEntity<RouteDTO> createRoute(@Valid @RequestBody CreateRouteRequest request) {
        RouteDTO route = routeService.createRoute(getFacilityId(), request);
        log.info("Created route: {}", route.name());
        return ResponseEntity.ok(route);
    }

    @GetMapping
    public ResponseEntity<List<RouteDTO>> listRoutes() {
        List<RouteDTO> routes = routeService.listRoutes(getFacilityId());
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteDetailDTO> getRoute(@PathVariable UUID id) {
        RouteDetailDTO route = routeService.getRouteDetails(id, getFacilityId());
        return ResponseEntity.ok(route);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RouteDTO> updateRoute(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRouteRequest request) {
        RouteDTO route = routeService.updateRoute(id, getFacilityId(), request);
        log.info("Updated route: {}", route.name());
        return ResponseEntity.ok(route);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RouteDTO> setStatus(
            @PathVariable UUID id,
            @Valid @RequestBody SetRouteStatusRequest body) {
        String statusStr = body != null ? body.status() : null;
        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        RouteStatus status;
        try {
            status = RouteStatus.valueOf(statusStr.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        RouteDTO route = routeService.setStatus(id, getFacilityId(), status);
        log.info("Route {} status changed to {}", route.name(), status);
        return ResponseEntity.ok(route);
    }

    // =============================================
    // Waypoints
    // =============================================

    @PutMapping("/{id}/waypoints")
    public ResponseEntity<List<RouteWaypointDTO>> setWaypoints(
            @PathVariable UUID id,
            @Valid @RequestBody SetWaypointsRequest request) {
        List<RouteWaypointDTO> waypoints = waypointService.setWaypoints(id, getFacilityId(), request);
        log.info("Set {} waypoints for route {}", waypoints.size(), id);
        return ResponseEntity.ok(waypoints);
    }

    // =============================================
    // Assignments
    // =============================================

    @PostMapping("/{id}/assign")
    public ResponseEntity<RouteAssignmentDTO> assignRoute(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRouteRequest request) {
        RouteAssignmentDTO assignment = assignmentService.assignRoute(id, getFacilityId(), request);
        log.info("Assigned route {} to staff {}", id, request.staffId());
        return ResponseEntity.ok(assignment);
    }

    @DeleteMapping("/{id}/assign")
    public ResponseEntity<Void> unassignRoute(@PathVariable UUID id) {
        assignmentService.unassignRoute(id, getFacilityId());
        log.info("Unassigned route {}", id);
        return ResponseEntity.noContent().build();
    }

    // =============================================
    // Map Data
    // =============================================

    @GetMapping("/map-data")
    public ResponseEntity<RouteMapDataDTO> getMapData(
            @RequestParam(name = "routeId", required = false) UUID routeId,
            @RequestParam(name = "activeOnly", required = false, defaultValue = "false") Boolean activeOnly) {
        RouteMapDataDTO data = mapService.getMapData(getFacilityId(), routeId, activeOnly);
        return ResponseEntity.ok(data);
    }

    // =============================================
    // Route Execution & Compliance
    // =============================================

    @GetMapping("/{id}/execution")
    public ResponseEntity<RouteExecutionDTO> getRouteExecution(@PathVariable UUID id) {
        RouteExecutionDTO execution = routeService.getRouteExecution(id, getFacilityId());
        return privateResponse(execution);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<RouteCycleHistoryDTO>> getCycleHistory(
            @PathVariable UUID id,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size) {
        List<RouteCycleHistoryDTO> history = routeService.getCycleHistory(id, getFacilityId(), page, size);
        return privateResponse(history);
    }

    private static <T> ResponseEntity<T> privateResponse(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<RouteAlertDTO>> getAlerts(
            @RequestParam(name = "limit", required = false, defaultValue = "" + DEFAULT_ALERT_LIMIT) int limit) {
        List<RouteAlertDTO> alerts = routeService.getUnresolvedAlerts(getFacilityId(), limit);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/alerts/count")
    public ResponseEntity<Map<String, Long>> getAlertCount() {
        long count = routeService.getUnresolvedAlertCount(getFacilityId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<RouteAlertDTO> resolveAlert(
            @PathVariable UUID alertId,
            @Valid @RequestBody(required = false) ResolveAlertRequest body) {
        String notes = normalizeOptionalNotes(body != null ? body.notes() : null);
        RouteAlertDTO alert = routeService.resolveAlert(alertId, getFacilityId(), notes);
        log.info("Resolved alert {}", alertId);
        return ResponseEntity.ok(alert);
    }

    public record SetRouteStatusRequest(
            @NotBlank(message = "Route status is required")
            @Size(max = MAX_STATUS_LENGTH, message = "Route status is invalid")
            String status) {
    }

    public record ResolveAlertRequest(
            @Size(max = MAX_ALERT_RESOLUTION_NOTES_LENGTH, message = "Resolution notes must be 1000 characters or fewer")
            String notes) {
    }

    private static String normalizeOptionalNotes(String notes) {
        if (notes == null) {
            return null;
        }
        String normalized = notes.strip();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > MAX_ALERT_RESOLUTION_NOTES_LENGTH) {
            throw new IllegalArgumentException(
                    "Resolution notes must be " + MAX_ALERT_RESOLUTION_NOTES_LENGTH + " characters or fewer");
        }
        return normalized;
    }
}
