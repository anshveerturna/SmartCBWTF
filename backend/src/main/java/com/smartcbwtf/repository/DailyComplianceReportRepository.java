package com.smartcbwtf.repository;

import com.smartcbwtf.domain.DailyComplianceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyComplianceReportRepository extends JpaRepository<DailyComplianceReport, UUID> {

    Page<DailyComplianceReport> findByFacilityId(UUID facilityId, Pageable pageable);

    Optional<DailyComplianceReport> findByFacilityIdAndReportDate(UUID facilityId, LocalDate reportDate);

    boolean existsByFacilityIdAndReportDate(UUID facilityId, LocalDate reportDate);
}
