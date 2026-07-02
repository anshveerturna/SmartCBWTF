package com.smartcbwtf.repository;

import com.smartcbwtf.domain.RouteCycleHistory;
import com.smartcbwtf.domain.RouteCycleHistory.CycleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteCycleHistoryRepository extends JpaRepository<RouteCycleHistory, UUID> {

    List<RouteCycleHistory> findByRouteId(UUID routeId);

    Page<RouteCycleHistory> findByRouteIdOrderByCycleStartDesc(UUID routeId, Pageable pageable);

    Page<RouteCycleHistory> findByFacilityIdOrderByCycleStartDesc(UUID facilityId, Pageable pageable);

    Optional<RouteCycleHistory> findByRouteIdAndStatus(UUID routeId, CycleStatus status);

    @Query("SELECT c FROM RouteCycleHistory c WHERE c.route.id = :routeId AND c.status = 'IN_PROGRESS'")
    Optional<RouteCycleHistory> findActiveCycleByRouteId(@Param("routeId") UUID routeId);

    @Query("SELECT c FROM RouteCycleHistory c WHERE c.status = 'IN_PROGRESS' AND c.cycleEnd < :today")
    List<RouteCycleHistory> findOverdueCycles(@Param("today") LocalDate today);

    @Query("SELECT c FROM RouteCycleHistory c WHERE c.facility.id = :facilityId ORDER BY c.cycleStart DESC")
    Page<RouteCycleHistory> findByFacilityId(@Param("facilityId") UUID facilityId, Pageable pageable);

    @Query("SELECT MAX(c.cycleNumber) FROM RouteCycleHistory c WHERE c.route.id = :routeId")
    Integer findMaxCycleNumberByRouteId(@Param("routeId") UUID routeId);

    @Query("SELECT c FROM RouteCycleHistory c WHERE c.route.id = :routeId AND c.cycleStart <= :date AND c.cycleEnd >= :date")
    Optional<RouteCycleHistory> findByRouteIdAndDate(@Param("routeId") UUID routeId, @Param("date") LocalDate date);

    @Query("""
            SELECT COALESCE(SUM(c.missedWaypoints), 0)
            FROM RouteCycleHistory c
            WHERE c.facility.id = :facilityId
              AND c.cycleStart <= :date
              AND c.cycleEnd >= :date
            """)
    long sumMissedWaypointsByFacilityAndDate(@Param("facilityId") UUID facilityId, @Param("date") LocalDate date);
}
