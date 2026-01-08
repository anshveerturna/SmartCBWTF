package com.smartcbwtf.repository;

import com.smartcbwtf.domain.RouteAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteAssignmentRepository extends JpaRepository<RouteAssignment, UUID> {

    Optional<RouteAssignment> findByRouteIdAndIsActiveTrue(UUID routeId);

    List<RouteAssignment> findByRouteIdOrderByCreatedAtDesc(UUID routeId);

    @Query("SELECT ra FROM RouteAssignment ra " +
            "JOIN FETCH ra.staff " +
            "LEFT JOIN FETCH ra.vehicle " +
            "WHERE ra.route.id = :routeId AND ra.isActive = true")
    Optional<RouteAssignment> findActiveAssignmentWithDetails(@Param("routeId") UUID routeId);

    @Query("SELECT ra FROM RouteAssignment ra " +
            "JOIN FETCH ra.staff " +
            "LEFT JOIN FETCH ra.vehicle " +
            "WHERE ra.route.id = :routeId " +
            "ORDER BY ra.createdAt DESC")
    List<RouteAssignment> findAssignmentHistoryWithDetails(@Param("routeId") UUID routeId);

    List<RouteAssignment> findByStaffIdAndIsActiveTrue(UUID staffId);

    List<RouteAssignment> findByVehicleIdAndIsActiveTrue(UUID vehicleId);

    long countByRouteId(UUID routeId);

    boolean existsByRouteIdAndIsActiveTrue(UUID routeId);
}
