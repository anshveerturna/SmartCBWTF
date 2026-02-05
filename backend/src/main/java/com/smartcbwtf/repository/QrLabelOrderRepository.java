package com.smartcbwtf.repository;

import com.smartcbwtf.domain.QrLabelOrder;
import com.smartcbwtf.domain.QrLabelOrder.QrOrderStatus;
import com.smartcbwtf.domain.QrLabelOrder.QrOrderType;
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

        List<QrLabelOrder> findByHcfIdOrderByRequestedAtDesc(UUID hcfId);

        List<QrLabelOrder> findByHcfIdAndStatusOrderByRequestedAtDesc(UUID hcfId, QrOrderStatus status);

        @Query("SELECT o FROM QrLabelOrder o LEFT JOIN FETCH o.hcf WHERE o.facility.id = :facilityId " +
                        "AND o.status = 'PENDING' ORDER BY o.requestedAt ASC")
        List<QrLabelOrder> findPendingOrdersByFacility(@Param("facilityId") UUID facilityId);

        @Query("SELECT o FROM QrLabelOrder o LEFT JOIN FETCH o.hcf WHERE o.facility.id = :facilityId " +
                        "AND o.orderType = :orderType ORDER BY o.requestedAt DESC")
        List<QrLabelOrder> findByFacilityAndOrderType(
                        @Param("facilityId") UUID facilityId,
                        @Param("orderType") QrOrderType orderType);

        @Query("SELECT COUNT(o) FROM QrLabelOrder o WHERE o.hcf.id = :hcfId AND o.status = 'PENDING'")
        long countPendingOrdersByHcf(@Param("hcfId") UUID hcfId);
}
