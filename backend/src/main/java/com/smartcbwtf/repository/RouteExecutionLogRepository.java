package com.smartcbwtf.repository;

import com.smartcbwtf.domain.RouteExecutionLog;
import com.smartcbwtf.domain.RouteExecutionLog.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteExecutionLogRepository extends JpaRepository<RouteExecutionLog, UUID> {

    List<RouteExecutionLog> findByCycleIdOrderBySequenceOrderAsc(UUID cycleId);

    List<RouteExecutionLog> findByRouteIdAndStatus(UUID routeId, ExecutionStatus status);

    Optional<RouteExecutionLog> findByCycleIdAndHcfId(UUID cycleId, UUID hcfId);

    Optional<RouteExecutionLog> findByCycleIdAndWaypointId(UUID cycleId, UUID waypointId);

    @Query("SELECT l FROM RouteExecutionLog l WHERE l.cycle.id = :cycleId AND l.status = 'PENDING'")
    List<RouteExecutionLog> findPendingByCycleId(@Param("cycleId") UUID cycleId);

    @Query("SELECT l FROM RouteExecutionLog l WHERE l.attendance.id = :attendanceId")
    Optional<RouteExecutionLog> findByAttendanceId(@Param("attendanceId") UUID attendanceId);

    @Query("SELECT l FROM RouteExecutionLog l " +
            "WHERE l.hcf.id = :hcfId " +
            "AND l.cycle.status = 'IN_PROGRESS' " +
            "AND l.status = 'PENDING' " +
            "AND l.route.id IN (SELECT ra.route.id FROM RouteAssignment ra WHERE ra.staff.id = :staffId AND ra.isActive = true)")
    List<RouteExecutionLog> findPendingLogsForHcfAndStaff(@Param("hcfId") UUID hcfId, @Param("staffId") UUID staffId);
}
