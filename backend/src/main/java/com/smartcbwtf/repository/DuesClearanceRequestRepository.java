package com.smartcbwtf.repository;

import com.smartcbwtf.domain.DuesClearanceRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DuesClearanceRequestRepository extends JpaRepository<DuesClearanceRequest, UUID> {

        @Query("SELECT d FROM DuesClearanceRequest d WHERE d.id = :id AND d.facility.id = :facilityId")
        Optional<DuesClearanceRequest> findByIdAndFacilityId(@Param("id") UUID id,
                        @Param("facilityId") UUID facilityId);

        @Query("SELECT d FROM DuesClearanceRequest d WHERE d.facility.id = :facilityId AND d.id IN :ids")
        List<DuesClearanceRequest> findByFacilityIdAndIdIn(@Param("facilityId") UUID facilityId,
                        @Param("ids") List<UUID> ids);

        List<DuesClearanceRequest> findByHcfIdOrderByRequestedAtDesc(UUID hcfId);

        List<DuesClearanceRequest> findByHcfIdOrderByRequestedAtDesc(UUID hcfId, Pageable pageable);

        List<DuesClearanceRequest> findByHcfIdAndFacilityIdOrderByRequestedAtDesc(UUID hcfId, UUID facilityId,
                        Pageable pageable);

        List<DuesClearanceRequest> findByFacilityIdOrderByRequestedAtDesc(UUID facilityId);

        List<DuesClearanceRequest> findByFacilityIdOrderByRequestedAtDesc(UUID facilityId, Pageable pageable);

        List<DuesClearanceRequest> findByFacilityIdAndManagementStatusOrderByRequestedAtDesc(
                        UUID facilityId, String managementStatus);

        List<DuesClearanceRequest> findByFacilityIdAndManagementStatusOrderByRequestedAtDesc(
                        UUID facilityId, String managementStatus, Pageable pageable);

        List<DuesClearanceRequest> findByManagementStatusOrderByRequestedAtDesc(String managementStatus);

        long countByFacilityId(UUID facilityId);

        long countByFacilityIdAndManagementStatus(UUID facilityId, String managementStatus);

        Optional<DuesClearanceRequest> findTopByHcfIdAndManagementStatusOrderByRequestedAtDesc(
                        UUID hcfId, String managementStatus);

        Optional<DuesClearanceRequest> findTopByHcfIdAndFacilityIdAndManagementStatusOrderByRequestedAtDesc(
                        UUID hcfId, UUID facilityId, String managementStatus);

        Optional<DuesClearanceRequest> findTopByHcfIdOrderByRequestedAtDesc(UUID hcfId);

        Optional<DuesClearanceRequest> findTopByHcfIdAndFacilityIdOrderByRequestedAtDesc(UUID hcfId, UUID facilityId);

        Optional<DuesClearanceRequest> findTopByHcfIdAndRequestMonthAndRequestYearOrderByRequestedAtDesc(
                        UUID hcfId, Integer requestMonth, Integer requestYear);

        Optional<DuesClearanceRequest> findTopByHcfIdAndFacilityIdAndRequestMonthAndRequestYearOrderByRequestedAtDesc(
                        UUID hcfId, UUID facilityId, Integer requestMonth, Integer requestYear);

        boolean existsByHcfIdAndManagementStatusIn(UUID hcfId, List<String> statuses);

        boolean existsByHcfIdAndFacilityIdAndManagementStatusIn(UUID hcfId, UUID facilityId, List<String> statuses);

        boolean existsByHcfIdAndRequestMonthAndRequestYearAndManagementStatus(
                        UUID hcfId, Integer requestMonth, Integer requestYear, String managementStatus);

        boolean existsByHcfIdAndFacilityIdAndRequestMonthAndRequestYearAndManagementStatus(
                        UUID hcfId, UUID facilityId, Integer requestMonth, Integer requestYear,
                        String managementStatus);

        boolean existsByHcfIdAndRequestMonthAndRequestYearAndManagementStatusIn(
                        UUID hcfId, Integer requestMonth, Integer requestYear, List<String> managementStatuses);

        boolean existsByHcfIdAndFacilityIdAndRequestMonthAndRequestYearAndManagementStatusIn(
                        UUID hcfId, UUID facilityId, Integer requestMonth, Integer requestYear,
                        List<String> managementStatuses);

        List<DuesClearanceRequest> findByHcfIdAndManagementStatusIn(UUID hcfId, List<String> statuses);

        List<DuesClearanceRequest> findByHcfIdAndFacilityIdAndManagementStatusIn(UUID hcfId, UUID facilityId,
                        List<String> statuses);
}
