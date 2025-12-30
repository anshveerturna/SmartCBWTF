package com.smartcbwtf.repository;

import com.smartcbwtf.domain.EmailDispatchLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailDispatchLogRepository extends JpaRepository<EmailDispatchLog, UUID> {

    Page<EmailDispatchLog> findByFacilityId(UUID facilityId, Pageable pageable);

    Optional<EmailDispatchLog> findByEventIdAndTemplateCode(UUID eventId, String templateCode);

    boolean existsByEventIdAndTemplateCode(UUID eventId, String templateCode);

    long countByEntityTypeAndEntityId(String entityType, UUID entityId);
}
