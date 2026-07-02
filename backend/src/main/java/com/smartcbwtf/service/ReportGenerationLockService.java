package com.smartcbwtf.service;

import com.smartcbwtf.domain.ReportGenerationLock;
import com.smartcbwtf.repository.ReportGenerationLockRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class ReportGenerationLockService {

    private static final Duration STALE_LOCK_AFTER = Duration.ofHours(6);

    private final ReportGenerationLockRepository lockRepository;

    public ReportGenerationLockService(ReportGenerationLockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean acquire(String reportType, String periodKey, UUID facilityId) {
        lockRepository.deleteByReportTypeAndPeriodKeyAndFacilityIdAndLockedAtBefore(
                reportType, periodKey, facilityId, Instant.now().minus(STALE_LOCK_AFTER));

        try {
            ReportGenerationLock lock = new ReportGenerationLock();
            lock.setReportType(reportType);
            lock.setPeriodKey(periodKey);
            lock.setFacilityId(facilityId);
            lock.setLockedAt(Instant.now());
            lockRepository.saveAndFlush(lock);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String reportType, String periodKey, UUID facilityId) {
        lockRepository.deleteByReportTypeAndPeriodKeyAndFacilityId(reportType, periodKey, facilityId);
    }
}
