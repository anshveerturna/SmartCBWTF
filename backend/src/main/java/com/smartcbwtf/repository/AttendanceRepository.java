package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    /** Check if attendance with this client-generated ID already exists (idempotency). */
    boolean existsByClientEventId(UUID clientEventId);

    Optional<Attendance> findByClientEventId(UUID clientEventId);

    /**
     * Find the most recent attendance record for a driver at any HCF.
     * Used for server-side cooldown enforcement.
     */
    @Query("SELECT a FROM Attendance a WHERE a.driver.id = :driverId ORDER BY a.eventTs DESC LIMIT 1")
    Optional<Attendance> findLatestByDriverId(@Param("driverId") UUID driverId);

    /**
     * Check if driver has any attendance within the cooldown window.
     * Returns true if driver is still in cooldown period.
     */
    @Query("SELECT COUNT(a) > 0 FROM Attendance a WHERE a.driver.id = :driverId AND a.eventTs > :cooldownStart")
    boolean existsByDriverIdAndEventTsAfter(@Param("driverId") UUID driverId, @Param("cooldownStart") Instant cooldownStart);

    /** Get attendance history for a specific driver. */
    List<Attendance> findByDriverIdOrderByEventTsDesc(UUID driverId);

    /** Get attendance history for a specific HCF. */
    List<Attendance> findByHcfIdOrderByEventTsDesc(UUID hcfId);

    /** Get attendance records within a date range for reporting. */
    @Query("SELECT a FROM Attendance a WHERE a.hcf.id = :hcfId AND a.eventTs BETWEEN :start AND :end ORDER BY a.eventTs DESC")
    List<Attendance> findByHcfIdAndEventTsBetween(
        @Param("hcfId") UUID hcfId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );
}
