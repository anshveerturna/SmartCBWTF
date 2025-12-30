package com.smartcbwtf.repository;

import com.smartcbwtf.domain.MonthlyComplianceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonthlyComplianceReportRepository extends JpaRepository<MonthlyComplianceReport, UUID> {

    Page<MonthlyComplianceReport> findByFacilityId(UUID facilityId, Pageable pageable);

    Optional<MonthlyComplianceReport> findByFacilityIdAndReportMonth(UUID facilityId, LocalDate reportMonth);

    boolean existsByFacilityIdAndReportMonth(UUID facilityId, LocalDate reportMonth);
}
