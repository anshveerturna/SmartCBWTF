package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Facility;
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

@Repository
public interface FacilityRepository extends JpaRepository<Facility, UUID> {

    Optional<Facility> findByCode(String code);

    boolean existsByCode(String code);

    // Subscription queries
    List<Facility> findBySubscriptionStatusAndSubscriptionExpiresAtBefore(
            String status, Instant expiresAt);

    List<Facility> findBySubscriptionStatus(String status);

    // Admin listing with filters
    Page<Facility> findBySubscriptionStatus(String status, Pageable pageable);

    Page<Facility> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
            String name, String code, Pageable pageable);

    // Count by status for platform stats
    long countBySubscriptionStatus(String status);

    @Query("SELECT COUNT(f) FROM Facility f WHERE f.subscriptionStatus IN :statuses")
    long countBySubscriptionStatusIn(@Param("statuses") List<String> statuses);
}
