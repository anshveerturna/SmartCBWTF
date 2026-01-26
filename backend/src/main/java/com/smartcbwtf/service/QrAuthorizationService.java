package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * QR Authorization Service - Manages QR lifecycle for waste movement
 * authorization.
 * 
 * Key responsibilities:
 * - Generate signed QR codes bound to agreements
 * - Validate QR codes during pickup and verification
 * - Manage QR lifecycle (ACTIVE → USED → VERIFIED)
 * - Handle expiration and agreement status changes
 * - Enforce verification SLA
 */
@Service
public class QrAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(QrAuthorizationService.class);

    // Verification SLA: USED → VERIFIED must occur within this many hours
    public static final int VERIFICATION_SLA_HOURS = 24;

    private final QrAuthorizationRepository qrRepository;
    private final AgreementRepository agreementRepository;
    private final HcfRepository hcfRepository;
    private final FacilityRepository facilityRepository;
    private final BagLabelRepository bagLabelRepository;
    private final QrSigningService signingService;
    private final AuditLogService auditLogService;
    
    // Serial number counter (for generating unique serial numbers)
    private static final AtomicLong serialCounter = new AtomicLong(System.currentTimeMillis() % 100000);

    public QrAuthorizationService(
            QrAuthorizationRepository qrRepository,
            AgreementRepository agreementRepository,
            HcfRepository hcfRepository,
            FacilityRepository facilityRepository,
            BagLabelRepository bagLabelRepository,
            QrSigningService signingService,
            AuditLogService auditLogService) {
        this.qrRepository = qrRepository;
        this.agreementRepository = agreementRepository;
        this.hcfRepository = hcfRepository;
        this.facilityRepository = facilityRepository;
        this.bagLabelRepository = bagLabelRepository;
        this.signingService = signingService;
        this.auditLogService = auditLogService;
    }

    // ============= QR Generation =============

    /**
     * Generate a new QR code for waste pickup authorization.
     * 
     * Validations:
     * - Agreement must be ACTIVE
     * - Validity period must be within agreement period
     * - No overlapping QR for same agreement + category + period
     */
    @Transactional
    public QrGenerateResult generateQr(
            UUID hcfId,
            String wasteCategory,
            Instant validFrom,
            Instant validTo,
            UUID createdBy) {

        // Validate HCF exists and belongs to facility
        Hcf hcf = hcfRepository.findById(hcfId)
                .orElseThrow(() -> new IllegalArgumentException("HCF not found"));

        // Get active agreement for HCF
        Agreement agreement = agreementRepository.findByHcfIdAndStatus(hcfId, "ACTIVE")
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No active agreement for this HCF"));

        // Get facility ID - for CBWTF context use TenantContext, for HCF context derive
        // from agreement
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            // HCF Admin context - get facility from agreement
            facilityId = agreement.getFacility().getId();
        } else {
            // CBWTF Admin context - verify agreement belongs to this facility
            if (!agreement.getFacility().getId().equals(facilityId)) {
                throw new SecurityException("Agreement does not belong to this facility");
            }
        }

        // Validate category
        QrAuthorization.WasteCategory.valueOf(wasteCategory); // Throws if invalid

        // Validate validity period is within agreement period
        if (validFrom.isBefore(agreement.getStartDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC))) {
            throw new IllegalArgumentException("QR validity cannot start before agreement start date");
        }
        if (agreement.getEndDate() != null &&
                validTo.isAfter(
                        agreement.getEndDate().plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC))) {
            throw new IllegalArgumentException("QR validity cannot extend beyond agreement end date");
        }

        // Note: Overlapping QR check removed to allow multiple QR labels for same
        // category/period
        // Each QR label is unique and used independently for waste collection

        // Generate ID first (before creating entity)
        UUID qrId = UUID.randomUUID();

        // Generate signed payload
        QrSigningService.SignedPayload signedPayload = signingService.generateSignedPayload(
                qrId,
                agreement.getId(),
                hcfId,
                facilityId,
                wasteCategory,
                validFrom,
                validTo);

        // Create QR entity with all required fields
        QrAuthorization qr = new QrAuthorization();
        qr.setId(qrId);
        qr.setAgreement(agreement);
        qr.setHcf(hcf);
        qr.setFacility(facilityRepository.findById(facilityId).orElseThrow());
        qr.setWasteCategory(wasteCategory);
        qr.setValidFrom(validFrom);
        qr.setValidTo(validTo);
        qr.setStatusEnum(QrAuthorization.Status.ACTIVE);
        qr.setCreatedBy(createdBy);
        qr.setCreatedAt(Instant.now());
        qr.setQrPayload(signedPayload.json());
        qr.setChecksum(signedPayload.checksum());

        // Save once with all fields populated
        qrRepository.save(qr);

        // *** AUTO-CREATE BAG LABEL ***
        // This creates the BagLabel record that BagEventService.sync() looks for
        BagLabel bagLabel = new BagLabel();
        bagLabel.setHcf(hcf);
        bagLabel.setFacility(facilityRepository.findById(facilityId).orElseThrow());
        bagLabel.setCategory(wasteCategory);
        bagLabel.setSerialNo(generateSerialNo(wasteCategory));
        bagLabel.setQrCode(signedPayload.json());  // Full QR payload JSON
        bagLabel.setStatus("ISSUED");
        bagLabel.setIssuedAt(Instant.now());
        bagLabelRepository.save(bagLabel);
        
        log.info("BagLabel auto-created: {} for QR {} category {}", bagLabel.getId(), qrId, wasteCategory);

        // Audit log
        auditLogService.log("QR", qr.getId(), "QR_CREATED", createdBy,
                String.format("{\"hcfId\":\"%s\",\"category\":\"%s\",\"validFrom\":\"%s\",\"validTo\":\"%s\"}",
                        hcfId, wasteCategory, validFrom, validTo));

        log.info("QR generated: {} for HCF {} category {}", qr.getId(), hcfId, wasteCategory);

        return new QrGenerateResult(qr.getId(), signedPayload.json());
    }

    // ============= QR Validation & Lifecycle =============

    /**
     * Validate QR for pickup and mark as USED.
     * Called when driver scans QR at HCF.
     */
    @Transactional
    public QrValidationResult validateAndMarkUsed(String qrPayloadJson, UUID pickupEventId, UUID userId) {
        // Verify checksum first
        if (!signingService.verifyChecksum(qrPayloadJson)) {
            auditLogService.log("QR", null, "QR_SCAN_BLOCKED", userId,
                    "{\"reason\":\"CHECKSUM_INVALID\"}");
            return QrValidationResult.invalid("QR checksum verification failed - possible tampering");
        }

        // Extract QR ID
        UUID qrId = signingService.extractQrId(qrPayloadJson);
        if (qrId == null) {
            return QrValidationResult.invalid("Invalid QR payload format");
        }

        UUID facilityId = TenantContext.getTenantId();

        // Find QR with tenant check
        Optional<QrAuthorization> optQr = qrRepository.findByIdAndFacilityId(qrId, facilityId);
        if (optQr.isEmpty()) {
            auditLogService.log("QR", qrId, "QR_SCAN_BLOCKED", userId,
                    "{\"reason\":\"CROSS_TENANT_ATTEMPT\"}");
            return QrValidationResult.invalid("QR not found or does not belong to this facility");
        }

        QrAuthorization qr = optQr.get();

        // Check status
        if (!qr.isActive()) {
            return QrValidationResult.invalid("QR is not active. Current status: " + qr.getStatus());
        }

        // Check validity period
        Instant now = Instant.now();
        if (now.isBefore(qr.getValidFrom())) {
            return QrValidationResult.invalid("QR is not yet valid. Valid from: " + qr.getValidFrom());
        }
        if (now.isAfter(qr.getValidTo())) {
            return QrValidationResult.invalid("QR has expired. Valid to: " + qr.getValidTo());
        }

        // Check agreement status
        if (!qr.getAgreement().isActive()) {
            return QrValidationResult.invalid("Agreement is not active. Cannot use QR.");
        }

        // Mark as USED
        qr.setStatusEnum(QrAuthorization.Status.USED);
        qr.setUsedAt(now);
        qr.setPickupEventId(pickupEventId);
        qrRepository.save(qr);

        // Audit log
        auditLogService.log("QR", qrId, "QR_USED_FOR_PICKUP", userId,
                String.format("{\"pickupEventId\":\"%s\"}", pickupEventId));

        log.info("QR {} marked as USED for pickup event {}", qrId, pickupEventId);

        return QrValidationResult.valid(qr);
    }

    /**
     * Verify QR at CBWTF and mark as VERIFIED.
     * Called when operator scans QR at facility.
     */
    @Transactional
    public QrValidationResult validateAndMarkVerified(String qrPayloadJson, UUID userId) {
        // Verify checksum
        if (!signingService.verifyChecksum(qrPayloadJson)) {
            auditLogService.log("QR", null, "QR_SCAN_BLOCKED", userId,
                    "{\"reason\":\"CHECKSUM_INVALID\"}");
            return QrValidationResult.invalid("QR checksum verification failed");
        }

        UUID qrId = signingService.extractQrId(qrPayloadJson);
        if (qrId == null) {
            return QrValidationResult.invalid("Invalid QR payload format");
        }

        UUID facilityId = TenantContext.getTenantId();

        Optional<QrAuthorization> optQr = qrRepository.findByIdAndFacilityId(qrId, facilityId);
        if (optQr.isEmpty()) {
            auditLogService.log("QR", qrId, "QR_SCAN_BLOCKED", userId,
                    "{\"reason\":\"CROSS_TENANT_ATTEMPT\"}");
            return QrValidationResult.invalid("QR not found or does not belong to this facility");
        }

        QrAuthorization qr = optQr.get();

        // Must be USED to verify
        if (!QrAuthorization.Status.USED.name().equals(qr.getStatus())) {
            return QrValidationResult.invalid("QR must be in USED status to verify. Current: " + qr.getStatus());
        }

        // Check agreement still active
        if (!qr.getAgreement().isActive()) {
            return QrValidationResult.invalid("Agreement is no longer active");
        }

        // Mark as VERIFIED
        qr.setStatusEnum(QrAuthorization.Status.VERIFIED);
        qr.setVerifiedAt(Instant.now());
        qrRepository.save(qr);

        // Audit log
        auditLogService.log("QR", qrId, "QR_VERIFIED_AT_CBWTF", userId, "{}");

        log.info("QR {} verified at CBWTF", qrId);

        return QrValidationResult.valid(qr);
    }

    // ============= Admin Operations =============

    /**
     * Manually revoke a QR code.
     */
    @Transactional
    public void revokeQr(UUID qrId, UUID userId, String reason) {
        UUID facilityId = TenantContext.getTenantId();

        QrAuthorization qr = qrRepository.findByIdAndFacilityId(qrId, facilityId)
                .orElseThrow(() -> new IllegalArgumentException("QR not found"));

        if (qr.getStatusEnum() == QrAuthorization.Status.VERIFIED) {
            throw new IllegalStateException("Cannot revoke verified QR");
        }

        qr.setStatusEnum(QrAuthorization.Status.REVOKED);
        qrRepository.save(qr);

        auditLogService.log("QR", qrId, "QR_REVOKED_MANUALLY", userId,
                String.format("{\"reason\":\"%s\"}", reason != null ? reason : ""));

        log.info("QR {} revoked by user {}", qrId, userId);
    }

    /**
     * Get QR details.
     */
    public Optional<QrAuthorization> getQr(UUID qrId) {
        UUID facilityId = TenantContext.getTenantId();
        return qrRepository.findByIdAndFacilityId(qrId, facilityId);
    }

    /**
     * List QRs for facility with optional filters.
     */
    public List<QrAuthorization> listQrs(UUID hcfId, String status) {
        UUID facilityId = TenantContext.getTenantId();

        // For HCF users, facilityId will be null - query by hcfId instead
        if (facilityId == null && hcfId != null) {
            // HCF user context - query by hcfId only
            if (status != null) {
                return qrRepository.findByHcfIdAndStatusOrderByCreatedAtDesc(hcfId, status);
            } else {
                return qrRepository.findByHcfIdOrderByCreatedAtDesc(hcfId);
            }
        }

        // CBWTF context - query by facilityId
        if (hcfId != null && status != null) {
            return qrRepository.findByFacilityIdAndHcfIdOrderByCreatedAtDesc(facilityId, hcfId)
                    .stream()
                    .filter(qr -> status.equals(qr.getStatus()))
                    .toList();
        } else if (hcfId != null) {
            return qrRepository.findByFacilityIdAndHcfIdOrderByCreatedAtDesc(facilityId, hcfId);
        } else if (status != null) {
            return qrRepository.findByFacilityIdAndStatusOrderByCreatedAtDesc(facilityId, status);
        } else {
            return qrRepository.findByFacilityIdOrderByCreatedAtDesc(facilityId);
        }
    }
    // ============= Scheduled Jobs =============

    /**
     * Expire QRs that have passed their validity period.
     * Runs daily at 1 AM.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expireQrsByTime() {
        List<QrAuthorization> expiredQrs = qrRepository.findExpiredActiveQrs(Instant.now());

        for (QrAuthorization qr : expiredQrs) {
            qr.setStatusEnum(QrAuthorization.Status.EXPIRED);
            qrRepository.save(qr);

            auditLogService.log("QR", qr.getId(), "QR_EXPIRED_BY_TIME", null, "{}");
        }

        if (!expiredQrs.isEmpty()) {
            log.info("Expired {} QRs by time", expiredQrs.size());
        }
    }

    /**
     * Check for USED QRs that have exceeded verification SLA.
     * Runs every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkVerificationSla() {
        Instant threshold = Instant.now().minus(VERIFICATION_SLA_HOURS, ChronoUnit.HOURS);
        List<QrAuthorization> breachedQrs = qrRepository.findUsedQrsBeyondSla(threshold);

        for (QrAuthorization qr : breachedQrs) {
            auditLogService.log("QR", qr.getId(), "QR_VERIFICATION_SLA_BREACHED", null,
                    String.format("{\"usedAt\":\"%s\",\"slaHours\":%d}", qr.getUsedAt(), VERIFICATION_SLA_HOURS));
            // TODO: Trigger alert
        }

        if (!breachedQrs.isEmpty()) {
            log.warn("Found {} QRs exceeding verification SLA of {} hours",
                    breachedQrs.size(), VERIFICATION_SLA_HOURS);
        }
    }

    /**
     * Block/revoke QRs when agreement status changes.
     * Called by agreement lifecycle events.
     */
    @Transactional
    public void handleAgreementStatusChange(UUID agreementId, Agreement.Status newStatus) {
        String qrNewStatus;
        String auditEvent;

        switch (newStatus) {
            case EXPIRED, TERMINATED -> {
                qrNewStatus = QrAuthorization.Status.REVOKED.name();
                auditEvent = "QR_REVOKED_BY_AGREEMENT";
            }
            case DISPUTED -> {
                qrNewStatus = QrAuthorization.Status.BLOCKED.name();
                auditEvent = "QR_REVOKED_BY_AGREEMENT";
            }
            default -> {
                return; // ACTIVE - no action needed
            }
        }

        int updated = qrRepository.updateStatusByAgreement(agreementId, qrNewStatus);

        if (updated > 0) {
            auditLogService.log("QR", null, auditEvent, null,
                    String.format("{\"agreementId\":\"%s\",\"newStatus\":\"%s\",\"count\":%d}",
                            agreementId, newStatus, updated));
            log.info("Updated {} QRs to {} due to agreement {} status change to {}",
                    updated, qrNewStatus, agreementId, newStatus);
        }
    }

    // ============= Helper Methods =============
    
    /**
     * Generate a unique serial number for bag labels.
     * Format: CAT-YYYYMMDD-NNNNN (e.g., YEL-20260126-00042)
     */
    private String generateSerialNo(String wasteCategory) {
        String catPrefix = switch (wasteCategory.toUpperCase()) {
            case "YELLOW" -> "YEL";
            case "RED" -> "RED";
            case "WHITE" -> "WHT";
            case "BLUE" -> "BLU";
            default -> "OTH";
        };
        String dateStr = LocalDate.now().toString().replace("-", "");
        long seq = serialCounter.incrementAndGet() % 100000;
        return String.format("%s-%s-%05d", catPrefix, dateStr, seq);
    }

    // ============= Result Records =============

    public record QrGenerateResult(UUID qrId, String qrPayloadJson) {
    }

    public record QrValidationResult(boolean valid, String message, QrAuthorization qr) {
        public static QrValidationResult valid(QrAuthorization qr) {
            return new QrValidationResult(true, "Valid", qr);
        }

        public static QrValidationResult invalid(String message) {
            return new QrValidationResult(false, message, null);
        }
    }
}
