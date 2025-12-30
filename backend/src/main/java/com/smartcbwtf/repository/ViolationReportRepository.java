package com.smartcbwtf.repository;

import com.smartcbwtf.domain.ViolationReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViolationReportRepository extends JpaRepository<ViolationReport, UUID> {

    Page<ViolationReport> findByFacilityId(UUID facilityId, Pageable pageable);

    Optional<ViolationReport> findByFacilityIdAndReportDate(UUID facilityId, LocalDate reportDate);

    boolean existsByFacilityIdAndReportDate(UUID facilityId, LocalDate reportDate);
}
