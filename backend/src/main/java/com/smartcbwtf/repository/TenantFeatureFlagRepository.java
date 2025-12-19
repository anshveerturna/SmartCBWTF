package com.smartcbwtf.repository;

import com.smartcbwtf.domain.TenantFeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantFeatureFlagRepository extends JpaRepository<TenantFeatureFlag, UUID> {

    /**
     * Find all feature flags for a tenant
     */
    List<TenantFeatureFlag> findByFacilityId(UUID facilityId);

    /**
     * Find specific feature flag for a tenant
     */
    Optional<TenantFeatureFlag> findByFacilityIdAndFeatureKey(UUID facilityId, String featureKey);

    /**
     * Check if a feature is enabled for a tenant
     */
    default boolean isFeatureEnabled(UUID facilityId, String featureKey) {
        return findByFacilityIdAndFeatureKey(facilityId, featureKey)
                .map(TenantFeatureFlag::isEnabled)
                .orElse(false);
    }

    /**
     * Find all facilities with a specific feature enabled
     */
    List<TenantFeatureFlag> findByFeatureKeyAndEnabledTrue(String featureKey);

    /**
     * Delete all flags for a facility
     */
    void deleteByFacilityId(UUID facilityId);
}
