package com.smartcbwtf.repository;

import com.smartcbwtf.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByActorUserIdOrderByTsDesc(UUID actorUserId, Pageable pageable);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:entityType IS NULL OR a.entityType = :entityType)
              AND (:entityId IS NULL OR a.entityId = :entityId)
              AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId)
              AND (:fromTs IS NULL OR a.ts >= :fromTs)
              AND (:toTs IS NULL OR a.ts <= :toTs)
            ORDER BY a.ts DESC
            """)
    Page<AuditLog> search(
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("actorUserId") UUID actorUserId,
            @Param("fromTs") Instant fromTs,
            @Param("toTs") Instant toTs,
            Pageable pageable);
}
