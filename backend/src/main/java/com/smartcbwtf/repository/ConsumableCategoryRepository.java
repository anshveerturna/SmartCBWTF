package com.smartcbwtf.repository;

import com.smartcbwtf.domain.ConsumableCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConsumableCategoryRepository extends JpaRepository<ConsumableCategory, UUID> {

    @Query("SELECT c FROM ConsumableCategory c WHERE c.facility.id = :facilityId ORDER BY c.displayOrder ASC")
    List<ConsumableCategory> findByFacilityIdOrderByDisplayOrder(@Param("facilityId") UUID facilityId);

    @Query("SELECT c FROM ConsumableCategory c WHERE c.facility.id = :facilityId AND c.isActive = true ORDER BY c.displayOrder ASC")
    List<ConsumableCategory> findActiveCategoriesByFacility(@Param("facilityId") UUID facilityId);
}
