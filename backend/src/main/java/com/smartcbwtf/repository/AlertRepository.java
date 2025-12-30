package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Alert;
import com.smartcbwtf.domain.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Page<Alert> findByFacilityId(UUID facilityId, Pageable pageable);

    Page<Alert> findByFacilityIdAndType(UUID facilityId, AlertType type, Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.facility.id = :facilityId AND a.type IN :types")
    Page<Alert> findByFacilityIdAndTypeIn(
            @Param("facilityId") UUID facilityId,
            @Param("types") java.util.List<AlertType> types,
            Pageable pageable);

    long countByFacilityIdAndIsReadFalse(UUID facilityId);

    boolean existsByEventIdAndType(UUID eventId, AlertType type);

    Optional<Alert> findByEventIdAndType(UUID eventId, AlertType type);

    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true WHERE a.id = :id AND a.facility.id = :facilityId")
    int markAsRead(@Param("id") UUID id, @Param("facilityId") UUID facilityId);

    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true WHERE a.facility.id = :facilityId AND a.isRead = false")
    int markAllAsRead(@Param("facilityId") UUID facilityId);
}
