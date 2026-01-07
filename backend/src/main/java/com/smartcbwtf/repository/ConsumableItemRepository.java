package com.smartcbwtf.repository;

import com.smartcbwtf.domain.ConsumableItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsumableItemRepository extends JpaRepository<ConsumableItem, UUID> {

    @Query("SELECT c FROM ConsumableItem c JOIN FETCH c.category WHERE c.facility.id = :facilityId ORDER BY c.name ASC")
    List<ConsumableItem> findByFacilityIdOrderByName(@Param("facilityId") UUID facilityId);

    @Query("SELECT c FROM ConsumableItem c JOIN FETCH c.category WHERE c.facility.id = :facilityId AND c.isActive = true ORDER BY c.name ASC")
    List<ConsumableItem> findActiveByFacility(@Param("facilityId") UUID facilityId);

    @Query("SELECT c FROM ConsumableItem c JOIN FETCH c.category WHERE c.id = :id AND c.facility.id = :facilityId")
    Optional<ConsumableItem> findByIdAndFacilityId(@Param("id") UUID id, @Param("facilityId") UUID facilityId);

    boolean existsByFacilityIdAndConsumableCodeIgnoreCase(UUID facilityId, String consumableCode);
}
