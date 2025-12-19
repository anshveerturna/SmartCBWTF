package com.smartcbwtf.repository;

import com.smartcbwtf.domain.UserLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, UUID> {

    // Get latest location for a user
    Optional<UserLocation> findFirstByUserIdOrderByRecordedAtDesc(UUID userId);

    // Get location history for a user
    Page<UserLocation> findByUserIdOrderByRecordedAtDesc(UUID userId, Pageable pageable);

    // Get locations within a time range
    List<UserLocation> findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            UUID userId, Instant start, Instant end);

    // Count locations for a user today
    long countByUserIdAndRecordedAtAfter(UUID userId, Instant after);
}
