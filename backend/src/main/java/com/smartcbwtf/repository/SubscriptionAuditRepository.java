package com.smartcbwtf.repository;

import com.smartcbwtf.domain.SubscriptionAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionAuditRepository extends JpaRepository<SubscriptionAudit, UUID> {

    /**
     * Find all audit records for a specific entity
     */
    Page<SubscriptionAudit> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, UUID entityId, Pageable pageable);

    /**
     * Find audit records for a facility (convenience method)
     */
    default Page<SubscriptionAudit> findByFacilityId(UUID facilityId, Pageable pageable) {
        return findByEntityTypeAndEntityIdOrderByCreatedAtDesc("FACILITY", facilityId, pageable);
    }

    /**
     * Find recent audit records for a facility
     */
    List<SubscriptionAudit> findTop10ByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, UUID entityId);

    /**
     * Find all audit records by action type
     */
    List<SubscriptionAudit> findByActionAndCreatedAtAfterOrderByCreatedAtDesc(
            String action, Instant after);

    /**
     * Find audits performed by a specific user
     */
    Page<SubscriptionAudit> findByPerformedByOrderByCreatedAtDesc(
            UUID performedBy, Pageable pageable);
}
