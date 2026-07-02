package com.smartcbwtf.repository;

import com.smartcbwtf.domain.AgreementCorrectionRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AgreementCorrectionRequestRepository extends JpaRepository<AgreementCorrectionRequest, UUID> {

    List<AgreementCorrectionRequest> findByFacilityIdOrderByRequestedAtDesc(UUID facilityId);

    List<AgreementCorrectionRequest> findByFacilityIdAndStatusOrderByRequestedAtDesc(UUID facilityId, AgreementCorrectionRequest.Status status);

    List<AgreementCorrectionRequest> findByFacilityIdAndStatusOrderByRequestedAtDesc(
            UUID facilityId, AgreementCorrectionRequest.Status status, Pageable pageable);

    List<AgreementCorrectionRequest> findByStatusOrderByRequestedAtDesc(AgreementCorrectionRequest.Status status);

    List<AgreementCorrectionRequest> findByAgreementIdOrderByRequestedAtDesc(UUID agreementId);

    @Query("SELECT r FROM AgreementCorrectionRequest r WHERE r.facility.id = :facilityId AND r.status = 'PENDING'")
    List<AgreementCorrectionRequest> findPendingByFacilityId(UUID facilityId);
}
