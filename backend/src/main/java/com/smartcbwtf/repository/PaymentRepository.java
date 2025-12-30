package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findByFacilityId(UUID facilityId, Pageable pageable);

    Page<Payment> findByHcfId(UUID hcfId, Pageable pageable);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.facility.id = :facilityId AND p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalCollected(@Param("facilityId") UUID facilityId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
