package com.smartcbwtf.repository;

import com.smartcbwtf.domain.SystemConfigAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SystemConfigAuditRepository extends JpaRepository<SystemConfigAudit, UUID> {

    List<SystemConfigAudit> findByConfigKeyOrderByChangedAtDesc(String configKey);

    Page<SystemConfigAudit> findByConfigKeyOrderByChangedAtDesc(String configKey, Pageable pageable);

    List<SystemConfigAudit> findTop20ByOrderByChangedAtDesc();
}
