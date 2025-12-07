package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgreementRepository extends JpaRepository<Agreement, UUID> {
    Optional<Agreement> findByAgreementNumber(String agreementNumber);

    Optional<Agreement> findFirstByHcfIdAndStatusOrderByStartDateDesc(UUID hcfId, String status);
}
