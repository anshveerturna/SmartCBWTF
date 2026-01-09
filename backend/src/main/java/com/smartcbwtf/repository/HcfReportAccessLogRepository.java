package com.smartcbwtf.repository;

import com.smartcbwtf.domain.HcfReportAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface HcfReportAccessLogRepository extends JpaRepository<HcfReportAccessLog, UUID> {

    List<HcfReportAccessLog> findByHcfIdOrderByAccessedAtDesc(UUID hcfId);

    List<HcfReportAccessLog> findByHcfIdAndAccessedAtBetweenOrderByAccessedAtDesc(
            UUID hcfId, Instant start, Instant end);

    List<HcfReportAccessLog> findByAccessedByOrderByAccessedAtDesc(UUID userId);
}
