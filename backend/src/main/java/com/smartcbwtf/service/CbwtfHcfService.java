package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.dto.*;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    private final HCFIdentityService hcfIdentityService;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BagEventRepository bagEventRepository;
    private final EmailService emailService;

    @Value("${app.portal.url:https://portal.smartcbwtf.com}")
    private String portalUrl;

    public CbwtfHcfService(
            HcfRepository hcfRepository,
            AgreementRepository agreementRepository,
            AgreementBillingConfigRepository billingConfigRepository,
            FacilityRepository facilityRepository,
            AuditLogService auditLogService,
            AgreementValidationService agreementValidationService,
            AgreementNumberGeneratorService agreementNumberGenerator,
            HCFIdentityService hcfIdentityService,
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            BagEventRepository bagEventRepository,
            EmailService emailService) {
        this.hcfRepository = hcfRepository;
        this.agreementRepository = agreementRepository;
        this.billingConfigRepository = billingConfigRepository;
        this.facilityRepository = facilityRepository;
        this.auditLogService = auditLogService;
        this.agreementValidationService = agreementValidationService;
        this.agreementNumberGenerator = agreementNumberGenerator;
        this.hcfIdentityService = hcfIdentityService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bagEventRepository = bagEventRepository;
        this.emailService = emailService;
    }

    /**
     * List HCFs with agreements for the facility.
     * Auto-corrects agreement/HCF status based on current date and validity period.
     */
    @Transactional
    public List<HcfListItemDTO> listByFacility(UUID facilityId) {
        // Return latest agreement for each HCF (including Expired/Terminated)
        List<Agreement> agreements = agreementRepository.findLatestAgreementsByFacilityId(facilityId);
        LocalDate today = LocalDate.now();

        return agreements.stream()
                .map(agreement -> {
                    Hcf hcf = agreement.getHcf();

                    // Auto-correct status based on validity period
                    LocalDate startDate = agreement.getStartDate();
                    LocalDate endDate = agreement.getEndDate();

                    boolean shouldBeActive = startDate != null && endDate != null &&
                            !today.isBefore(startDate) && !today.isAfter(endDate);

                    if (shouldBeActive && !Agreement.Status.ACTIVE.name().equals(agreement.getStatus())) {
                        agreement.setStatus(Agreement.Status.ACTIVE.name());
                        hcf.setStatus("ACTIVE");
                        agreement.setUpdatedAt(Instant.now());
                        hcf.setUpdatedAt(Instant.now());
                        agreementRepository.save(agreement);
                        hcfRepository.save(hcf);
                        log.info("Auto-corrected agreement {} status to ACTIVE in list",
                                agreement.getAgreementNumber());
                    } else if (!shouldBeActive && Agreement.Status.ACTIVE.name().equals(agreement.getStatus())) {
                        agreement.setStatus(Agreement.Status.EXPIRED.name());
                        hcf.setStatus("INACTIVE");
                        agreement.setUpdatedAt(Instant.now());
                        hcf.setUpdatedAt(Instant.now());
                        agreementRepository.save(agreement);
                        hcfRepository.save(hcf);
                        log.info("Auto-corrected agreement {} status to EXPIRED in list",
                                agreement.getAgreementNumber());
                    }

                    // TODO: Get last pickup timestamp from attendance/pickup events
                    Instant lastPickupAt = null;
                    return HcfListItemDTO.from(hcf, agreement, lastPickupAt);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get HCF detail with agreement, billing config, and operational summary.
     * Auto-corrects agreement/HCF status based on current date and validity period.
     */
    @Transactional
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

        // Auto-correct status based on validity period
        LocalDate today = LocalDate.now();
        LocalDate startDate = agreement.getStartDate();
        LocalDate endDate = agreement.getEndDate();

        boolean shouldBeActive = startDate != null && endDate != null &&
                !today.isBefore(startDate) && !today.isAfter(endDate);

        boolean statusCorrected = false;
        if (shouldBeActive && !Agreement.Status.ACTIVE.name().equals(agreement.getStatus())) {
            agreement.setStatus(Agreement.Status.ACTIVE.name());
            hcf.setStatus("ACTIVE");
            statusCorrected = true;
            log.info("Auto-corrected agreement {} status to ACTIVE (within validity period)",
                    agreement.getAgreementNumber());
        } else if (!shouldBeActive && Agreement.Status.ACTIVE.name().equals(agreement.getStatus())) {
            agreement.setStatus(Agreement.Status.EXPIRED.name());
            hcf.setStatus("INACTIVE");
            statusCorrected = true;
            log.info("Auto-corrected agreement {} status to EXPIRED (outside validity period)",
                    agreement.getAgreementNumber());
        }

        if (statusCorrected) {
            agreement.setUpdatedAt(Instant.now());
            hcf.setUpdatedAt(Instant.now());
            agreementRepository.save(agreement);
            hcfRepository.save(hcf);
        }

        // Get active billing config
        AgreementBillingConfig billingConfig = billingConfigRepository
                .findActiveByAgreementId(agreement.getId())
                .orElse(null);

        // Build operational summary with REAL data from bag events
        HcfDetailDTO.OperationalSummary summary = new HcfDetailDTO.OperationalSummary();
        summary.setTotalPickups(bagEventRepository.countPickupDaysByHcfId(hcfId));
        summary.setTotalWasteKg(bagEventRepository.sumTotalWasteByHcfId(hcfId));
        summary.setLastPickupAt(bagEventRepository.findLastPickupTimeByHcfId(hcfId));
        summary.setTotalAttendanceMarks(0); // TODO: implement when attendance tracking is added

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
     * Activate HCF (Re-enable).
     */
    @Transactional
    public void activate(UUID hcfId, UUID facilityId) {
        // Find latest agreement (even if inactive)
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or not associated with this facility"));

        if (agreement.isActive()) {
            throw new IllegalStateException("Agreement is already active");
        }

        // Reactivate
        agreement.setStatus(Agreement.Status.ACTIVE.name());
        agreement.setUpdatedAt(Instant.now());
        agreementRepository.save(agreement);

        // Update HCF status
        Hcf hcf = agreement.getHcf();
        hcf.setStatus("ACTIVE");
        hcf.setUpdatedAt(Instant.now());
        hcfRepository.save(hcf);

        // Audit log
        auditLogService.log("HCF", hcfId, "HCF_ACTIVATED", null, "HCF re-enabled");
        log.info("HCF {} activated by facility {}", hcfId, facilityId);
    }

    /**
     * Update Agreement validity dates.
     */
    @Transactional
    public HcfDetailDTO updateAgreement(UUID hcfId, UUID facilityId, UpdateAgreementRequest request) {
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or not associated with this facility"));

        // Allow updating dates even if inactive, but usually for corrections
        LocalDate oldStart = agreement.getStartDate();
        LocalDate oldEnd = agreement.getEndDate();

        agreement.setStartDate(request.getStartDate());
        agreement.setEndDate(request.getEndDate());

        // Auto-update status based on validity
        LocalDate today = LocalDate.now();
        boolean isExpired = request.getEndDate().isBefore(today);

        if (isExpired) {
            agreement.setStatus(Agreement.Status.EXPIRED.name());
            agreement.getHcf().setStatus("INACTIVE");
        } else {
            // Re-activate if valid
            agreement.setStatus(Agreement.Status.ACTIVE.name());
            agreement.getHcf().setStatus("ACTIVE");
        }

        agreement.setUpdatedAt(Instant.now());
        agreementRepository.save(agreement);
        hcfRepository.save(agreement.getHcf());

        // Audit log
        String details = String.format("Agreement period changed from %s - %s to %s - %s",
                oldStart, oldEnd, request.getStartDate(), request.getEndDate());
        auditLogService.log("HCF", hcfId, "AGREEMENT_UPDATED", null, details);
        log.info("HCF {} agreement updated: {}", hcfId, details);

        return getHcfDetail(hcfId, facilityId);
    }

    /**
     * List pending HCF registrations (from Android app).
     * Only returns HCFs that:
     * 1. Have PENDING_APPROVAL status
     * 2. Do NOT already have an ACTIVE agreement (data consistency check)
     */
    @Transactional(readOnly = true)
    public List<HcfListItemDTO> listPending(UUID facilityId) {
        // Get pending HCFs - these are HCFs with PENDING_APPROVAL status
        // that were registered by staff belonging to this facility
        List<Hcf> pendingHcfs = hcfRepository.findByStatus("PENDING_APPROVAL", null).getContent();

        // Filter out any HCFs that already have an ACTIVE agreement (data inconsistency
        // fix)
        return pendingHcfs.stream()
                .filter(hcf -> {
                    // Check if HCF already has an active agreement
                    boolean hasActiveAgreement = agreementRepository.findActiveByHcfId(hcf.getId()).isPresent();
                    if (hasActiveAgreement) {
                        log.warn(
                                "Data inconsistency: HCF {} has PENDING_APPROVAL status but already has ACTIVE agreement. "
                                        +
                                        "Auto-fixing status to ACTIVE.",
                                hcf.getId());
                        // Auto-fix the inconsistency
                        hcf.setStatus("ACTIVE");
                        hcf.setUpdatedAt(java.time.Instant.now());
                        hcfRepository.save(hcf);
                        return false; // Don't include in pending list
                    }
                    return true;
                })
                .map(hcf -> HcfListItemDTO.from(hcf, null, null))
                .collect(Collectors.toList());
    }

    /**
     * Approve a pending HCF registration.
     * Creates agreement + default billing config.
     */
    @Transactional
    public HcfDetailDTO approveHcf(UUID hcfId, UUID facilityId, CbwtfHcfApprovalRequest request) {
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
        config.setEffectiveFrom(LocalDate.now());
        // createdBy would be set from security context
        config.setCreatedBy(UUID.randomUUID()); // TODO: Get from security context
        billingConfigRepository.save(config);

        // Create HCF portal admin user for 30+ bed HCFs
        String generatedPassword = null;
        if (hcf.isPortalEligible()) {
            generatedPassword = generateRandomPassword();
            AppUser hcfAdmin = new AppUser();
            hcfAdmin.setUsername(agreement.getAgreementNumber());
            hcfAdmin.setPasswordHash(passwordEncoder.encode(generatedPassword));
            hcfAdmin.setRole("HCF_ADMIN");
            hcfAdmin.setHcf(hcf);
            hcfAdmin.setFullName(hcf.getName() + " Admin");
            hcfAdmin.setEmail(hcf.getContactEmail());
            hcfAdmin.setActive(true);
            hcfAdmin.setCreatedAt(Instant.now());
            hcfAdmin.setUpdatedAt(Instant.now());
            userRepository.save(hcfAdmin);
            log.info("Created HCF_ADMIN user on approval: username={}", agreement.getAgreementNumber());

            // Send credentials email
            sendCredentialsEmail(hcf.getName(), hcf.getContactEmail(), agreement.getAgreementNumber(),
                    generatedPassword);
        }

        // Send HCF approval email (for all HCFs, regardless of portal eligibility)
        sendHcfApprovalEmail(hcf, agreement);

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

    /**
     * Renew an expired agreement by creating a NEW agreement.
     * Old agreement remains immutable.
     * 
     * Rules:
     * - Only allowed if last agreement is EXPIRED
     * - Dues must be CLEAR
     * - Creates new agreement with new number
     * - Version is incremented
     */
    @Transactional
    public HcfDetailDTO renewAgreement(UUID hcfId, UUID facilityId, RenewAgreementRequest request) {
        // Get latest agreement (even if expired)
        Agreement oldAgreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or not associated with this facility"));

        // Validate: must be EXPIRED
        if (!Agreement.Status.EXPIRED.name().equals(oldAgreement.getStatus())) {
            throw new IllegalStateException("Cannot renew: Agreement is not EXPIRED");
        }

        // Validate: dues must be CLEAR
        if (!oldAgreement.isDuesClear()) {
            throw new IllegalStateException("Cannot renew: Outstanding dues must be cleared first");
        }

        Hcf hcf = oldAgreement.getHcf();
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));

        // Create NEW agreement
        Agreement newAgreement = new Agreement();
        newAgreement.setHcf(hcf);
        newAgreement.setFacility(facility);
        newAgreement.setAgreementNumber(agreementNumberGenerator.generateNextAgreementNumber(facility));
        newAgreement.setStatus(Agreement.Status.ACTIVE.name());
        newAgreement.setDuesStatus(Agreement.DuesStatus.CLEAR.name());
        newAgreement.setStartDate(request.getStartDate());
        newAgreement.setEndDate(request.getEndDate());
        newAgreement.setPerBedPerDayRate(request.getPerBedPerDayRate());
        newAgreement.setVersion(oldAgreement.getVersion() + 1);
        newAgreement.setCreatedAt(Instant.now());
        newAgreement.setUpdatedAt(Instant.now());
        agreementRepository.save(newAgreement);

        // Update HCF status to ACTIVE
        hcf.setStatus("ACTIVE");
        hcf.setUpdatedAt(Instant.now());
        hcfRepository.save(hcf);

        // Create billing config with new bed rate
        AgreementBillingConfig config = new AgreementBillingConfig();
        config.setAgreement(newAgreement);
        config.setBaseGramsPerBedPerDay(277);
        config.setBaseRatePerBedPerDay(request.getPerBedPerDayRate());
        config.setEffectiveFrom(request.getStartDate());
        config.setCreatedBy(UUID.randomUUID()); // TODO: Get from security context
        billingConfigRepository.save(config);

        // Audit log
        auditLogService.log("HCF", hcfId, "AGREEMENT_RENEWED", null,
                String.format("New agreement %s created (version %d), replacing expired %s",
                        newAgreement.getAgreementNumber(),
                        newAgreement.getVersion(),
                        oldAgreement.getAgreementNumber()));
        log.info("Agreement renewed for HCF {}: {} -> {}",
                hcfId, oldAgreement.getAgreementNumber(), newAgreement.getAgreementNumber());

        return getHcfDetail(hcfId, facilityId);
    }

    /**
     * Directly register an HCF by CBWTF Admin (no approval required).
     * 
     * Security checks:
     * 1. Compute identity hash to prevent duplicates
     * 2. Validate eligibility for creating new agreement
     * 3. Enforce rent agreement document for RENTED properties
     * 
     * Creates HCF with ACTIVE status, agreement, and billing config immediately.
     * Logs audit event: HCF_REGISTERED_BY_ADMIN
     */
    @Transactional
    public HcfDetailDTO registerHcfDirectly(UUID facilityId, UUID adminUserId,
            CbwtfAdminHcfRegistrationRequest request) {
        log.info("CBWTF Admin {} registering new HCF: {} for facility {}", adminUserId, request.getName(), facilityId);

        // 1. Validate rent agreement for RENTED properties (backend enforcement)
        if ("RENTED".equals(request.getOwnershipType()) &&
                (request.getRentAgreementUrl() == null || request.getRentAgreementUrl().isBlank())) {
            throw new IllegalArgumentException("Rent agreement document is required for rented properties");
        }

        // 2. Validate bedded facilities must have bed count
        if (Boolean.TRUE.equals(request.getBedded()) &&
                (request.getNumberOfBeds() == null || request.getNumberOfBeds() <= 0)) {
            throw new IllegalArgumentException("Number of beds is required for bedded facilities");
        }

        // 3. Compute identity hash for duplicate detection
        String identityHash = hcfIdentityService.computeFingerprint(
                request.getName(),
                request.getGstNo(),
                request.getPanNo(),
                request.getGpsLat(),
                request.getGpsLon());
        log.debug("Computed identity hash: {}", identityHash.substring(0, 16) + "...");

        // Get facility
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));

        // 4. Create HCF entity
        Hcf hcf = new Hcf();
        hcf.setCode("HCF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        hcf.setName(request.getName());
        hcf.setAddress(request.getAddress());
        hcf.setPincode(request.getPincode());
        hcf.setState(request.getState());
        hcf.setDoctorName(request.getDoctorName());
        hcf.setContactPhone(request.getContactPhone());
        hcf.setContactEmail(request.getContactEmail());
        hcf.setPanNo(request.getPanNo());
        hcf.setGstNo(request.getGstNo());
        hcf.setAadharNo(request.getAadharNo());
        hcf.setOwnershipType(request.getOwnershipType());
        hcf.setRentAgreementUrl(request.getRentAgreementUrl());
        hcf.setBedded(request.getBedded());
        hcf.setNumberOfBeds(request.getNumberOfBeds());
        hcf.setMonthlyCharges(request.getMonthlyCharges());
        hcf.setOtherNotes(request.getOtherNotes());
        hcf.setGpsLat(request.getGpsLat());
        hcf.setGpsLon(request.getGpsLon());
        hcf.setIdentityHash(identityHash);

        // New HCF category fields
        hcf.setCity(request.getCity());
        hcf.setSeatCount(request.getSeatCount());
        if (request.getHcfType() != null && !request.getHcfType().isBlank()) {
            try {
                hcf.setHcfType(com.smartcbwtf.domain.HcfType.valueOf(request.getHcfType()));
            } catch (IllegalArgumentException e) {
                hcf.setHcfType(com.smartcbwtf.domain.HcfType.HOSPITAL); // Default
            }
        }

        hcf.setStatus("ACTIVE"); // Auto-approved for admin registration
        hcf.setApprovalStatus(com.smartcbwtf.domain.ApprovalStatus.APPROVED); // Portal access enabled
        hcf.setCreatedAt(Instant.now());
        hcf.setUpdatedAt(Instant.now());

        // Recalculate bed access category (considers hcfType)
        hcf.recalculateBedAccessCategory();

        hcfRepository.save(hcf);

        // 5. Check eligibility for new agreement (after HCF is created)
        agreementValidationService.assertCanCreateAgreement(hcf.getId());

        // 6. Create Agreement
        Agreement agreement = new Agreement();
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setAgreementNumber(agreementNumberGenerator.generateNextAgreementNumber(facility));
        agreement.setStatus(Agreement.Status.ACTIVE.name());
        agreement.setDuesStatus(Agreement.DuesStatus.CLEAR.name());
        agreement.setStartDate(request.getAgreementStartDate());
        agreement.setEndDate(request.getAgreementEndDate());
        agreement.setPerBedPerDayRate(request.getPerBedPerDayRate());
        agreement.setCreatedAt(Instant.now());
        agreement.setUpdatedAt(Instant.now());
        agreementRepository.save(agreement);

        // 7. Create default billing config
        AgreementBillingConfig config = new AgreementBillingConfig();
        config.setAgreement(agreement);
        config.setBaseGramsPerBedPerDay(270); // Default waste allowance
        config.setBaseRatePerBedPerDay(request.getPerBedPerDayRate());
        config.setEffectiveFrom(request.getAgreementStartDate());
        config.setCreatedBy(adminUserId);
        billingConfigRepository.save(config);

        // 7.5 Create HCF portal admin user for 30+ bed HCFs
        String generatedPassword = null;
        if (hcf.isPortalEligible()) {
            generatedPassword = generateRandomPassword();
            AppUser hcfAdmin = new AppUser();
            hcfAdmin.setUsername(agreement.getAgreementNumber()); // Agreement number as username
            hcfAdmin.setPasswordHash(passwordEncoder.encode(generatedPassword));
            hcfAdmin.setRole("HCF_ADMIN");
            hcfAdmin.setHcf(hcf);
            hcfAdmin.setFullName(hcf.getName() + " Admin");
            hcfAdmin.setEmail(hcf.getContactEmail());
            hcfAdmin.setActive(true);
            hcfAdmin.setCreatedAt(Instant.now());
            hcfAdmin.setUpdatedAt(Instant.now());
            userRepository.save(hcfAdmin);
            log.info("Created HCF_ADMIN user for portal: username={}", agreement.getAgreementNumber());

            // Send credentials email
            sendCredentialsEmail(hcf.getName(), hcf.getContactEmail(), agreement.getAgreementNumber(),
                    generatedPassword);
        }

        // 8. Audit log with distinct event type
        String auditDetails = String.format(
                "Admin registered HCF. Agreement: %s, Source: CBWTF_PORTAL, Admin: %s%s",
                agreement.getAgreementNumber(), adminUserId,
                generatedPassword != null ? ", Portal user created" : "");
        auditLogService.log("HCF", hcf.getId(), "HCF_REGISTERED_BY_ADMIN", adminUserId, auditDetails);
        log.info("HCF {} registered by admin {}, agreement {} created",
                hcf.getId(), adminUserId, agreement.getAgreementNumber());

        // Send HCF approval/registration confirmation email (for ALL HCFs)
        sendHcfApprovalEmail(hcf, agreement);

        return getHcfDetail(hcf.getId(), facilityId);
    }

    /**
     * Generate a random password for HCF portal users.
     */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Upload rent agreement document to local storage.
     * Returns the URL path to the uploaded file.
     */
    public String uploadRentAgreement(UUID facilityId, org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }

        String contentType = file.getContentType();
        String ext;
        if ("application/pdf".equals(contentType)) {
            ext = "pdf";
        } else if (contentType != null && contentType.startsWith("image/")) {
            ext = switch (contentType) {
                case "image/jpeg" -> "jpg";
                case "image/png" -> "png";
                case "image/gif" -> "gif";
                case "image/webp" -> "webp";
                default -> "jpg";
            };
        } else {
            throw new IllegalArgumentException("Only PDF and image files allowed");
        }

        try {
            String uploadDir = "uploads/rent-agreements";
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            String filename = facilityId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            java.nio.file.Path filePath = uploadPath.resolve(filename);
            java.nio.file.Files.copy(file.getInputStream(), filePath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String url = "/uploads/rent-agreements/" + filename;
            log.info("Rent agreement uploaded for facility {}: {}", facilityId, url);
            return url;

        } catch (java.io.IOException e) {
            log.error("Failed to upload rent agreement", e);
            throw new RuntimeException("Failed to save file", e);
        }
    }

    /**
     * Get HCF portal admin user info.
     * Only for HCFs with 30+ beds (portal eligible).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPortalAdminInfo(UUID hcfId, UUID facilityId) {
        // Verify HCF belongs to facility
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or not associated with this facility"));

        Hcf hcf = agreement.getHcf();

        // Check if portal eligible (30+ beds)
        if (!hcf.isPortalEligible()) {
            return Map.of(
                    "eligible", false,
                    "reason", "HCF has less than 30 beds or is not approved");
        }

        // Find HCF_ADMIN user for this HCF
        return userRepository.findByHcfIdAndRole(hcfId, "HCF_ADMIN")
                .stream()
                .findFirst()
                .map(user -> Map.<String, Object>of(
                        "eligible", true,
                        "hasAdmin", true,
                        "username", user.getUsername(),
                        "fullName", user.getFullName() != null ? user.getFullName() : "",
                        "active", user.isActive()))
                .orElse(Map.of(
                        "eligible", true,
                        "hasAdmin", false,
                        "message", "No HCF_ADMIN user exists for this HCF"));
    }

    /**
     * Create HCF portal admin user for an existing eligible HCF.
     * Only for HCFs with 30+ beds that don't already have an admin.
     * Username = Agreement Number
     */
    @Transactional
    public Map<String, Object> createPortalAdmin(UUID hcfId, UUID facilityId) {
        // Verify HCF belongs to facility
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or not associated with this facility"));

        Hcf hcf = agreement.getHcf();

        // Check if portal eligible (30+ beds)
        if (!hcf.isPortalEligible()) {
            throw new IllegalStateException("HCF is not portal eligible (requires 30+ beds and approved status)");
        }

        // Check if admin already exists
        boolean adminExists = userRepository.findByHcfIdAndRole(hcfId, "HCF_ADMIN")
                .stream()
                .findFirst()
                .isPresent();

        if (adminExists) {
            throw new IllegalStateException("HCF admin already exists for this HCF");
        }

        // Generate password
        String generatedPassword = generateRandomPassword();

        // Create HCF_ADMIN user
        AppUser hcfAdmin = new AppUser();
        hcfAdmin.setUsername(agreement.getAgreementNumber()); // Agreement number as username
        hcfAdmin.setPasswordHash(passwordEncoder.encode(generatedPassword));
        hcfAdmin.setRole("HCF_ADMIN");
        hcfAdmin.setHcf(hcf);
        hcfAdmin.setFullName(hcf.getName() + " Admin");
        hcfAdmin.setEmail(hcf.getContactEmail());
        hcfAdmin.setActive(true);
        hcfAdmin.setCreatedAt(Instant.now());
        hcfAdmin.setUpdatedAt(Instant.now());
        userRepository.save(hcfAdmin);

        // Send credentials email
        sendCredentialsEmail(hcf.getName(), hcf.getContactEmail(), agreement.getAgreementNumber(), generatedPassword);

        // Audit log
        auditLogService.log("USER", hcfAdmin.getId(), "HCF_ADMIN_CREATED", null,
                "Created HCF portal admin for HCF: " + hcf.getName() + ", username: " + agreement.getAgreementNumber());
        log.info("Created HCF_ADMIN user for existing HCF: username={}, hcfId={}", agreement.getAgreementNumber(),
                hcfId);

        return Map.of(
                "success", true,
                "username", agreement.getAgreementNumber(),
                "tempPassword", generatedPassword,
                "message", "HCF admin created successfully");
    }

    /**
     * Reset HCF portal admin password.
     * Only for HCFs with 30+ beds (portal eligible).
     */
    @Transactional
    public Map<String, Object> resetPortalAdminPassword(UUID hcfId, UUID facilityId, String newPassword) {
        // Verify HCF belongs to facility
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or not associated with this facility"));

        Hcf hcf = agreement.getHcf();

        // Check if portal eligible (30+ beds)
        if (!hcf.isPortalEligible()) {
            throw new IllegalStateException("HCF is not portal eligible (requires 30+ beds and approved status)");
        }

        // Find HCF_ADMIN user for this HCF
        AppUser adminUser = userRepository.findByHcfIdAndRole(hcfId, "HCF_ADMIN")
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No HCF_ADMIN user exists for this HCF"));

        // Validate password
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        // Update password
        adminUser.setPasswordHash(passwordEncoder.encode(newPassword));
        adminUser.setUpdatedAt(Instant.now());
        userRepository.save(adminUser);

        // Send password reset email
        sendPasswordResetEmail(hcf.getName(), hcf.getContactEmail(), adminUser.getUsername(), newPassword);

        // Audit log
        auditLogService.log("USER", adminUser.getId(), "HCF_ADMIN_PASSWORD_RESET", null,
                "Password reset by CBWTF admin for HCF portal user: " + adminUser.getUsername());
        log.info("Password reset for HCF_ADMIN user {} of HCF {}", adminUser.getUsername(), hcfId);

        return Map.of(
                "success", true,
                "username", adminUser.getUsername(),
                "message", "Password updated successfully");
    }

    /**
     * Enable portal access for a small HCF (0-30 beds).
     * Creates an HCF_ADMIN user with generated password.
     * This is a manual override for HCFs that don't automatically qualify.
     */
    @Transactional
    public Map<String, Object> enablePortalAccessForSmallHcf(UUID hcfId, UUID facilityId) {
        // Verify HCF belongs to facility
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or not associated with this facility"));

        Hcf hcf = agreement.getHcf();

        // Verify HCF is NOT auto-eligible (i.e., it's 0-30 beds)
        if (hcf.getBedAccessCategory() != null && hcf.getBedAccessCategory().isPortalEligible()) {
            throw new IllegalStateException(
                    "HCF is already auto-eligible for portal access (30+ beds). Use createPortalAdmin instead.");
        }

        // Verify HCF is approved
        if (!"ACTIVE".equals(hcf.getStatus())) {
            throw new IllegalStateException("HCF must be approved before enabling portal access");
        }

        // Check if portal access already manually enabled
        if (hcf.isPortalAccessManuallyEnabled()) {
            throw new IllegalStateException("Portal access is already enabled for this HCF");
        }

        // Check if admin already exists
        boolean adminExists = userRepository.findByHcfIdAndRole(hcfId, "HCF_ADMIN")
                .stream()
                .findFirst()
                .isPresent();

        if (adminExists) {
            throw new IllegalStateException("HCF admin already exists for this HCF");
        }

        // Enable manual portal access
        hcf.setPortalAccessManuallyEnabled(true);
        hcf.setPortalAccessEnabled(true);
        hcfRepository.save(hcf);

        // Generate password
        String generatedPassword = generateRandomPassword();

        // Create HCF_ADMIN user
        AppUser hcfAdmin = new AppUser();
        hcfAdmin.setUsername(agreement.getAgreementNumber()); // Agreement number as username
        hcfAdmin.setPasswordHash(passwordEncoder.encode(generatedPassword));
        hcfAdmin.setRole("HCF_ADMIN");
        hcfAdmin.setHcf(hcf);
        hcfAdmin.setFullName(hcf.getName() + " Admin");
        hcfAdmin.setEmail(hcf.getContactEmail());
        hcfAdmin.setActive(true);
        hcfAdmin.setCreatedAt(Instant.now());
        hcfAdmin.setUpdatedAt(Instant.now());
        userRepository.save(hcfAdmin);

        // Send credentials email
        sendCredentialsEmail(hcf.getName(), hcf.getContactEmail(), agreement.getAgreementNumber(), generatedPassword);

        // Audit log
        auditLogService.log("USER", hcfAdmin.getId(), "HCF_ADMIN_CREATED_MANUAL", null,
                "Manually enabled portal access for small HCF: " + hcf.getName() + ", username: "
                        + agreement.getAgreementNumber());
        log.info("Manually enabled portal access for small HCF: username={}, hcfId={}, beds={}",
                agreement.getAgreementNumber(), hcfId, hcf.getNumberOfBeds());

        return Map.of(
                "success", true,
                "username", agreement.getAgreementNumber(),
                "tempPassword", generatedPassword,
                "message", "Portal access enabled and admin created successfully");
    }

    /**
     * Send HCF credentials email using professional template.
     */
    private void sendCredentialsEmail(String hcfName, String email, String username, String password) {
        log.info("sendCredentialsEmail called: hcfName={}, email={}, username={}", hcfName, email, username);
        if (email == null || email.isBlank()) {
            log.warn("Cannot send credentials email: no email for HCF {}", hcfName);
            return;
        }
        try {
            String html = emailService.getTemplates().hcfCredentials(hcfName, username, password, portalUrl);
            emailService.sendHtmlEmail(email, "Your SmartCBWTF Portal Credentials", html);
            log.info("Credentials email sent to HCF: {}", email);
        } catch (Exception e) {
            log.warn("Failed to send credentials email to {}: {}", email, e.getMessage());
        }
    }

    /**
     * Send HCF approval notification email.
     */
    private void sendHcfApprovalEmail(Hcf hcf, Agreement agreement) {
        log.info("sendHcfApprovalEmail called: hcfName={}, email={}, agreementNumber={}",
                hcf.getName(), hcf.getContactEmail(), agreement.getAgreementNumber());
        if (hcf.getContactEmail() == null || hcf.getContactEmail().isBlank()) {
            log.warn("Cannot send approval email: no email for HCF {}", hcf.getName());
            return;
        }
        try {
            String html = emailService.getTemplates().hcfApproved(
                    hcf.getName(),
                    agreement.getAgreementNumber(),
                    agreement.getStartDate().toString(),
                    agreement.getEndDate().toString());
            emailService.sendHtmlEmail(hcf.getContactEmail(), "Your HCF Registration is Approved - SmartCBWTF", html);
            log.info("Approval email sent to HCF: {}", hcf.getContactEmail());
        } catch (Exception e) {
            log.warn("Failed to send approval email to {}: {}", hcf.getContactEmail(), e.getMessage());
        }
    }

    /**
     * Send password reset notification email.
     */
    private void sendPasswordResetEmail(String hcfName, String email, String username, String newPassword) {
        if (email == null || email.isBlank()) {
            log.warn("Cannot send password reset email: no email for HCF {}", hcfName);
            return;
        }
        try {
            String html = emailService.getTemplates().passwordReset(hcfName, newPassword);
            emailService.sendHtmlEmail(email, "Your SmartCBWTF Password Has Been Reset", html);
            log.info("Password reset email sent to HCF: {}", email);
        } catch (Exception e) {
            log.warn("Failed to send password reset email to {}: {}", email, e.getMessage());
        }
    }
}
