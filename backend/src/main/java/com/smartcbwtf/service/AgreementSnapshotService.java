package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.AgreementSnapshot;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.AgreementSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for creating agreement snapshots.
 * Snapshots preserve historical truth for invoices, reports, and disputes.
 */
@Service
public class AgreementSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(AgreementSnapshotService.class);

    private final AgreementRepository agreementRepo;
    private final AgreementSnapshotRepository snapshotRepo;

    public AgreementSnapshotService(AgreementRepository agreementRepo,
            AgreementSnapshotRepository snapshotRepo) {
        this.agreementRepo = agreementRepo;
        this.snapshotRepo = snapshotRepo;
    }

    /**
     * Create a snapshot of agreement at current point in time.
     * 
     * @param agreementId The agreement to snapshot
     * @param reason      Why the snapshot is being created
     * @param actorId     Who triggered the snapshot
     * @return The created snapshot
     */
    @Transactional
    public AgreementSnapshot createSnapshot(UUID agreementId,
            AgreementSnapshot.SnapshotReason reason,
            UUID actorId) {
        Agreement agreement = agreementRepo.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found: " + agreementId));

        Hcf hcf = agreement.getHcf();
        Facility facility = agreement.getFacility();

        AgreementSnapshot snapshot = new AgreementSnapshot();

        // Agreement data
        snapshot.setAgreementId(agreementId);
        snapshot.setAgreementNumber(agreement.getAgreementNumber());
        snapshot.setStatus(agreement.getStatus());
        snapshot.setDuesStatus(agreement.getDuesStatus());
        snapshot.setPerBedPerDayRate(agreement.getPerBedPerDayRate());
        snapshot.setTermsText(agreement.getTermsText());

        // HCF data
        snapshot.setHcfId(hcf.getId());
        snapshot.setHcfName(hcf.getName());
        snapshot.setHcfGst(hcf.getGstNo());
        snapshot.setHcfPan(hcf.getPanNo());
        snapshot.setHcfAddress(hcf.getAddress());
        snapshot.setHcfBeds(hcf.getNumberOfBeds());

        // Facility data
        snapshot.setFacilityId(facility.getId());
        snapshot.setFacilityName(facility.getName());

        // Metadata
        snapshot.setSnapshotReasonEnum(reason);
        snapshot.setCreatedBy(actorId);

        AgreementSnapshot saved = snapshotRepo.save(snapshot);
        log.info("Created snapshot {} for agreement {} (reason: {})",
                saved.getId(), agreementId, reason);

        return saved;
    }

    /**
     * Create snapshot for invoice generation.
     */
    @Transactional
    public AgreementSnapshot createInvoiceSnapshot(UUID agreementId, UUID actorId) {
        return createSnapshot(agreementId, AgreementSnapshot.SnapshotReason.INVOICE_GENERATED, actorId);
    }

    /**
     * Create snapshot for CPCB report.
     */
    @Transactional
    public AgreementSnapshot createCpcbReportSnapshot(UUID agreementId, UUID actorId) {
        return createSnapshot(agreementId, AgreementSnapshot.SnapshotReason.CPCB_REPORT, actorId);
    }
}
