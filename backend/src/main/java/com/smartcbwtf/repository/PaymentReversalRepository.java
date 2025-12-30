package com.smartcbwtf.repository;

import com.smartcbwtf.domain.PaymentReversal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentReversalRepository extends JpaRepository<PaymentReversal, UUID> {

    boolean existsByOriginalPaymentId(UUID originalPaymentId);

    Optional<PaymentReversal> findByOriginalPaymentId(UUID originalPaymentId);
}
