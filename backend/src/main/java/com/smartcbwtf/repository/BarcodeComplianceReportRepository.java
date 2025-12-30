package com.smartcbwtf.repository;

import com.smartcbwtf.domain.BarcodeComplianceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BarcodeComplianceReportRepository extends JpaRepository<BarcodeComplianceReport, UUID> {

    Page<BarcodeComplianceReport> findByFacilityId(UUID facilityId, Pageable pageable);

    Optional<BarcodeComplianceReport> findByFacilityIdAndReportDateAndReportType(
            UUID facilityId, LocalDate reportDate, BarcodeComplianceReport.ReportType reportType);

    boolean existsByFacilityIdAndReportDateAndReportType(
            UUID facilityId, LocalDate reportDate, BarcodeComplianceReport.ReportType reportType);
}
