package com.smartcbwtf.repository;

import com.smartcbwtf.domain.SubscriptionAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

        /**
         * Find all audit records by action type (paginated)
         */
        Page<SubscriptionAudit> findByAction(String action, Pageable pageable);

        /**
         * Find recent error/warning actions for system monitoring
         * Returns the 20 most recent audit entries to show as "recent activity"
         */
        default List<SubscriptionAudit> findRecentErrorActions() {
                // Return recent audit entries as "activity" since we don't have a dedicated
                // error table
                // In a production system, this would query a dedicated error_log table
                return findTop20ByOrderByCreatedAtDesc();
        }

        /**
         * Find most recent 20 audit entries
         */
        List<SubscriptionAudit> findTop20ByOrderByCreatedAtDesc();

        /**
         * Find audit records by entity type
         */
        Page<SubscriptionAudit> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

        /**
         * Find audit records by performer role
         */
        Page<SubscriptionAudit> findByPerformedByRoleOrderByCreatedAtDesc(String role, Pageable pageable);

        @Query("""
                        SELECT a FROM SubscriptionAudit a
                        WHERE (:entityType IS NULL OR a.entityType = :entityType)
                          AND (:action IS NULL OR a.action = :action)
                          AND (:actorId IS NULL OR a.performedBy = :actorId)
                          AND (:fromTs IS NULL OR a.createdAt >= :fromTs)
                          AND (:toTs IS NULL OR a.createdAt <= :toTs)
                        ORDER BY a.createdAt DESC
                        """)
        Page<SubscriptionAudit> searchAuditLogs(
                        @Param("entityType") String entityType,
                        @Param("action") String action,
                        @Param("actorId") UUID actorId,
                        @Param("fromTs") Instant from,
                        @Param("toTs") Instant to,
                        Pageable pageable);
}
