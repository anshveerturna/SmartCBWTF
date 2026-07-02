package com.smartcbwtf.repository;

import com.smartcbwtf.domain.QrLabelOrder;
import com.smartcbwtf.domain.QrLabelOrder.QrOrderStatus;
import com.smartcbwtf.domain.QrLabelOrder.QrOrderType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QrLabelOrderRepository extends JpaRepository<QrLabelOrder, UUID> {

        List<QrLabelOrder> findByFacilityIdAndStatusOrderByRequestedAtDesc(UUID facilityId, QrOrderStatus status);

        @Query("SELECT o FROM QrLabelOrder o LEFT JOIN FETCH o.hcf WHERE o.facility.id = :facilityId ORDER BY o.requestedAt DESC")
        List<QrLabelOrder> findByFacilityIdOrderByRequestedAtDesc(@Param("facilityId") UUID facilityId);

        @Query("SELECT o FROM QrLabelOrder o LEFT JOIN FETCH o.hcf WHERE o.facility.id = :facilityId ORDER BY o.requestedAt DESC")
        List<QrLabelOrder> findRecentByFacilityId(
                        @Param("facilityId") UUID facilityId,
                        Pageable pageable);

        @Query("SELECT o FROM QrLabelOrder o LEFT JOIN FETCH o.hcf WHERE o.facility.id = :facilityId " +
                        "AND o.status = :status ORDER BY o.requestedAt DESC")
        List<QrLabelOrder> findRecentByFacilityIdAndStatus(
                        @Param("facilityId") UUID facilityId,
                        @Param("status") QrOrderStatus status,
                        Pageable pageable);

        List<QrLabelOrder> findByHcfIdOrderByRequestedAtDesc(UUID hcfId);

        List<QrLabelOrder> findByHcfIdOrderByRequestedAtDesc(UUID hcfId, Pageable pageable);

        List<QrLabelOrder> findByHcfIdAndFacilityIdOrderByRequestedAtDesc(UUID hcfId, UUID facilityId,
                        Pageable pageable);

        List<QrLabelOrder> findByHcfIdAndStatusOrderByRequestedAtDesc(UUID hcfId, QrOrderStatus status);

        java.util.Optional<QrLabelOrder> findByIdAndFacilityId(UUID id, UUID facilityId);

        java.util.Optional<QrLabelOrder> findByIdAndHcfId(UUID id, UUID hcfId);

        java.util.Optional<QrLabelOrder> findByIdAndHcfIdAndFacilityId(UUID id, UUID hcfId, UUID facilityId);

        @Query("SELECT o FROM QrLabelOrder o LEFT JOIN FETCH o.hcf WHERE o.facility.id = :facilityId " +
                        "AND o.status = 'PENDING' ORDER BY o.requestedAt ASC")
        List<QrLabelOrder> findPendingOrdersByFacility(@Param("facilityId") UUID facilityId);

        @Query("SELECT o FROM QrLabelOrder o LEFT JOIN FETCH o.hcf WHERE o.facility.id = :facilityId " +
                        "AND o.status = 'PENDING' ORDER BY o.requestedAt ASC")
        List<QrLabelOrder> findPendingOrdersByFacility(@Param("facilityId") UUID facilityId, Pageable pageable);

        @Query("SELECT o FROM QrLabelOrder o LEFT JOIN FETCH o.hcf WHERE o.facility.id = :facilityId " +
                        "AND o.orderType = :orderType ORDER BY o.requestedAt DESC")
        List<QrLabelOrder> findByFacilityAndOrderType(
                        @Param("facilityId") UUID facilityId,
                        @Param("orderType") QrOrderType orderType);

        @Query("SELECT COUNT(o) FROM QrLabelOrder o WHERE o.hcf.id = :hcfId AND o.status = 'PENDING'")
        long countPendingOrdersByHcf(@Param("hcfId") UUID hcfId);
}
