package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {

        Optional<Bill> findByAgreementIdAndBillingMonth(UUID agreementId, LocalDate billingMonth);

        @Query("SELECT b FROM Bill b WHERE b.id = :id AND b.facility.id = :facilityId")
        @EntityGraph(attributePaths = { "agreement", "agreement.hcf", "snapshot" })
        Optional<Bill> findByIdAndFacilityId(@Param("id") UUID id, @Param("facilityId") UUID facilityId);

        boolean existsByAgreementIdAndBillingMonth(UUID agreementId, LocalDate billingMonth);

        @Query("SELECT b FROM Bill b WHERE b.facility.id = :facilityId ORDER BY b.billingMonth DESC")
        @EntityGraph(attributePaths = { "agreement", "agreement.hcf" })
        Page<Bill> findByFacilityId(@Param("facilityId") UUID facilityId, Pageable pageable);

        @Query("SELECT b FROM Bill b WHERE b.facility.id = :facilityId AND b.billingMonth = :billingMonth")
        @EntityGraph(attributePaths = { "agreement", "agreement.hcf" })
        List<Bill> findByFacilityAndMonth(
                        @Param("facilityId") UUID facilityId,
                        @Param("billingMonth") LocalDate billingMonth);

        // Alias for TallyExportService
        @Query("SELECT b FROM Bill b WHERE b.facility.id = :facilityId AND b.billingMonth = :billingMonth")
        @EntityGraph(attributePaths = { "agreement", "agreement.hcf", "snapshot" })
        List<Bill> findByFacilityIdAndBillingMonth(
                        @Param("facilityId") UUID facilityId,
                        @Param("billingMonth") LocalDate billingMonth);

        @Query("SELECT b FROM Bill b WHERE b.facility.id = :facilityId AND b.billingMonth BETWEEN :startMonth AND :endMonth ORDER BY b.billingMonth DESC")
        @EntityGraph(attributePaths = { "agreement", "agreement.hcf", "snapshot" })
        List<Bill> findByFacilityAndMonthRange(
                        @Param("facilityId") UUID facilityId,
                        @Param("startMonth") LocalDate startMonth,
                        @Param("endMonth") LocalDate endMonth);
}
