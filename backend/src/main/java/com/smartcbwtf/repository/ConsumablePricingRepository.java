package com.smartcbwtf.repository;

import com.smartcbwtf.domain.ConsumablePricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsumablePricingRepository extends JpaRepository<ConsumablePricing, UUID> {

    @Query("SELECT p FROM ConsumablePricing p WHERE p.consumableItem.id = :consumableItemId ORDER BY p.createdAt DESC")
    List<ConsumablePricing> findByConsumableItemIdOrderByCreatedAtDesc(
            @Param("consumableItemId") UUID consumableItemId);

    @Query("SELECT p FROM ConsumablePricing p WHERE p.consumableItem.id = :consumableItemId AND p.isActive = true")
    Optional<ConsumablePricing> findActiveByConsumableItemId(@Param("consumableItemId") UUID consumableItemId);

    @Modifying
    @Query("UPDATE ConsumablePricing p SET p.isActive = false WHERE p.consumableItem.id = :consumableItemId AND p.isActive = true")
    int deactivateAllForConsumable(@Param("consumableItemId") UUID consumableItemId);
}
