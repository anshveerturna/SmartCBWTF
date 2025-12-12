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
    @Query("SELECT s FROM AgreementNumberSequence s WHERE s.facility.id = :facilityId AND s.year = :year")
    Optional<AgreementNumberSequence> findByFacilityIdAndYearForUpdate(
            @Param("facilityId") UUID facilityId, 
            @Param("year") Integer year
    );
    
    /**
     * Find sequence without lock (for reads)
     */
    Optional<AgreementNumberSequence> findByFacilityIdAndYear(UUID facilityId, Integer year);
}
