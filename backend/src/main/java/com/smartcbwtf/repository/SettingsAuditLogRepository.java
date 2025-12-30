package com.smartcbwtf.repository;

import com.smartcbwtf.domain.SettingsAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SettingsAuditLogRepository extends JpaRepository<SettingsAuditLog, UUID> {

    Page<SettingsAuditLog> findByFacilityIdOrderByChangedAtDesc(UUID facilityId, Pageable pageable);

    Page<SettingsAuditLog> findByFacilityIdAndSectionOrderByChangedAtDesc(UUID facilityId, String section,
            Pageable pageable);
}
