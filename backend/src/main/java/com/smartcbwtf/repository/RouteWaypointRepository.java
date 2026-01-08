package com.smartcbwtf.repository;

import com.smartcbwtf.domain.RouteWaypoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RouteWaypointRepository extends JpaRepository<RouteWaypoint, UUID> {

    List<RouteWaypoint> findByRouteIdOrderBySequenceOrderAsc(UUID routeId);

    List<RouteWaypoint> findByRouteIdAndIsActiveTrueOrderBySequenceOrderAsc(UUID routeId);

    @Query("SELECT rw FROM RouteWaypoint rw " +
            "JOIN FETCH rw.hcf " +
            "WHERE rw.route.id = :routeId " +
            "ORDER BY rw.sequenceOrder ASC")
    List<RouteWaypoint> findByRouteIdWithHcf(@Param("routeId") UUID routeId);

    @Modifying
    @Query("DELETE FROM RouteWaypoint rw WHERE rw.route.id = :routeId")
    void deleteAllByRouteId(@Param("routeId") UUID routeId);

    boolean existsByHcfId(UUID hcfId);

    long countByRouteId(UUID routeId);

    @Query("SELECT MAX(rw.sequenceOrder) FROM RouteWaypoint rw WHERE rw.route.id = :routeId")
    Integer findMaxSequenceOrderByRouteId(@Param("routeId") UUID routeId);
}
