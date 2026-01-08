package com.smartcbwtf.scheduler;

import com.smartcbwtf.service.RouteExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job for route compliance checking.
 * Runs daily to process overdue cycles and generate alerts.
 */
@Component
public class RouteComplianceScheduler {

    private static final Logger log = LoggerFactory.getLogger(RouteComplianceScheduler.class);

    private final RouteExecutionService routeExecutionService;

    public RouteComplianceScheduler(RouteExecutionService routeExecutionService) {
        this.routeExecutionService = routeExecutionService;
    }

    /**
     * Runs every day at 1:00 AM to check for overdue route cycles.
     * - Marks pending waypoints as MISSED
     * - Generates alerts for incomplete routes
     * - Starts new cycles for active routes
     */
    @Scheduled(cron = "0 0 1 * * *") // 1:00 AM daily
    public void checkOverdueRoutes() {
        log.info("Starting daily route compliance check");
        try {
            routeExecutionService.processOverdueCycles();
            log.info("Completed daily route compliance check");
        } catch (Exception e) {
            log.error("Error during route compliance check", e);
        }
    }

    /**
     * Runs every 6 hours to catch any cycles that may have been missed.
     * This is a backup in case the daily job fails.
     */
    @Scheduled(cron = "0 0 */6 * * *") // Every 6 hours
    public void periodicCycleCheck() {
        log.debug("Running periodic cycle check");
        try {
            routeExecutionService.processOverdueCycles();
        } catch (Exception e) {
            log.error("Error during periodic cycle check", e);
        }
    }
}
