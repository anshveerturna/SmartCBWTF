package com.smartcbwtf.repository;

import com.smartcbwtf.domain.FacilityBranding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FacilityBrandingRepository extends JpaRepository<FacilityBranding, UUID> {
}
