package com.smartcbwtf.repository;

import com.smartcbwtf.domain.ConsumableOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConsumableOrderRepository extends JpaRepository<ConsumableOrder, UUID> {

    List<ConsumableOrder> findByHcfIdOrderByOrderedAtDesc(UUID hcfId);

    List<ConsumableOrder> findByHcfIdAndStatusOrderByOrderedAtDesc(UUID hcfId, String status);

    List<ConsumableOrder> findByFacilityIdOrderByOrderedAtDesc(UUID facilityId);

    List<ConsumableOrder> findByFacilityIdAndStatusOrderByOrderedAtDesc(UUID facilityId, String status);

    long countByFacilityId(UUID facilityId);
}
