package com.smartcbwtf.service;

import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Route;
import com.smartcbwtf.domain.enums.RouteStatus;
import com.smartcbwtf.dto.route.*;
import com.smartcbwtf.repository.RouteAssignmentRepository;
import com.smartcbwtf.repository.RouteRepository;
import com.smartcbwtf.repository.RouteAlertRepository;
import com.smartcbwtf.repository.FacilityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.smartcbwtf.util.PaginationUtils.pageRequest;

/**
 * Service for managing collection routes.
 * Routes are first-class entities, independent of staff lifecycle.
 */
@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository routeRepository;
    private final RouteAssignmentRepository assignmentRepository;
    private final FacilityRepository facilityRepository;
    private final RouteExecutionService routeExecutionService;
    private final RouteAlertRepository routeAlertRepository;

    public RouteService(
            RouteRepository routeRepository,
            RouteAssignmentRepository assignmentRepository,
            FacilityRepository facilityRepository,
            RouteExecutionService routeExecutionService,
            RouteAlertRepository routeAlertRepository) {
        this.routeRepository = routeRepository;
        this.assignmentRepository = assignmentRepository;
        this.facilityRepository = facilityRepository;
        this.routeExecutionService = routeExecutionService;
        this.routeAlertRepository = routeAlertRepository;
    }

    /**
     * Create a new route for a facility.
     */
    @Transactional
    public RouteDTO createRoute(UUID facilityId, CreateRouteRequest request) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Facility not found: " + facilityId));

        if (routeRepository.existsByFacilityIdAndName(facilityId, request.name())) {
            throw new IllegalArgumentException("A route with this name already exists");
        }

        Route route = new Route();
        route.setFacility(facility);
        route.setName(request.name());
        route.setDescription(request.description());
        if (request.color() != null && !request.color().isBlank()) {
            route.setColor(request.color());
        }
        if (request.completionDays() != null && request.completionDays() > 0) {
            route.setCompletionDays(request.completionDays());
        }
        route.setStatus(RouteStatus.DRAFT);

        route = routeRepository.save(route);
        log.info("Created route '{}' for facility {}", route.getName(), facilityId);

        return RouteDTO.from(route);
    }

    /**
     * Update an existing route.
     */
    @Transactional
    public RouteDTO updateRoute(UUID routeId, UUID facilityId, UpdateRouteRequest request) {
        Route route = routeRepository.findByIdAndFacilityId(routeId, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found: " + routeId));

        if (request.name() != null && !request.name().isBlank()) {
            // Check for duplicate name if changing
            if (!route.getName().equals(request.name()) &&
                    routeRepository.existsByFacilityIdAndName(facilityId, request.name())) {
                throw new IllegalArgumentException("A route with this name already exists");
            }
            route.setName(request.name());
        }
        if (request.description() != null) {
            route.setDescription(request.description());
        }
        if (request.color() != null && !request.color().isBlank()) {
            route.setColor(request.color());
        }
        if (request.status() != null) {
            route.setStatus(request.status());
        }
        if (request.completionDays() != null && request.completionDays() > 0) {
            route.setCompletionDays(request.completionDays());
        }

        route = routeRepository.save(route);
        log.info("Updated route '{}' (id: {})", route.getName(), routeId);

        return RouteDTO.from(route);
    }

    /**
     * Set route status.
     */
    @Transactional
    public RouteDTO setStatus(UUID routeId, UUID facilityId, RouteStatus status) {
        Route route = routeRepository.findByIdAndFacilityId(routeId, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found: " + routeId));

        RouteStatus oldStatus = route.getStatus();
        route.setStatus(status);
        route = routeRepository.save(route);

        log.info("Route '{}' status changed: {} -> {}", route.getName(), oldStatus, status);

        return RouteDTO.from(route);
    }

    /**
     * Get route details with waypoints and assignment history.
     */
    @Transactional(readOnly = true)
    public RouteDetailDTO getRouteDetails(UUID routeId, UUID facilityId) {
        Route route = routeRepository.findByIdWithDetails(routeId, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found: " + routeId));

        List<RouteAssignmentDTO> history = assignmentRepository
                .findAssignmentHistoryWithDetails(routeId)
                .stream()
                .map(RouteAssignmentDTO::from)
                .toList();

        return RouteDetailDTO.from(route, history);
    }

    /**
     * List all routes for a facility.
     */
    @Transactional(readOnly = true)
    public List<RouteDTO> listRoutes(UUID facilityId) {
        return routeRepository.findByFacilityIdOrderByNameAsc(facilityId)
                .stream()
                .map(RouteDTO::from)
                .toList();
    }

    /**
     * List active routes for a facility.
     */
    @Transactional(readOnly = true)
    public List<RouteDTO> listActiveRoutes(UUID facilityId) {
        return routeRepository.findByFacilityIdAndIsActiveTrueOrderByNameAsc(facilityId)
                .stream()
                .map(RouteDTO::from)
                .toList();
    }

    /**
     * Get route entity (for internal use).
     */
    @Transactional(readOnly = true)
    public Route getRoute(UUID routeId, UUID facilityId) {
        return routeRepository.findByIdAndFacilityId(routeId, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found: " + routeId));
    }

    // =============================================
    // Route Execution & Compliance
    // =============================================

    /**
     * Get current route execution state including active cycle and waypoint
     * statuses.
     */
    @Transactional(readOnly = true)
    public RouteExecutionDTO getRouteExecution(UUID routeId, UUID facilityId) {
        Route route = routeRepository.findByIdAndFacilityId(routeId, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found: " + routeId));

        var activeCycleOpt = routeExecutionService.getActiveCycle(routeId);

        RouteExecutionDTO.RouteCycleDTO cycleDTO = null;
        java.util.List<RouteExecutionDTO.ExecutionLogDTO> logDTOs = new java.util.ArrayList<>();

        if (activeCycleOpt.isPresent()) {
            var cycle = activeCycleOpt.get();
            var staff = cycle.getStaff();

            cycleDTO = new RouteExecutionDTO.RouteCycleDTO(
                    cycle.getId().toString(),
                    cycle.getCycleNumber(),
                    cycle.getCycleStart(),
                    cycle.getCycleEnd(),
                    cycle.getTotalWaypoints(),
                    cycle.getCompletedWaypoints(),
                    cycle.getMissedWaypoints(),
                    cycle.getCompletionPercentage(),
                    cycle.getStatus().name(),
                    staff != null ? staff.getName() : null,
                    cycle.getCompletedAt());

            var logs = routeExecutionService.getExecutionLogs(cycle.getId());
            logDTOs = logs.stream().map(execLog -> {
                var hcf = execLog.getHcf();
                var logStaff = execLog.getStaff();
                return new RouteExecutionDTO.ExecutionLogDTO(
                        execLog.getId().toString(),
                        execLog.getWaypoint().getId().toString(),
                        hcf.getId().toString(),
                        hcf.getName(),
                        hcf.getCode(),
                        execLog.getSequenceOrder(),
                        execLog.getStatus().name(),
                        execLog.getVisitedAt(),
                        logStaff != null ? logStaff.getName() : null);
            }).toList();
        }

        return new RouteExecutionDTO(
                route.getId().toString(),
                route.getName(),
                route.getCompletionDays(),
                cycleDTO,
                logDTOs);
    }

    /**
     * Get cycle history for a route.
     */
    @Transactional(readOnly = true)
    public List<RouteCycleHistoryDTO> getCycleHistory(UUID routeId, UUID facilityId, int page, int size) {
        // Verify route belongs to facility
        routeRepository.findByIdAndFacilityId(routeId, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Route not found: " + routeId));

        var pageable = pageRequest(page, size, 10);
        var cycles = routeExecutionService.getCycleHistory(routeId, pageable);

        return cycles.stream().map(cycle -> {
            var staff = cycle.getStaff();
            return new RouteCycleHistoryDTO(
                    cycle.getId().toString(),
                    cycle.getRoute().getId().toString(),
                    cycle.getRoute().getName(),
                    cycle.getCycleNumber(),
                    cycle.getCycleStart(),
                    cycle.getCycleEnd(),
                    cycle.getTotalWaypoints(),
                    cycle.getCompletedWaypoints(),
                    cycle.getMissedWaypoints(),
                    cycle.getCompletionPercentage(),
                    cycle.getStatus().name(),
                    staff != null ? staff.getId().toString() : null,
                    staff != null ? staff.getName() : null,
                    cycle.getCompletedAt(),
                    cycle.getCreatedAt());
        }).toList();
    }

    /**
     * Get unresolved alerts for a facility.
     */
    @Transactional(readOnly = true)
    public List<RouteAlertDTO> getUnresolvedAlerts(UUID facilityId, int limit) {
        var alerts = routeExecutionService.getUnresolvedAlerts(facilityId, pageRequest(0, limit, 100));
        return alerts.stream().map(this::toAlertDTO).toList();
    }

    /**
     * Get count of unresolved alerts for a facility.
     */
    @Transactional(readOnly = true)
    public long getUnresolvedAlertCount(UUID facilityId) {
        return routeAlertRepository.countUnresolvedByFacilityId(facilityId);
    }

    /**
     * Resolve an alert.
     */
    @Transactional
    public RouteAlertDTO resolveAlert(UUID alertId, UUID facilityId, String notes) {
        var alert = routeAlertRepository.findByIdAndFacilityId(alertId, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found: " + alertId));

        // Get current user - for now just mark as resolved without specific user
        alert.setIsResolved(true);
        alert.setResolvedAt(java.time.Instant.now());
        alert.setResolutionNotes(notes);
        alert = routeAlertRepository.save(alert);

        log.info("Resolved alert {} for route {}", alertId, alert.getRoute().getName());
        return toAlertDTO(alert);
    }

    private RouteAlertDTO toAlertDTO(com.smartcbwtf.domain.RouteAlert alert) {
        var staff = alert.getStaff();
        var resolvedBy = alert.getResolvedBy();
        var cycle = alert.getCycle();
        var route = alert.getRoute();

        return new RouteAlertDTO(
                alert.getId().toString(),
                route.getId().toString(),
                route.getName(),
                route.getColor(),
                cycle.getId().toString(),
                cycle.getCycleNumber(),
                cycle.getCycleStart(),
                cycle.getCycleEnd(),
                alert.getAlertType().name(),
                alert.getSeverity().name(),
                alert.getTitle(),
                alert.getMessage(),
                alert.getMissedHcfCount(),
                staff != null ? staff.getId().toString() : null,
                staff != null ? staff.getName() : null,
                alert.getIsResolved(),
                resolvedBy != null ? resolvedBy.getName() : null,
                alert.getResolvedAt(),
                alert.getResolutionNotes(),
                alert.getCreatedAt());
    }
}
