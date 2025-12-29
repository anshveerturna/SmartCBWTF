package com.smartcbwtf.repository;

import com.smartcbwtf.domain.BillingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingSnapshotRepository extends JpaRepository<BillingSnapshot, UUID> {

    Optional<BillingSnapshot> findByAgreementIdAndBillingMonth(UUID agreementId, LocalDate billingMonth);

    boolean existsByAgreementIdAndBillingMonth(UUID agreementId, LocalDate billingMonth);

    @Query("SELECT s FROM BillingSnapshot s WHERE s.facility.id = :facilityId AND s.billingMonth = :billingMonth")
    java.util.List<BillingSnapshot> findByFacilityAndMonth(
            @Param("facilityId") UUID facilityId,
            @Param("billingMonth") LocalDate billingMonth);
}
