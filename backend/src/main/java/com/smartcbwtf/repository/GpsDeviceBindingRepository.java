package com.smartcbwtf.repository;

import com.smartcbwtf.domain.GpsDeviceBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GpsDeviceBindingRepository extends JpaRepository<GpsDeviceBinding, UUID> {

    // Get binding history for a device
    List<GpsDeviceBinding> findByDeviceIdOrderByPerformedAtDesc(String deviceId);

    // Get binding history for a vehicle
    List<GpsDeviceBinding> findByVehicleIdOrderByPerformedAtDesc(UUID vehicleId);

    // Get latest binding for a device
    @Query("SELECT b FROM GpsDeviceBinding b WHERE b.deviceId = :deviceId " +
            "ORDER BY b.performedAt DESC LIMIT 1")
    Optional<GpsDeviceBinding> findLatestByDeviceId(@Param("deviceId") String deviceId);

    // Check if device is currently bound to any vehicle
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM GpsDeviceBinding b " +
            "WHERE b.deviceId = :deviceId AND b.action = 'BOUND' " +
            "AND NOT EXISTS (SELECT 1 FROM GpsDeviceBinding b2 WHERE b2.deviceId = :deviceId " +
            "AND b2.action = 'UNBOUND' AND b2.performedAt > b.performedAt)")
    boolean isDeviceCurrentlyBound(@Param("deviceId") String deviceId);
}
