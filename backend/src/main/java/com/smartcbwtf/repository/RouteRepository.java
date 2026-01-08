package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Route;
import com.smartcbwtf.domain.enums.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {

        List<Route> findByFacilityIdOrderByNameAsc(UUID facilityId);

        List<Route> findByFacilityIdAndIsActiveTrueOrderByNameAsc(UUID facilityId);

        List<Route> findByFacilityIdAndStatusOrderByNameAsc(UUID facilityId, RouteStatus status);

        Optional<Route> findByIdAndFacilityId(UUID id, UUID facilityId);

        @Query("SELECT r FROM Route r " +
                        "LEFT JOIN FETCH r.waypoints w " +
                        "LEFT JOIN FETCH w.hcf " +
                        "WHERE r.id = :id AND r.facility.id = :facilityId")
        Optional<Route> findByIdWithWaypoints(@Param("id") UUID id, @Param("facilityId") UUID facilityId);

        @Query("SELECT DISTINCT r FROM Route r " +
                        "LEFT JOIN FETCH r.waypoints w " +
                        "LEFT JOIN FETCH w.hcf " +
                        "WHERE r.id = :id AND r.facility.id = :facilityId")
        Optional<Route> findByIdWithDetails(@Param("id") UUID id, @Param("facilityId") UUID facilityId);

        @Query("SELECT DISTINCT r FROM Route r " +
                        "LEFT JOIN FETCH r.waypoints w " +
                        "LEFT JOIN FETCH w.hcf " +
                        "WHERE r.facility.id = :facilityId AND r.isActive = true " +
                        "ORDER BY r.name")
        List<Route> findActiveRoutesWithWaypoints(@Param("facilityId") UUID facilityId);

        @Query("SELECT DISTINCT r FROM Route r " +
                        "LEFT JOIN FETCH r.waypoints w " +
                        "LEFT JOIN FETCH w.hcf " +
                        "WHERE r.facility.id = :facilityId " +
                        "ORDER BY r.name")
        List<Route> findAllRoutesWithWaypoints(@Param("facilityId") UUID facilityId);

        boolean existsByFacilityIdAndName(UUID facilityId, String name);

        long countByFacilityId(UUID facilityId);

        long countByFacilityIdAndIsActiveTrue(UUID facilityId);
}
