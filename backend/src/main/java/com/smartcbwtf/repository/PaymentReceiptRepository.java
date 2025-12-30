package com.smartcbwtf.repository;

import com.smartcbwtf.domain.PaymentReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, UUID> {

    Optional<PaymentReceipt> findByPaymentId(UUID paymentId);

    boolean existsByPaymentId(UUID paymentId);
}
