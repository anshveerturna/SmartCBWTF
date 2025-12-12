package com.smartcbwtf.repository;

import com.smartcbwtf.domain.FacilityTerms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacilityTermsRepository extends JpaRepository<FacilityTerms, UUID> {
    
    /**
     * Find active terms for a facility
     */
    Optional<FacilityTerms> findByFacilityIdAndActiveTrue(UUID facilityId);
    
    /**
     * Find all terms versions for a facility
     */
    List<FacilityTerms> findByFacilityIdOrderByEffectiveFromDesc(UUID facilityId);
    
    /**
     * Find terms by facility and version
     */
    Optional<FacilityTerms> findByFacilityIdAndVersion(UUID facilityId, String version);
    
    /**
     * Deactivate all terms for a facility
     */
    @Modifying
    @Query("UPDATE FacilityTerms t SET t.active = false WHERE t.facility.id = :facilityId")
    void deactivateAllForFacility(@Param("facilityId") UUID facilityId);
    
    /**
     * Find the latest active terms across all facilities (for global default)
     */
    @Query("SELECT t FROM FacilityTerms t WHERE t.active = true ORDER BY t.effectiveFrom DESC")
    List<FacilityTerms> findAllActive();
}
