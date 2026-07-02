package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    // Find all vehicles for a facility
    List<Vehicle> findByFacilityIdAndStatus(UUID facilityId, String status);

    List<Vehicle> findByFacilityId(UUID facilityId);

    Page<Vehicle> findByFacilityId(UUID facilityId, Pageable pageable);

    Optional<Vehicle> findByIdAndFacilityId(UUID id, UUID facilityId);

    Optional<Vehicle> findByIdAndFacilityIdAndStatus(UUID id, UUID facilityId, String status);

    // Find vehicle by registration number within a facility
    Optional<Vehicle> findByFacilityIdAndRegistrationNumber(UUID facilityId, String registrationNumber);

    // Find vehicle by GPS device ID
    Optional<Vehicle> findByGpsDeviceId(String gpsDeviceId);

    // Count active vehicles for a facility
    long countByFacilityIdAndStatus(UUID facilityId, String status);

    // Find vehicles with GPS online (last GPS within threshold)
    @Query("SELECT v FROM Vehicle v WHERE v.facility.id = :facilityId " +
            "AND v.lastGpsAt IS NOT NULL AND v.lastGpsAt > :threshold")
    List<Vehicle> findOnlineVehicles(@Param("facilityId") UUID facilityId,
            @Param("threshold") Instant threshold);

    // Count online vehicles
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.facility.id = :facilityId " +
            "AND v.lastGpsAt IS NOT NULL AND v.lastGpsAt > :threshold")
    long countOnlineVehicles(@Param("facilityId") UUID facilityId,
            @Param("threshold") Instant threshold);

    // Find vehicles needing status update (offline detection)
    @Query("SELECT v FROM Vehicle v WHERE v.gpsStatus = 'ONLINE' " +
            "AND (v.lastGpsAt IS NULL OR v.lastGpsAt < :threshold)")
    List<Vehicle> findVehiclesToMarkOffline(@Param("threshold") Instant threshold);
}
