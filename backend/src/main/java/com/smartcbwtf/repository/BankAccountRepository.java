package com.smartcbwtf.repository;

import com.smartcbwtf.domain.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for bank account management.
 */
@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    /**
     * Find all bank accounts for a CBWTF.
     */
    List<BankAccount> findByFacilityIdOrderByIsPrimaryDescCreatedAtDesc(UUID facilityId);

    /**
     * Find the primary bank account for a CBWTF.
     */
    Optional<BankAccount> findByFacilityIdAndIsPrimaryTrue(UUID facilityId);

    /**
     * Count accounts for a facility.
     */
    long countByFacilityId(UUID facilityId);

    /**
     * Find accounts by facility and status.
     */
    List<BankAccount> findByFacilityIdAndStatus(UUID facilityId, BankAccount.Status status);

    /**
     * Count active accounts for a facility.
     */
    long countByFacilityIdAndStatus(UUID facilityId, BankAccount.Status status);

    /**
     * Clear primary flag for all accounts of a facility.
     */
    @Modifying
    @Query("UPDATE BankAccount b SET b.isPrimary = false WHERE b.facility.id = :facilityId")
    void clearPrimaryForFacility(UUID facilityId);
}
