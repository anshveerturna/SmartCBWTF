package com.smartcbwtf.repository;

import com.smartcbwtf.domain.PaymentReversal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentReversalRepository extends JpaRepository<PaymentReversal, UUID> {

    boolean existsByOriginalPaymentId(UUID originalPaymentId);

    boolean existsByReversalPaymentId(UUID reversalPaymentId);

    Optional<PaymentReversal> findByOriginalPaymentId(UUID originalPaymentId);

    @Query("SELECT pr.originalPayment.id FROM PaymentReversal pr WHERE pr.originalPayment.id IN :paymentIds")
    List<UUID> findOriginalPaymentIdsIn(@Param("paymentIds") List<UUID> paymentIds);

    @Query("SELECT pr.reversalPayment.id FROM PaymentReversal pr WHERE pr.reversalPayment.id IN :paymentIds")
    List<UUID> findReversalPaymentIdsIn(@Param("paymentIds") List<UUID> paymentIds);
}
