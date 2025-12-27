package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.dto.*;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for CBWTF Admin HCF Management operations.
 * All methods are tenant-scoped via facilityId parameter.
 */
@Service
public class CbwtfHcfService {

    private static final Logger log = LoggerFactory.getLogger(CbwtfHcfService.class);

    private final HcfRepository hcfRepository;
    private final AgreementRepository agreementRepository;
    private final AgreementBillingConfigRepository billingConfigRepository;
    private final FacilityRepository facilityRepository;
    private final AuditLogService auditLogService;
    private final AgreementValidationService agreementValidationService;
    private final AgreementNumberGeneratorService agreementNumberGenerator;

    public CbwtfHcfService(
            HcfRepository hcfRepository,
            AgreementRepository agreementRepository,
            AgreementBillingConfigRepository billingConfigRepository,
            FacilityRepository facilityRepository,
            AuditLogService auditLogService,
            AgreementValidationService agreementValidationService,
            AgreementNumberGeneratorService agreementNumberGenerator) {
        this.hcfRepository = hcfRepository;
        this.agreementRepository = agreementRepository;
        this.billingConfigRepository = billingConfigRepository;
        this.facilityRepository = facilityRepository;
        this.auditLogService = auditLogService;
        this.agreementValidationService = agreementValidationService;
        this.agreementNumberGenerator = agreementNumberGenerator;
    }

    /**
     * List HCFs with active agreements for the facility.
     */
    @Transactional(readOnly = true)
    public List<HcfListItemDTO> listByFacility(UUID facilityId) {
        // Return latest agreement for each HCF (including Expired/Terminated)
        List<Agreement> agreements = agreementRepository.findLatestAgreementsByFacilityId(facilityId);

        return agreements.stream()
                .map(agreement -> {
                    Hcf hcf = agreement.getHcf();
                    // TODO: Get last pickup timestamp from attendance/pickup events
                    Instant lastPickupAt = null;
                    return HcfListItemDTO.from(hcf, agreement, lastPickupAt);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get HCF detail with agreement, billing config, and operational summary.
     */
    @Transactional(readOnly = true)
    public HcfDetailDTO getHcfDetail(UUID hcfId, UUID facilityId) {
        log.info("Fetching HCF detail for HCF: {} and Facility: {}", hcfId, facilityId);

        List<Agreement> agreements = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId);
        log.info("Found {} agreements for HCF {} and Facility {}", agreements.size(), hcfId, facilityId);

        // Verify HCF belongs to this facility via agreement (any status)
        Agreement agreement = agreements.stream()
                .findFirst()
                .orElseThrow(() -> {
                    log.error("No agreement found for HCF {} and Facility {}", hcfId, facilityId);
                    return new IllegalArgumentException("HCF not found or not associated with this facility");
                });

        Hcf hcf = agreement.getHcf();

        // Get active billing config
        AgreementBillingConfig billingConfig = billingConfigRepository
                .findActiveByAgreementId(agreement.getId())
                .orElse(null);

        // Build operational summary (TODO: implement with actual data)
        HcfDetailDTO.OperationalSummary summary = new HcfDetailDTO.OperationalSummary();
        summary.setTotalPickups(0);
        summary.setTotalAttendanceMarks(0);

        return HcfDetailDTO.from(hcf, agreement, billingConfig, summary);
    }

    /**
     * Update HCF profile fields.
     * Audit logged.
     */
    @Transactional
    public HcfDetailDTO updateHcf(UUID hcfId, UUID facilityId, UpdateHcfRequest request) {
        // Verify access (allow updating profile even if agreement is not active)
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or not associated with this facility"));

        Hcf hcf = agreement.getHcf();

        // Update fields if provided
        if (request.getName() != null) {
            hcf.setName(request.getName());
        }
        if (request.getContactEmail() != null) {
            hcf.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            hcf.setContactPhone(request.getContactPhone());
        }
        if (request.getAddress() != null) {
            hcf.setAddress(request.getAddress());
        }
        if (request.getNumberOfBeds() != null) {
            hcf.setNumberOfBeds(request.getNumberOfBeds());
        }
        if (request.getDoctorName() != null) {
            hcf.setDoctorName(request.getDoctorName());
        }
        if (request.getGstNo() != null) {
            hcf.setGstNo(request.getGstNo());
        }
        if (request.getPanNo() != null) {
            hcf.setPanNo(request.getPanNo());
        }
        if (request.getAadharNo() != null) {
            hcf.setAadharNo(request.getAadharNo());
        }
        if (request.getPcbAuthorizationNo() != null) {
            hcf.setPcbAuthorizationNo(request.getPcbAuthorizationNo());
        }
        if (request.getMonthlyCharges() != null) {
            hcf.setMonthlyCharges(request.getMonthlyCharges());
        }
        if (request.getBedded() != null) {
            hcf.setBedded(request.getBedded());
        }
        if (request.getOtherNotes() != null) {
            hcf.setOtherNotes(request.getOtherNotes());
        }

        hcf.setUpdatedAt(Instant.now());
        hcfRepository.save(hcf);

        // Audit log
        auditLogService.log("HCF", hcfId, "HCF_UPDATED", null, "Profile updated");
        log.info("HCF {} updated by facility {}", hcfId, facilityId);

        return getHcfDetail(hcfId, facilityId);
    }

    /**
     * Update HCF registered location.
     * Only allowed if agreement is ACTIVE.
     * Audit logged.
     */
    @Transactional
    public HcfDetailDTO updateLocation(UUID hcfId, UUID facilityId, UpdateLocationRequest request) {
        // Verify access and active agreement
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or not associated with this facility"));

        if (!agreement.isActive()) {
            throw new IllegalStateException("Cannot update location: Agreement is not active");
        }

        Hcf hcf = agreement.getHcf();
        Double oldLat = hcf.getGpsLat();
        Double oldLon = hcf.getGpsLon();

        hcf.setGpsLat(request.getLatitude());
        hcf.setGpsLon(request.getLongitude());
        hcf.setUpdatedAt(Instant.now());
        hcfRepository.save(hcf);

        // Audit log with old and new values
        String details = String.format("Location changed from (%.6f, %.6f) to (%.6f, %.6f)",
                oldLat, oldLon, request.getLatitude(), request.getLongitude());
        auditLogService.log("HCF", hcfId, "HCF_LOCATION_CHANGED", null, details);
        log.info("HCF {} location updated: {}", hcfId, details);

        return getHcfDetail(hcfId, facilityId);
    }

    /**
     * Deactivate HCF by expiring/terminating its agreement.
     * Audit logged.
     */
    @Transactional
    public void deactivate(UUID hcfId, UUID facilityId, DeactivateHcfRequest request) {
        // Verify access
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or not associated with this facility"));

        if (!agreement.isActive()) {
            throw new IllegalStateException("Agreement is already not active");
        }

        // Set new status
        String newStatus = request.isTerminate()
                ? Agreement.Status.TERMINATED.name()
                : Agreement.Status.EXPIRED.name();

        agreement.setStatus(newStatus);
        agreement.setTerminationReason(request.getReason());
        agreement.setTerminatedAt(Instant.now());
        agreement.setUpdatedAt(Instant.now());
        agreementRepository.save(agreement);

        // Update HCF status
        Hcf hcf = agreement.getHcf();
        hcf.setStatus("INACTIVE");
        hcf.setUpdatedAt(Instant.now());
        hcfRepository.save(hcf);

        // Audit log
        auditLogService.log("HCF", hcfId, "HCF_DEACTIVATED", null,
                String.format("Agreement %s %s: %s",
                        agreement.getAgreementNumber(), newStatus.toLowerCase(), request.getReason()));
        log.info("HCF {} deactivated, agreement {} set to {}", hcfId, agreement.getAgreementNumber(), newStatus);
    }

    /**
     * List pending HCF registrations (from Android app).
     */
    @Transactional(readOnly = true)
    public List<HcfListItemDTO> listPending(UUID facilityId) {
        // Get pending HCFs - these are HCFs with PENDING_APPROVAL status
        // that were registered by staff belonging to this facility
        // For now, return all pending HCFs (TODO: filter by facility's staff)
        List<Hcf> pendingHcfs = hcfRepository.findByStatus("PENDING_APPROVAL", null).getContent();

        return pendingHcfs.stream()
                .map(hcf -> HcfListItemDTO.from(hcf, null, null))
                .collect(Collectors.toList());
    }

    /**
     * Approve a pending HCF registration.
     * Creates agreement + default billing config.
     */
    @Transactional
    public HcfDetailDTO approveHcf(UUID hcfId, UUID facilityId, HcfApprovalRequest request) {
        Hcf hcf = hcfRepository.findById(hcfId)
                .orElseThrow(() -> new IllegalArgumentException("HCF not found"));

        if (!"PENDING_APPROVAL".equals(hcf.getStatus())) {
            throw new IllegalStateException("HCF is not pending approval");
        }

        // Check eligibility for new agreement
        agreementValidationService.assertCanCreateAgreement(hcfId);

        // Get the facility
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));

        // Create agreement
        Agreement agreement = new Agreement();
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setAgreementNumber(agreementNumberGenerator.generateNextAgreementNumber(facility));
        agreement.setStatus(Agreement.Status.ACTIVE.name());
        agreement.setDuesStatus(Agreement.DuesStatus.CLEAR.name());
        agreement.setStartDate(LocalDate.now());
        agreement.setEndDate(LocalDate.now().plusYears(1));
        agreement.setPerBedPerDayRate(request.getPerBedPerDayRate());
        agreement.setCreatedAt(Instant.now());
        agreementRepository.save(agreement);

        // Update HCF status
        hcf.setStatus("ACTIVE");
        hcf.setUpdatedAt(Instant.now());
        hcfRepository.save(hcf);

        // Create default billing config
        AgreementBillingConfig config = new AgreementBillingConfig();
        config.setAgreement(agreement);
        config.setBaseGramsPerBedPerDay(270);
        config.setBaseRatePerBedPerDay(request.getPerBedPerDayRate());
        config.setExcessRatePerKg(request.getExcessRatePerKg());
        config.setEffectiveFrom(LocalDate.now());
        // createdBy would be set from security context
        config.setCreatedBy(UUID.randomUUID()); // TODO: Get from security context
        billingConfigRepository.save(config);

        // Audit log
        auditLogService.log("HCF", hcfId, "HCF_APPROVED", null,
                "Agreement " + agreement.getAgreementNumber() + " created");
        log.info("HCF {} approved, agreement {} created", hcfId, agreement.getAgreementNumber());

        return getHcfDetail(hcfId, facilityId);
    }

    /**
     * Reject a pending HCF registration.
     */
    @Transactional
    public void rejectHcf(UUID hcfId, UUID facilityId, HcfRejectionRequest request) {
        Hcf hcf = hcfRepository.findById(hcfId)
                .orElseThrow(() -> new IllegalArgumentException("HCF not found"));

        if (!"PENDING_APPROVAL".equals(hcf.getStatus())) {
            throw new IllegalStateException("HCF is not pending approval");
        }

        hcf.setStatus("REJECTED");
        hcf.setOtherNotes(request.getReason());
        hcf.setUpdatedAt(Instant.now());
        hcfRepository.save(hcf);

        // Audit log
        auditLogService.log("HCF", hcfId, "HCF_REJECTED", null, "Reason: " + request.getReason());
        log.info("HCF {} rejected: {}", hcfId, request.getReason());
    }
}
