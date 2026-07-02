package com.smartcbwtf.repository;

import com.smartcbwtf.domain.HcfAdvanceLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface HcfAdvanceLedgerRepository extends JpaRepository<HcfAdvanceLedger, UUID> {

    List<HcfAdvanceLedger> findByHcfId(UUID hcfId);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM HcfAdvanceLedger l WHERE l.hcf.id = :hcfId")
    BigDecimal getAdvanceBalance(@Param("hcfId") UUID hcfId);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM HcfAdvanceLedger l WHERE l.hcf.id = :hcfId AND l.sourcePayment.facility.id = :facilityId")
    BigDecimal getAdvanceBalanceForFacility(@Param("facilityId") UUID facilityId, @Param("hcfId") UUID hcfId);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM HcfAdvanceLedger l WHERE l.sourcePayment.facility.id = :facilityId")
    BigDecimal getTotalAdvanceBalanceForFacility(@Param("facilityId") UUID facilityId);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM HcfAdvanceLedger l WHERE l.sourcePayment.id = :paymentId")
    BigDecimal sumByPaymentId(@Param("paymentId") UUID paymentId);
}
