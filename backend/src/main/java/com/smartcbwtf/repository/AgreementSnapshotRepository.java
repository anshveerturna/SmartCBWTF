package com.smartcbwtf.repository;

import com.smartcbwtf.domain.AgreementSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgreementSnapshotRepository extends JpaRepository<AgreementSnapshot, UUID> {

    List<AgreementSnapshot> findByAgreementIdOrderBySnapshotAtDesc(UUID agreementId);

    List<AgreementSnapshot> findByAgreementIdAndSnapshotReason(UUID agreementId, String reason);
}
