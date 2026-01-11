package com.smartcbwtf.repository;

import com.smartcbwtf.domain.DuesClearanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DuesClearanceRequestRepository extends JpaRepository<DuesClearanceRequest, UUID> {

        List<DuesClearanceRequest> findByHcfIdOrderByRequestedAtDesc(UUID hcfId);

        List<DuesClearanceRequest> findByFacilityIdOrderByRequestedAtDesc(UUID facilityId);

        List<DuesClearanceRequest> findByFacilityIdAndManagementStatusOrderByRequestedAtDesc(
                        UUID facilityId, String managementStatus);

        List<DuesClearanceRequest> findByManagementStatusOrderByRequestedAtDesc(String managementStatus);

        Optional<DuesClearanceRequest> findTopByHcfIdAndManagementStatusOrderByRequestedAtDesc(
                        UUID hcfId, String managementStatus);

        Optional<DuesClearanceRequest> findTopByHcfIdOrderByRequestedAtDesc(UUID hcfId);

        boolean existsByHcfIdAndManagementStatusIn(UUID hcfId, List<String> statuses);

        List<DuesClearanceRequest> findByHcfIdAndManagementStatusIn(UUID hcfId, List<String> statuses);
}
