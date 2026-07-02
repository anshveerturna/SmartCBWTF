package com.smartcbwtf.repository;

import com.smartcbwtf.domain.ConsumableOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsumableOrderRepository extends JpaRepository<ConsumableOrder, UUID> {

    @Query("SELECT o FROM ConsumableOrder o WHERE o.id = :id AND o.facility.id = :facilityId")
    Optional<ConsumableOrder> findByIdAndFacilityId(@Param("id") UUID id, @Param("facilityId") UUID facilityId);

    @Query("SELECT o FROM ConsumableOrder o WHERE o.id = :id AND o.hcf.id = :hcfId")
    Optional<ConsumableOrder> findByIdAndHcfId(@Param("id") UUID id, @Param("hcfId") UUID hcfId);

    @Query("SELECT o FROM ConsumableOrder o WHERE o.id = :id AND o.hcf.id = :hcfId AND o.facility.id = :facilityId")
    Optional<ConsumableOrder> findByIdAndHcfIdAndFacilityId(@Param("id") UUID id, @Param("hcfId") UUID hcfId,
            @Param("facilityId") UUID facilityId);

    List<ConsumableOrder> findByHcfIdOrderByOrderedAtDesc(UUID hcfId);

    List<ConsumableOrder> findByHcfIdOrderByOrderedAtDesc(UUID hcfId, Pageable pageable);

    List<ConsumableOrder> findByHcfIdAndFacilityIdOrderByOrderedAtDesc(UUID hcfId, UUID facilityId,
            Pageable pageable);

    List<ConsumableOrder> findByHcfIdAndStatusOrderByOrderedAtDesc(UUID hcfId, String status);

    List<ConsumableOrder> findByHcfIdAndStatusOrderByOrderedAtDesc(UUID hcfId, String status, Pageable pageable);

    List<ConsumableOrder> findByHcfIdAndFacilityIdAndStatusOrderByOrderedAtDesc(UUID hcfId, UUID facilityId,
            String status, Pageable pageable);

    List<ConsumableOrder> findByFacilityIdOrderByOrderedAtDesc(UUID facilityId);

    List<ConsumableOrder> findByFacilityIdOrderByOrderedAtDesc(UUID facilityId, Pageable pageable);

    List<ConsumableOrder> findByFacilityIdAndStatusOrderByOrderedAtDesc(UUID facilityId, String status);

    List<ConsumableOrder> findByFacilityIdAndStatusOrderByOrderedAtDesc(UUID facilityId, String status,
            Pageable pageable);

    long countByFacilityId(UUID facilityId);

    long countByFacilityIdAndStatus(UUID facilityId, String status);

    long countByFacilityIdAndOrderedAtAfter(UUID facilityId, java.time.Instant orderedAt);

    long countByHcfId(UUID hcfId);

    long countByHcfIdAndStatus(UUID hcfId, String status);

    long countByHcfIdAndFacilityId(UUID hcfId, UUID facilityId);

    long countByHcfIdAndFacilityIdAndStatus(UUID hcfId, UUID facilityId, String status);

    List<ConsumableOrder> findByFacilityIdAndOrderedAtAfterOrderByOrderedAtDesc(UUID facilityId,
            java.time.Instant orderedAt);

    @Query("""
            SELECT DISTINCT o FROM ConsumableOrder o
            JOIN FETCH o.hcf
            LEFT JOIN FETCH o.items
            WHERE o.facility.id = :facilityId AND o.orderedAt > :orderedAt
            ORDER BY o.orderedAt DESC
            """)
    List<ConsumableOrder> findExportRowsByFacilityIdAndOrderedAtAfter(
            @Param("facilityId") UUID facilityId,
            @Param("orderedAt") java.time.Instant orderedAt);

    @Query("SELECT i.order.id, COUNT(i) FROM ConsumableOrderItem i WHERE i.order.id IN :orderIds GROUP BY i.order.id")
    List<Object[]> countItemsByOrderIds(@Param("orderIds") List<UUID> orderIds);
}
