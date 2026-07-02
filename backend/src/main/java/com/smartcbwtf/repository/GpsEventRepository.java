package com.smartcbwtf.repository;

import com.smartcbwtf.domain.GpsEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for GPS Events.
 * IMPORTANT: This repository should ONLY be used for INSERT and SELECT
 * operations.
 * NO UPDATE or DELETE operations allowed - GPS events are append-only.
 */
@Repository
public interface GpsEventRepository extends JpaRepository<GpsEvent, UUID> {

    // Get last GPS event for a tenant-scoped vehicle
    @Query("SELECT g FROM GpsEvent g WHERE g.vehicle.facility.id = :facilityId " +
            "AND g.vehicle.id = :vehicleId " +
            "ORDER BY g.recordedAt DESC LIMIT 1")
    Optional<GpsEvent> findLatestByFacilityIdAndVehicleId(
            @Param("facilityId") UUID facilityId,
            @Param("vehicleId") UUID vehicleId);

    // Get recent events for a tenant-scoped vehicle (for trail/history)
    @Query("SELECT g FROM GpsEvent g WHERE g.vehicle.facility.id = :facilityId " +
            "AND g.vehicle.id = :vehicleId " +
            "ORDER BY g.recordedAt DESC")
    List<GpsEvent> findRecentByFacilityIdAndVehicleId(
            @Param("facilityId") UUID facilityId,
            @Param("vehicleId") UUID vehicleId,
            Pageable pageable);

    // Get events in a time range
    @Query("SELECT g FROM GpsEvent g WHERE g.vehicle.id = :vehicleId " +
            "AND g.recordedAt BETWEEN :start AND :end ORDER BY g.recordedAt ASC")
    List<GpsEvent> findByVehicleIdAndTimeRange(@Param("vehicleId") UUID vehicleId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    // Count events for a vehicle today
    @Query("SELECT COUNT(g) FROM GpsEvent g WHERE g.vehicle.id = :vehicleId " +
            "AND g.receivedAt >= :startOfDay")
    long countEventsToday(@Param("vehicleId") UUID vehicleId,
            @Param("startOfDay") Instant startOfDay);

    // Get last location for all vehicles of a facility (for live map)
    @Query("SELECT g FROM GpsEvent g WHERE g.vehicle.facility.id = :facilityId " +
            "AND g.id IN (SELECT MAX(g2.id) FROM GpsEvent g2 " +
            "WHERE g2.vehicle.facility.id = :facilityId GROUP BY g2.vehicle.id)")
    List<GpsEvent> findLastLocationsByFacility(@Param("facilityId") UUID facilityId);
}
