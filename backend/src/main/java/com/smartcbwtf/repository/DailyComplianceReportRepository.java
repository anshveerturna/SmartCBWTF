package com.smartcbwtf.repository;

import com.smartcbwtf.domain.DailyComplianceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyComplianceReportRepository extends JpaRepository<DailyComplianceReport, UUID> {

    @Query("SELECT r FROM DailyComplianceReport r WHERE r.id = :id AND r.facility.id = :facilityId")
    Optional<DailyComplianceReport> findByIdAndFacilityId(@Param("id") UUID id,
            @Param("facilityId") UUID facilityId);

    Page<DailyComplianceReport> findByFacilityId(UUID facilityId, Pageable pageable);

    Optional<DailyComplianceReport> findByFacilityIdAndReportDate(UUID facilityId, LocalDate reportDate);

    boolean existsByFacilityIdAndReportDate(UUID facilityId, LocalDate reportDate);
}
