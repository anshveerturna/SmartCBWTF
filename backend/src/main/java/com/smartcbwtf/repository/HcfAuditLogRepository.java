package com.smartcbwtf.repository;

import com.smartcbwtf.domain.HcfAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HcfAuditLogRepository extends JpaRepository<HcfAuditLog, UUID> {

    /**
     * Find all audit logs for an HCF, ordered by change time descending.
     */
    List<HcfAuditLog> findByHcfIdOrderByChangedAtDesc(UUID hcfId);

    /**
     * Find audit logs for a specific field.
     */
    List<HcfAuditLog> findByHcfIdAndFieldName(UUID hcfId, String fieldName);
}
