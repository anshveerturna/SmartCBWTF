package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.domain.enums.RouteStatus;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing route execution cycles, tracking waypoint completion,
 * and generating alerts for incomplete routes.
 */
@Service
public class RouteExecutionService {

    private static final Logger log = LoggerFactory.getLogger(RouteExecutionService.class);

    private final RouteRepository routeRepository;
    private final RouteCycleHistoryRepository cycleRepository;
    private final RouteExecutionLogRepository executionLogRepository;
    private final RouteAlertRepository alertRepository;

    public RouteExecutionService(
            RouteRepository routeRepository,
            RouteCycleHistoryRepository cycleRepository,
            RouteExecutionLogRepository executionLogRepository,
            RouteAlertRepository alertRepository) {
        this.routeRepository = routeRepository;
        this.cycleRepository = cycleRepository;
        this.executionLogRepository = executionLogRepository;
        this.alertRepository = alertRepository;
    }

    /**
     * Starts a new execution cycle for a route.
     * Called when route becomes ACTIVE or when previous cycle ends.
     */
    @Transactional
    public RouteCycleHistory startNewCycle(Route route) {
        if (route.getStatus() != RouteStatus.ACTIVE) {
            log.warn("Cannot start cycle for non-active route {}", route.getId());
            return null;
        }

        // Close any existing IN_PROGRESS cycle first
        Optional<RouteCycleHistory> existingCycle = cycleRepository.findActiveCycleByRouteId(route.getId());
        if (existingCycle.isPresent()) {
            log.info("Closing existing cycle {} before starting new one", existingCycle.get().getId());
            finalizeCycle(existingCycle.get());
        }

        // Calculate cycle dates
        LocalDate startDate = LocalDate.now();
        int completionDays = route.getCompletionDays() != null ? route.getCompletionDays() : 1;
        LocalDate endDate = startDate.plusDays(completionDays - 1);

        // Get next cycle number
        Integer maxCycle = cycleRepository.findMaxCycleNumberByRouteId(route.getId());
        int cycleNumber = (maxCycle != null ? maxCycle : 0) + 1;

        // Get current assignment
        RouteAssignment assignment = route.getCurrentAssignment();

        // Create cycle
        RouteCycleHistory cycle = new RouteCycleHistory();
        cycle.setRoute(route);
        cycle.setFacility(route.getFacility());
        cycle.setStaff(assignment != null ? assignment.getStaff() : null);
        cycle.setCycleNumber(cycleNumber);
        cycle.setCycleStart(startDate);
        cycle.setCycleEnd(endDate);
        cycle.setTotalWaypoints(route.getWaypoints().size());
        cycle.setStatus(RouteCycleHistory.CycleStatus.IN_PROGRESS);

        cycle = cycleRepository.save(cycle);

        // Create execution log entries for each waypoint
        for (RouteWaypoint waypoint : route.getWaypoints()) {
            RouteExecutionLog execLog = new RouteExecutionLog();
            execLog.setCycle(cycle);
            execLog.setRoute(route);
            execLog.setWaypoint(waypoint);
            execLog.setHcf(waypoint.getHcf());
            execLog.setSequenceOrder(waypoint.getSequenceOrder());
            execLog.setStatus(RouteExecutionLog.ExecutionStatus.PENDING);
            executionLogRepository.save(execLog);
        }

        // Update route's cycle start date
        route.setCycleStartDate(startDate);
        routeRepository.save(route);

        log.info("Started cycle {} for route {} ({} waypoints, ends {})",
                cycleNumber, route.getName(), route.getWaypoints().size(), endDate);

        return cycle;
    }

    /**
     * Called when attendance is marked at an HCF.
     * Updates execution logs for any routes the staff is assigned to.
     */
    @Transactional
    public void onAttendanceMarked(Attendance attendance) {
        UUID hcfId = attendance.getHcf().getId();
        UUID staffId = attendance.getDriver().getId();

        // Find all pending execution logs for this HCF where staff is assigned
        List<RouteExecutionLog> pendingLogs = executionLogRepository.findPendingLogsForHcfAndStaff(hcfId, staffId);

        for (RouteExecutionLog execLog : pendingLogs) {
            execLog.markCompleted(attendance);
            executionLogRepository.save(execLog);

            // Update cycle stats
            RouteCycleHistory cycle = execLog.getCycle();
            cycle.recalculateStats();

            // Check if cycle is now complete
            if (cycle.isComplete()) {
                cycle.setStatus(RouteCycleHistory.CycleStatus.COMPLETED);
                cycle.setCompletedAt(Instant.now());
            }
            cycleRepository.save(cycle);

            log.info("Marked waypoint {} as completed for route {} (attendance {})",
                    execLog.getHcf().getName(), execLog.getRoute().getName(), attendance.getId());
        }
    }

    /**
     * Finalizes an overdue cycle - marks pending waypoints as MISSED,
     * creates alerts, and starts new cycle.
     */
    @Transactional
    public void finalizeCycle(RouteCycleHistory cycle) {
        if (cycle.getStatus() != RouteCycleHistory.CycleStatus.IN_PROGRESS) {
            return;
        }

        // Mark all pending waypoints as MISSED
        List<RouteExecutionLog> pendingLogs = executionLogRepository.findPendingByCycleId(cycle.getId());
        for (RouteExecutionLog log : pendingLogs) {
            log.markMissed();
            executionLogRepository.save(log);
        }

        // Recalculate stats
        cycle.recalculateStats();

        // Determine final status
        if (cycle.getCompletedWaypoints().equals(cycle.getTotalWaypoints())) {
            cycle.setStatus(RouteCycleHistory.CycleStatus.COMPLETED);
        } else {
            cycle.setStatus(RouteCycleHistory.CycleStatus.INCOMPLETE);
        }
        cycle.setCompletedAt(Instant.now());
        cycleRepository.save(cycle);

        // Create alert if incomplete
        if (cycle.getStatus() == RouteCycleHistory.CycleStatus.INCOMPLETE && cycle.getMissedWaypoints() > 0) {
            createIncompleteRouteAlert(cycle);
        }

        log.info("Finalized cycle {} for route {}: {} completed, {} missed",
                cycle.getCycleNumber(), cycle.getRoute().getName(),
                cycle.getCompletedWaypoints(), cycle.getMissedWaypoints());
    }

    /**
     * Creates an alert for an incomplete route cycle.
     */
    private void createIncompleteRouteAlert(RouteCycleHistory cycle) {
        RouteAlert alert = new RouteAlert();
        alert.setRoute(cycle.getRoute());
        alert.setCycle(cycle);
        alert.setFacility(cycle.getFacility());
        alert.setStaff(cycle.getStaff());
        alert.setAlertType(RouteAlert.AlertType.ROUTE_INCOMPLETE);

        int missed = cycle.getMissedWaypoints();
        if (missed == cycle.getTotalWaypoints()) {
            alert.setSeverity(RouteAlert.Severity.CRITICAL);
            alert.setTitle("Route not started: " + cycle.getRoute().getName());
            alert.setAlertType(RouteAlert.AlertType.ROUTE_NOT_STARTED);
        } else if (missed > cycle.getTotalWaypoints() / 2) {
            alert.setSeverity(RouteAlert.Severity.CRITICAL);
            alert.setTitle("Route largely incomplete: " + cycle.getRoute().getName());
        } else {
            alert.setSeverity(RouteAlert.Severity.WARNING);
            alert.setTitle("Route partially incomplete: " + cycle.getRoute().getName());
        }

        alert.setMissedHcfCount(missed);
        alert.setMessage(String.format(
                "Cycle %d (%s to %s): %d of %d waypoints missed (%.1f%% completed)",
                cycle.getCycleNumber(),
                cycle.getCycleStart(),
                cycle.getCycleEnd(),
                missed,
                cycle.getTotalWaypoints(),
                cycle.getCompletionPercentage().doubleValue()));

        alertRepository.save(alert);
        log.info("Created alert for incomplete cycle {} of route {}",
                cycle.getCycleNumber(), cycle.getRoute().getName());
    }

    /**
     * Checks all active routes for overdue cycles and processes them.
     * Should be called by a scheduled job.
     */
    @Transactional
    public void processOverdueCycles() {
        LocalDate today = LocalDate.now();
        List<RouteCycleHistory> overdueCycles = cycleRepository.findOverdueCycles(today);

        log.info("Found {} overdue cycles to process", overdueCycles.size());

        for (RouteCycleHistory cycle : overdueCycles) {
            try {
                // Finalize the overdue cycle
                finalizeCycle(cycle);

                // Start new cycle if route is still active
                Route route = cycle.getRoute();
                if (route.getStatus() == RouteStatus.ACTIVE) {
                    startNewCycle(route);
                }
            } catch (Exception e) {
                log.error("Failed to process overdue cycle {}", cycle.getId(), e);
            }
        }
    }

    /**
     * Gets execution logs for a specific cycle.
     */
    public List<RouteExecutionLog> getExecutionLogs(UUID cycleId) {
        return executionLogRepository.findByCycleIdOrderBySequenceOrderAsc(cycleId);
    }

    /**
     * Gets the active cycle for a route.
     */
    public Optional<RouteCycleHistory> getActiveCycle(UUID routeId) {
        return cycleRepository.findActiveCycleByRouteId(routeId);
    }

    /**
     * Gets cycle history for a route.
     */
    public Page<RouteCycleHistory> getCycleHistory(UUID routeId, Pageable pageable) {
        return cycleRepository.findByRouteIdOrderByCycleStartDesc(routeId, pageable);
    }

    /**
     * Gets unresolved alerts for a facility.
     */
    public List<RouteAlert> getUnresolvedAlerts(UUID facilityId, Pageable pageable) {
        return alertRepository.findUnresolvedByFacilityIdPaged(facilityId, pageable).getContent();
    }

    /**
     * Resolves an alert.
     */
    @Transactional
    public RouteAlert resolveAlert(UUID alertId, AppUser resolvedBy, String notes) {
        RouteAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.resolve(resolvedBy, notes);
        return alertRepository.save(alert);
    }

    /**
     * Ensures active routes have an active cycle.
     * Called on route activation or app startup.
     */
    @Transactional
    public void ensureActiveCycle(Route route) {
        if (route.getStatus() != RouteStatus.ACTIVE) {
            return;
        }

        Optional<RouteCycleHistory> activeCycle = cycleRepository.findActiveCycleByRouteId(route.getId());
        if (activeCycle.isEmpty()) {
            startNewCycle(route);
        }
    }
}
