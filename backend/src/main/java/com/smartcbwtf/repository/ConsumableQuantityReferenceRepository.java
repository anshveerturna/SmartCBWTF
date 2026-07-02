package com.smartcbwtf.repository;

import com.smartcbwtf.domain.ConsumableQuantityReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsumableQuantityReferenceRepository extends JpaRepository<ConsumableQuantityReference, UUID> {
    Optional<ConsumableQuantityReference> findByConsumableItemId(UUID consumableItemId);
}
