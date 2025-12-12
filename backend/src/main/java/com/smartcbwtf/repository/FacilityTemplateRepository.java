package com.smartcbwtf.repository;

import com.smartcbwtf.domain.FacilityTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacilityTemplateRepository extends JpaRepository<FacilityTemplate, UUID> {
    
    /**
     * Find active template for a facility
     */
    Optional<FacilityTemplate> findByFacilityIdAndActiveTrue(UUID facilityId);
    
    /**
     * Find all templates for a facility
     */
    List<FacilityTemplate> findByFacilityIdOrderByCreatedAtDesc(UUID facilityId);
    
    /**
     * Find template by facility and version
     */
    Optional<FacilityTemplate> findByFacilityIdAndVersion(UUID facilityId, String version);
    
    /**
     * Deactivate all templates for a facility
     */
    @Modifying
    @Query("UPDATE FacilityTemplate t SET t.active = false WHERE t.facility.id = :facilityId")
    void deactivateAllForFacility(@Param("facilityId") UUID facilityId);
    
    /**
     * Check if any active template exists
     */
    boolean existsByFacilityIdAndActiveTrue(UUID facilityId);
}
