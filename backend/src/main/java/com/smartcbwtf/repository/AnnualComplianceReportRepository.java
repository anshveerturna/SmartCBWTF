package com.smartcbwtf.repository;

import com.smartcbwtf.domain.AnnualComplianceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnualComplianceReportRepository extends JpaRepository<AnnualComplianceReport, UUID> {

    Page<AnnualComplianceReport> findByFacilityId(UUID facilityId, Pageable pageable);

    Optional<AnnualComplianceReport> findByFacilityIdAndFinancialYear(UUID facilityId, String financialYear);

    boolean existsByFacilityIdAndFinancialYear(UUID facilityId, String financialYear);
}
