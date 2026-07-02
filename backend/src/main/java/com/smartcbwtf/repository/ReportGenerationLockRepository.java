package com.smartcbwtf.repository;

import com.smartcbwtf.domain.ReportGenerationLock;
import com.smartcbwtf.domain.ReportGenerationLockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ReportGenerationLockRepository extends JpaRepository<ReportGenerationLock, ReportGenerationLockId> {

    void deleteByReportTypeAndPeriodKeyAndFacilityId(String reportType, String periodKey, UUID facilityId);

    long deleteByReportTypeAndPeriodKeyAndFacilityIdAndLockedAtBefore(
            String reportType, String periodKey, UUID facilityId, Instant lockedAt);
}
