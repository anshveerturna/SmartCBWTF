package com.smartcbwtf.repository;

import com.smartcbwtf.domain.GpsVendorIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GpsVendorIntegrationRepository extends JpaRepository<GpsVendorIntegration, UUID> {

    Optional<GpsVendorIntegration> findByFacilityIdAndVendor(UUID facilityId, String vendor);

    List<GpsVendorIntegration> findByFacilityId(UUID facilityId);

    List<GpsVendorIntegration> findByFacilityIdAndStatus(UUID facilityId, String status);

    List<GpsVendorIntegration> findByVendorAndStatus(String vendor, String status);
}
