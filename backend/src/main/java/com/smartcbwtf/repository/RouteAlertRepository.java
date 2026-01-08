package com.smartcbwtf.repository;

import com.smartcbwtf.domain.RouteAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RouteAlertRepository extends JpaRepository<RouteAlert, UUID> {

    List<RouteAlert> findByRouteId(UUID routeId);

    Page<RouteAlert> findByFacilityIdOrderByCreatedAtDesc(UUID facilityId, Pageable pageable);

    @Query("SELECT a FROM RouteAlert a WHERE a.facility.id = :facilityId AND a.isResolved = false ORDER BY a.createdAt DESC")
    List<RouteAlert> findUnresolvedByFacilityId(@Param("facilityId") UUID facilityId);

    @Query("SELECT a FROM RouteAlert a WHERE a.facility.id = :facilityId AND a.isResolved = false")
    Page<RouteAlert> findUnresolvedByFacilityIdPaged(@Param("facilityId") UUID facilityId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM RouteAlert a WHERE a.facility.id = :facilityId AND a.isResolved = false")
    long countUnresolvedByFacilityId(@Param("facilityId") UUID facilityId);

    List<RouteAlert> findByCycleId(UUID cycleId);

    @Query("SELECT a FROM RouteAlert a WHERE a.route.id = :routeId AND a.isResolved = false")
    List<RouteAlert> findUnresolvedByRouteId(@Param("routeId") UUID routeId);
}
