package com.smartcbwtf.repository;

import com.smartcbwtf.domain.PaymentReceiptSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentReceiptSequenceRepository
        extends JpaRepository<PaymentReceiptSequence, PaymentReceiptSequence.SequenceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PaymentReceiptSequence s WHERE s.facilityId = :facilityId AND s.financialYear = :fy")
    Optional<PaymentReceiptSequence> findByFacilityIdAndFinancialYearForUpdate(
            @Param("facilityId") UUID facilityId,
            @Param("fy") String financialYear);
}
