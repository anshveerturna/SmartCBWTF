package com.smartcbwtf.service;

import com.smartcbwtf.domain.AuditLog;
import com.smartcbwtf.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String entityType, UUID entityId, String action, UUID actorUserId, String dataJson) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setActorUserId(actorUserId);
        log.setDataJson(dataJson);
        auditLogRepository.save(log);
    }

    /**
     * Log with data hash for tamper-evident audit trail.
     */
    public void logWithData(String entityType, UUID entityId, String action, 
                            UUID actorUserId, String dataJson, String dataHash) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setActorUserId(actorUserId);
        log.setDataJson(dataJson);
        log.setDataHash(dataHash);
        auditLogRepository.save(log);
    }
}
