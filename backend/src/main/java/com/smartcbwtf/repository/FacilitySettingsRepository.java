package com.smartcbwtf.repository;

import com.smartcbwtf.domain.FacilitySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FacilitySettingsRepository extends JpaRepository<FacilitySettings, UUID> {
    // Primary key is facility_id, so findById works directly
}
