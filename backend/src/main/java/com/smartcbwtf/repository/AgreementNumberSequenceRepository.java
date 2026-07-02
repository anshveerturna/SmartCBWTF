package com.smartcbwtf.repository;

import com.smartcbwtf.domain.AgreementNumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AgreementNumberSequenceRepository extends JpaRepository<AgreementNumberSequence, UUID> {
    
    /**
     * Find and lock the sequence for atomic increment
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM AgreementNumberSequence s
            WHERE s.facility.id = :facilityId
              AND s.year = :year
              AND s.periodMonth = :periodMonth
            """)
    Optional<AgreementNumberSequence> findByFacilityIdAndYearAndPeriodMonthForUpdate(
            @Param("facilityId") UUID facilityId, 
            @Param("year") Integer year,
            @Param("periodMonth") Integer periodMonth
    );
    
    /**
     * Find sequence without lock (for reads)
     */
    Optional<AgreementNumberSequence> findByFacilityIdAndYearAndPeriodMonth(
            UUID facilityId,
            Integer year,
            Integer periodMonth
    );
}
