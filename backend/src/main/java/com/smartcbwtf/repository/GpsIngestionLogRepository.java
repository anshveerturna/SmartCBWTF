package com.smartcbwtf.repository;

import com.smartcbwtf.domain.GpsIngestionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GpsIngestionLogRepository extends JpaRepository<GpsIngestionLog, UUID> {

    Optional<GpsIngestionLog> findByFacilityIdAndVendor(UUID facilityId, String vendor);

    List<GpsIngestionLog> findByFacilityId(UUID facilityId);
}
