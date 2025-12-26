package com.smartcbwtf.repository;

import com.smartcbwtf.domain.UserGpsEvent;
import org.springframework.data.domain.Page;
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
 * Repository for staff GPS events.
 * READ and CREATE only - no updates or deletes (append-only table).
 */
@Repository
public interface UserGpsEventRepository extends JpaRepository<UserGpsEvent, UUID> {

    /**
     * Check if event with this client ID already exists (idempotency).
     */
    boolean existsByClientEventId(UUID clientEventId);

    /**
     * Find most recent GPS event for a staff user.
     */
    @Query("SELECT e FROM UserGpsEvent e WHERE e.staffUser.id = :staffId ORDER BY e.recordedAt DESC LIMIT 1")
    Optional<UserGpsEvent> findLatestByStaffUserId(@Param("staffId") UUID staffId);

    /**
     * Get GPS history for a staff user within date range.
     */
    @Query("SELECT e FROM UserGpsEvent e WHERE e.staffUser.id = :staffId " +
            "AND e.recordedAt BETWEEN :start AND :end ORDER BY e.recordedAt DESC")
    List<UserGpsEvent> findByStaffUserIdAndDateRange(
            @Param("staffId") UUID staffId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    /**
     * Get recent GPS events for all staff in a facility.
     */
    @Query("SELECT e FROM UserGpsEvent e WHERE e.facility.id = :facilityId " +
            "AND e.recordedAt > :since ORDER BY e.recordedAt DESC")
    List<UserGpsEvent> findRecentByFacilityId(
            @Param("facilityId") UUID facilityId,
            @Param("since") Instant since);

    /**
     * Count events received in last N minutes for health monitoring.
     */
    @Query("SELECT COUNT(e) FROM UserGpsEvent e WHERE e.facility.id = :facilityId " +
            "AND e.receivedAt > :since")
    long countRecentByFacilityId(
            @Param("facilityId") UUID facilityId,
            @Param("since") Instant since);

    /**
     * Paginated GPS history for a staff user.
     */
    Page<UserGpsEvent> findByStaffUserIdOrderByRecordedAtDesc(UUID staffId, Pageable pageable);

    /**
     * Paginated GPS history for a facility.
     */
    Page<UserGpsEvent> findByFacilityIdOrderByRecordedAtDesc(UUID facilityId, Pageable pageable);
}
