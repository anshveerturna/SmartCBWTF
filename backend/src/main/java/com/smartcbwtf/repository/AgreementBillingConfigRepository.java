package com.smartcbwtf.repository;

import com.smartcbwtf.domain.AgreementBillingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AgreementBillingConfig.
 */
@Repository
public interface AgreementBillingConfigRepository extends JpaRepository<AgreementBillingConfig, UUID> {

    /**
     * Find the current active billing config for an agreement.
     * Active means effective_to IS NULL.
     */
    @Query("SELECT c FROM AgreementBillingConfig c WHERE c.agreement.id = :agreementId AND c.effectiveTo IS NULL")
    Optional<AgreementBillingConfig> findActiveByAgreementId(UUID agreementId);

    /**
     * Find all billing configs for an agreement (history).
     */
    List<AgreementBillingConfig> findByAgreementIdOrderByEffectiveFromDesc(UUID agreementId);

    /**
     * Check if an active config exists for the agreement.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM AgreementBillingConfig c WHERE c.agreement.id = :agreementId AND c.effectiveTo IS NULL")
    boolean existsActiveByAgreementId(UUID agreementId);
}
