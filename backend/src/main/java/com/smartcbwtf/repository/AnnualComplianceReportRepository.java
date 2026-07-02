package com.smartcbwtf.repository;

import com.smartcbwtf.domain.AnnualComplianceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnualComplianceReportRepository extends JpaRepository<AnnualComplianceReport, UUID> {

    @Query("SELECT r FROM AnnualComplianceReport r WHERE r.id = :id AND r.facility.id = :facilityId")
    Optional<AnnualComplianceReport> findByIdAndFacilityId(@Param("id") UUID id,
            @Param("facilityId") UUID facilityId);

    Page<AnnualComplianceReport> findByFacilityId(UUID facilityId, Pageable pageable);

    Optional<AnnualComplianceReport> findByFacilityIdAndFinancialYear(UUID facilityId, String financialYear);

    boolean existsByFacilityIdAndFinancialYear(UUID facilityId, String financialYear);
}
