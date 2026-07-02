package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.dto.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.util.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.smartcbwtf.dto.AgreementCorrectionRequestDTO;

/**
 * Service for CBWTF Admin HCF Management operations.
 * All methods are tenant-scoped via facilityId parameter.
 */
@Service
public class CbwtfHcfService {

    private static final Logger log = LoggerFactory.getLogger(CbwtfHcfService.class);
    private static final int DEFAULT_HCF_LIST_LIMIT = 500;
    private static final int MAX_HCF_LIST_LIMIT = 1000;
    private static final int DEFAULT_QUEUE_LIMIT = 100;
    private static final int MAX_QUEUE_LIMIT = 250;

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
    private final AttendanceRepository attendanceRepository;
    private final EmailService emailService;
    private final FacilitySettingsRepository facilitySettingsRepository;
    private final AgreementCorrectionRequestRepository correctionRequestRepository;
    private final PasswordPolicyValidator passwordPolicyValidator;

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
            AttendanceRepository attendanceRepository,
            EmailService emailService,
            FacilitySettingsRepository facilitySettingsRepository,
            AgreementCorrectionRequestRepository correctionRequestRepository,
            PasswordPolicyValidator passwordPolicyValidator) {
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
        this.attendanceRepository = attendanceRepository;
        this.emailService = emailService;
        this.facilitySettingsRepository = facilitySettingsRepository;
        this.correctionRequestRepository = correctionRequestRepository;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    /**
     * List HCFs with agreements for the facility.
     * Auto-corrects agreement/HCF status based on current date and validity period.
     */
    @Transactional
    public List<HcfListItemDTO> listByFacility(UUID facilityId) {
        return listByFacility(facilityId, DEFAULT_HCF_LIST_LIMIT);
    }

    @Transactional
    public List<HcfListItemDTO> listByFacility(UUID facilityId, int limit) {
        // Return latest agreement for each HCF (including Expired/Terminated)
        PageRequest pageable = PageRequest.of(0,
                PaginationUtils.normalizeSize(limit, DEFAULT_HCF_LIST_LIMIT, MAX_HCF_LIST_LIMIT));
        List<Agreement> agreements = agreementRepository.findLatestAgreementsByFacilityId(facilityId, pageable);
        Map<UUID, Instant> lastPickupByHcfId = lastPickupTimesByHcfId(agreements);
        LocalDate today = LocalDate.now();

        return agreements.stream()
                .map(agreement -> {
                    Hcf hcf = agreement.getHcf();

                    // Auto-correct status based on validity period
                    LocalDate startDate = agreement.getStartDate();
                    LocalDate endDate = agreement.getEndDate();

                    boolean isPendingApproval = Agreement.Status.PENDING_APPROVAL.name().equals(agreement.getStatus());

                    if (!isPendingApproval && startDate != null && endDate != null) {
                        if (today.isBefore(startDate)) {
                            if (!Agreement.Status.UPCOMING.name().equals(agreement.getStatus())) {
                                agreement.setStatus(Agreement.Status.UPCOMING.name());
                                hcf.setStatus("ACTIVE"); // Keep them active so they can log in
                                agreement.setUpdatedAt(Instant.now());
                                hcf.setUpdatedAt(Instant.now());
                                agreementRepository.save(agreement);
                                hcfRepository.save(hcf);
                                log.info("Auto-corrected agreement {} status to UPCOMING in list",
                                        agreement.getAgreementNumber());
                            }
                        } else if (today.isAfter(endDate)) {
                            if (!Agreement.Status.EXPIRED.name().equals(agreement.getStatus())) {
                                agreement.setStatus(Agreement.Status.EXPIRED.name());
                                hcf.setStatus("INACTIVE");
                                agreement.setUpdatedAt(Instant.now());
                                hcf.setUpdatedAt(Instant.now());
                                agreementRepository.save(agreement);
                                hcfRepository.save(hcf);
                                log.info("Auto-corrected agreement {} status to EXPIRED in list",
                                        agreement.getAgreementNumber());
                            }
                        } else {
                            if (!Agreement.Status.ACTIVE.name().equals(agreement.getStatus())) {
                                agreement.setStatus(Agreement.Status.ACTIVE.name());
                                hcf.setStatus("ACTIVE");
                                agreement.setUpdatedAt(Instant.now());
                                hcf.setUpdatedAt(Instant.now());
                                agreementRepository.save(agreement);
                                hcfRepository.save(hcf);
                                log.info("Auto-corrected agreement {} status to ACTIVE in list",
                                        agreement.getAgreementNumber());
                            }
                        }
                    }

                    Instant lastPickupAt = lastPickupByHcfId.get(hcf.getId());
                    return HcfListItemDTO.from(hcf, agreement, lastPickupAt);
                })
                .collect(Collectors.toList());
    }

    private Map<UUID, Instant> lastPickupTimesByHcfId(List<Agreement> agreements) {
        List<UUID> hcfIds = agreements.stream()
                .map(Agreement::getHcf)
                .filter(hcf -> hcf != null && hcf.getId() != null)
                .map(Hcf::getId)
                .distinct()
                .toList();
        if (hcfIds.isEmpty()) {
            return Map.of();
        }
        return bagEventRepository.findLastPickupTimesByHcfIds(hcfIds).stream()
                .collect(Collectors.toMap(
                        BagEventRepository.HcfLastPickup::getHcfId,
                        BagEventRepository.HcfLastPickup::getLastPickupAt));
    }

    /**
     * Get HCF detail with agreement, billing config, and operational summary.
     * Auto-corrects agreement/HCF status based on current date and validity period.
     */
    @Transactional
    public HcfDetailDTO getHcfDetail(UUID hcfId, UUID facilityId) {
        log.info("Fetching HCF detail for HCF: {} and Facility: {}", hcfId, facilityId);

        // Verify HCF belongs to this facility via agreement (any status)
        Agreement agreement = latestAgreementForHcf(hcfId, facilityId)
                .orElseThrow(() -> {
                    log.error("No agreement found for HCF {} and Facility {}", hcfId, facilityId);
                    return new IllegalArgumentException("HCF not found or not associated with this facility");
                });

        Hcf hcf = agreement.getHcf();

        // Auto-correct status based on validity period
        LocalDate today = LocalDate.now();
        LocalDate startDate = agreement.getStartDate();
        LocalDate endDate = agreement.getEndDate();

        boolean isPendingApproval = Agreement.Status.PENDING_APPROVAL.name().equals(agreement.getStatus());
        boolean statusCorrected = false;

        if (!isPendingApproval && startDate != null && endDate != null) {
            if (today.isBefore(startDate)) {
                if (!Agreement.Status.UPCOMING.name().equals(agreement.getStatus())) {
                    agreement.setStatus(Agreement.Status.UPCOMING.name());
                    hcf.setStatus("ACTIVE");
                    statusCorrected = true;
                    log.info("Auto-corrected agreement {} status to UPCOMING (future validity period)",
                            agreement.getAgreementNumber());
                }
            } else if (today.isAfter(endDate)) {
                if (!Agreement.Status.EXPIRED.name().equals(agreement.getStatus())) {
                    agreement.setStatus(Agreement.Status.EXPIRED.name());
                    hcf.setStatus("INACTIVE");
                    statusCorrected = true;
                    log.info("Auto-corrected agreement {} status to EXPIRED (outside validity period)",
                            agreement.getAgreementNumber());
                }
            } else {
                if (!Agreement.Status.ACTIVE.name().equals(agreement.getStatus())) {
                    agreement.setStatus(Agreement.Status.ACTIVE.name());
                    hcf.setStatus("ACTIVE");
                    statusCorrected = true;
                    log.info("Auto-corrected agreement {} status to ACTIVE (within validity period)",
                            agreement.getAgreementNumber());
                }
            }
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

        HcfDetailDTO.OperationalSummary summary = buildOperationalSummary(facilityId, hcfId);

        return HcfDetailDTO.from(hcf, agreement, billingConfig, summary);
    }

    /**
     * Get HCF detail for Top Management within the current facility.
     */
    @Transactional
    public HcfDetailDTO getHcfDetailForTopManagement(UUID hcfId, UUID facilityId) {
        log.info("Fetching HCF detail for Top Management for HCF: {} and facility: {}", hcfId, facilityId);

        Agreement agreement = latestAgreementForHcf(hcfId, facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found for HCF"));

        Hcf hcf = agreement.getHcf();
        if (!"PENDING_APPROVAL".equals(hcf.getStatus())) {
            throw new IllegalStateException("HCF is not pending Top Management approval");
        }

        // Status auto-correction is ideally handled elsewhere, but can be done here if needed.
        // We will skip auto-correction for top management read-only view to prevent
        // unexpected side effects during approval listing.

        AgreementBillingConfig billingConfig = billingConfigRepository
                .findActiveByAgreementId(agreement.getId())
                .orElse(null);

        HcfDetailDTO.OperationalSummary summary = buildOperationalSummary(facilityId, hcfId);

        return HcfDetailDTO.from(hcf, agreement, billingConfig, summary);
    }

    private Optional<Agreement> latestAgreementForHcf(UUID hcfId, UUID facilityId) {
        return agreementRepository.findLatestByHcfIdAndFacilityId(hcfId, facilityId, PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }

    private HcfDetailDTO.OperationalSummary buildOperationalSummary(UUID facilityId, UUID hcfId) {
        HcfDetailDTO.OperationalSummary summary = new HcfDetailDTO.OperationalSummary();
        summary.setTotalPickups(bagEventRepository.countPickupDaysByHcfId(hcfId));
        summary.setTotalWasteKg(bagEventRepository.sumTotalWasteByHcfId(hcfId));
        summary.setLastPickupAt(bagEventRepository.findLastPickupTimeByHcfId(hcfId));
        summary.setTotalAttendanceMarks(toIntCount(attendanceRepository.countByFacilityIdAndHcfId(facilityId, hcfId)));
        summary.setLastAttendanceAt(attendanceRepository.findLastAttendanceTimeByFacilityIdAndHcfId(facilityId, hcfId));
        return summary;
    }

    private int toIntCount(long count) {
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
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
        if (request.getOccupancy() != null) {
            hcf.setOccupancy(request.getOccupancy());
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
        log.info("HCF {} location updated; exact coordinates recorded in audit log", hcfId);

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
     * Submit an Agreement Correction Request
     */
    @Transactional
    public void submitCorrectionRequest(UUID facilityId, UUID hcfId, AgreementCorrectionRequestDTO request, UUID requestedBy) {
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or not associated with this facility"));

        Hcf hcf = agreement.getHcf();
        Facility facility = agreement.getFacility();

        // Determine current value dynamically or let frontend pass it.
        // We will rely on the current value being correctly handled, but we can store what frontend sends or derive it.
        // For simplicity, we just save the request directly.
        AgreementCorrectionRequest correction = new AgreementCorrectionRequest();
        correction.setAgreement(agreement);
        correction.setHcf(hcf);
        correction.setFacility(facility);
        correction.setFieldName(request.getFieldName());

        // We could look up current value, or just leave it null/use what's needed. Let's try to extract if common fields:
        String currentValue = "N/A";
        switch (request.getFieldName()) {
            case "HCF Name": currentValue = hcf.getName(); break;
            case "Address": currentValue = hcf.getAddress(); break;
            case "Contact Phone": currentValue = hcf.getContactPhone(); break;
            case "Contact Email": currentValue = hcf.getContactEmail(); break;
            case "Doctor Name": currentValue = hcf.getDoctorName(); break;
            case "Number of Beds": currentValue = String.valueOf(hcf.getNumberOfBeds()); break;
            case "PAN No": currentValue = hcf.getPanNo(); break;
            case "GST No": currentValue = hcf.getGstNo(); break;
            case "Start Date": currentValue = agreement.getStartDate() != null ? agreement.getStartDate().toString() : "N/A"; break;
            case "End Date": currentValue = agreement.getEndDate() != null ? agreement.getEndDate().toString() : "N/A"; break;
            case "Per Bed Rate": currentValue = agreement.getPerBedPerDayRate() != null ? agreement.getPerBedPerDayRate().toString() : "N/A"; break;
        }

        correction.setCurrentValue(currentValue);
        correction.setRequestedValue(request.getRequestedValue());
        correction.setReason(request.getReason());
        correction.setRequestedBy(requestedBy);

        correctionRequestRepository.save(correction);

        auditLogService.log("HCF", hcfId, "CORRECTION_REQUESTED", requestedBy,
                String.format("Requested to correct %s to %s. Reason: %s", request.getFieldName(), request.getRequestedValue(), request.getReason()));
    }

    /**
     * Get Correction Requests for an HCF
     */
    @Transactional(readOnly = true)
    public List<AgreementCorrectionRequest> getCorrectionRequests(UUID facilityId, UUID hcfId) {
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found"));

        return correctionRequestRepository.findByAgreementIdOrderByRequestedAtDesc(agreement.getId());
    }

    /**
     * Approve an Agreement Correction Request
     */
    @Transactional
    public void approveCorrectionRequest(UUID requestId, UUID facilityId, UUID adminUserId) {
        AgreementCorrectionRequest req = correctionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Correction request not found"));

        if (req.getFacility() == null || !facilityId.equals(req.getFacility().getId())) {
            throw new IllegalArgumentException("Correction request not found");
        }

        if (req.getStatus() != AgreementCorrectionRequest.Status.PENDING) {
            throw new IllegalStateException("Correction request is not pending");
        }

        Hcf hcf = req.getHcf();
        Agreement agreement = req.getAgreement();
        String requestedValue = req.getRequestedValue();

        switch (req.getFieldName()) {
            case "HCF Name": hcf.setName(requestedValue); break;
            case "Address": hcf.setAddress(requestedValue); break;
            case "Contact Phone": hcf.setContactPhone(requestedValue); break;
            case "Contact Email": hcf.setContactEmail(requestedValue); break;
            case "Doctor Name": hcf.setDoctorName(requestedValue); break;
            case "Number of Beds":
                try {
                    hcf.setNumberOfBeds(Integer.parseInt(requestedValue));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid number of beds: " + requestedValue);
                }
                break;
            case "PAN No": hcf.setPanNo(requestedValue); break;
            case "GST No": hcf.setGstNo(requestedValue); break;
            case "Start Date":
                try {
                    agreement.setStartDate(LocalDate.parse(requestedValue));
                } catch (Exception e) {}
                break;
            case "End Date":
                try {
                    agreement.setEndDate(LocalDate.parse(requestedValue));
                } catch (Exception e) {}
                break;
            case "Per Bed Rate":
                try {
                    agreement.setPerBedPerDayRate(new BigDecimal(requestedValue));
                } catch (Exception e) {}
                break;
        }

        hcf.setUpdatedAt(Instant.now());
        agreement.setUpdatedAt(Instant.now());

        hcfRepository.save(hcf);
        agreementRepository.save(agreement);

        req.setStatus(AgreementCorrectionRequest.Status.APPROVED);
        req.setReviewedBy(adminUserId);
        req.setReviewedAt(Instant.now());
        correctionRequestRepository.save(req);

        auditLogService.log("HCF", hcf.getId(), "CORRECTION_APPROVED", adminUserId,
                String.format("Correction request for %s approved.", req.getFieldName()));
    }

    /**
     * Reject an Agreement Correction Request
     */
    @Transactional
    public void rejectCorrectionRequest(UUID requestId, UUID facilityId, String reason, UUID adminUserId) {
        AgreementCorrectionRequest req = correctionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Correction request not found"));

        if (req.getFacility() == null || !facilityId.equals(req.getFacility().getId())) {
            throw new IllegalArgumentException("Correction request not found");
        }

        if (req.getStatus() != AgreementCorrectionRequest.Status.PENDING) {
            throw new IllegalStateException("Correction request is not pending");
        }

        req.setStatus(AgreementCorrectionRequest.Status.REJECTED);
        req.setRejectionReason(reason);
        req.setReviewedBy(adminUserId);
        req.setReviewedAt(Instant.now());
        correctionRequestRepository.save(req);

        auditLogService.log("HCF", req.getHcf().getId(), "CORRECTION_REJECTED", adminUserId,
                String.format("Correction request for %s rejected. Reason: %s", req.getFieldName(), reason));
    }

    /**
     * List pending HCF registrations (from Android app).
     * Only returns HCFs that:
     * 1. Have PENDING_APPROVAL status
     * 2. Do NOT already have an ACTIVE agreement (data consistency check)
     */
    @Transactional(readOnly = true)
    public List<HcfListItemDTO> listPending(UUID facilityId) {
        return listPending(facilityId, DEFAULT_QUEUE_LIMIT);
    }

    @Transactional(readOnly = true)
    public List<HcfListItemDTO> listPending(UUID facilityId, int limit) {
        return agreementRepository
                .findLatestPendingOrResubmittableAgreementsByFacilityId(facilityId, firstQueuePage(limit)).stream()
                .map(agreement -> HcfListItemDTO.from(agreement.getHcf(), agreement, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public HcfListItemDTO updatePendingBillingModel(UUID hcfId, UUID facilityId, HcfUpdateRequest request) {
        if (request.billingModel() == null) {
            throw new IllegalArgumentException("Billing model is required");
        }

        Agreement agreement = agreementForFacility(hcfId, facilityId);
        Hcf hcf = agreement.getHcf();

        if (hcf.getApprovalStatus() == ApprovalStatus.APPROVED || "ACTIVE".equals(hcf.getStatus())) {
            throw new IllegalStateException("Cannot edit approved HCF. Billing model is locked.");
        }

        hcf.setBillingModel(request.billingModel());
        hcf.setBedded(request.billingModel() == BillingModel.BEDDED);
        hcf.setNumberOfBeds(request.numberOfBeds());
        hcf.setMonthlyCharges(request.monthlyCharges());
        hcf.recalculateBedAccessCategory();
        hcf.setUpdatedAt(Instant.now());
        hcfRepository.save(hcf);

        auditLogService.log("HCF", hcfId, "HCF_BILLING_MODEL_UPDATED", TenantContext.getUserId(),
                "Billing model updated before approval");
        log.info("Updated pending HCF {} billing model for facility {}", hcfId, facilityId);

        return HcfListItemDTO.from(hcf, agreement, null);
    }

    /**
     * List draft HCF registrations saved by CBWTF admin.
     */
    @Transactional(readOnly = true)
    public List<HcfListItemDTO> listDrafts(UUID facilityId) {
        return listDrafts(facilityId, DEFAULT_QUEUE_LIMIT);
    }

    @Transactional(readOnly = true)
    public List<HcfListItemDTO> listDrafts(UUID facilityId, int limit) {
        return agreementRepository.findLatestDraftAgreementsByFacilityId(facilityId, firstQueuePage(limit)).stream()
                .map(a -> HcfListItemDTO.from(a.getHcf(), a, null))
                .collect(Collectors.toList());
    }

    private static PageRequest firstQueuePage(int requestedLimit) {
        int limit = PaginationUtils.normalizeSize(requestedLimit, DEFAULT_QUEUE_LIMIT, MAX_QUEUE_LIMIT);
        return PageRequest.of(0, limit);
    }

    private static String validateOptionalRentAgreementUrl(String rentAgreementUrl, UUID facilityId) {
        if (rentAgreementUrl == null || rentAgreementUrl.isBlank()) {
            return null;
        }
        return UploadFileValidator.rentAgreementUrlForFacility(rentAgreementUrl, facilityId);
    }

    /**
     * Approve a pending HCF registration.
     * Creates agreement + default billing config.
     */
    @Transactional
    public HcfDetailDTO approveHcf(UUID hcfId, UUID facilityId, CbwtfHcfApprovalRequest request) {
        Agreement agreement = agreementForFacility(hcfId, facilityId);
        Hcf hcf = agreement.getHcf();

        if (!"PENDING_APPROVAL".equals(hcf.getStatus())) {
            throw new IllegalStateException("HCF is not pending approval");
        }

        agreement.setStatus(Agreement.Status.PENDING_APPROVAL.name()); // Forwarding to Top Management
        agreement.setDuesStatus(Agreement.DuesStatus.CLEAR.name());
        if (agreement.getStartDate() == null) {
            agreement.setStartDate(LocalDate.now());
        }
        if (agreement.getEndDate() == null) {
            agreement.setEndDate(agreement.getStartDate().plusYears(1));
        }
        BigDecimal perBedPerDayRate = request.getPerBedPerDayRate() != null
                ? request.getPerBedPerDayRate()
                : BigDecimal.ZERO;
        agreement.setPerBedPerDayRate(perBedPerDayRate);
        agreement.setUpdatedAt(Instant.now());
        agreementRepository.save(agreement);

        // Update HCF status - remains pending for Top Management
        hcf.setStatus("PENDING_APPROVAL");
        hcf.setUpdatedAt(Instant.now());
        hcfRepository.save(hcf);

        // Create default billing config
        AgreementBillingConfig config = billingConfigRepository.findActiveByAgreementId(agreement.getId())
                .orElseGet(AgreementBillingConfig::new);
        config.setAgreement(agreement);
        config.setBaseGramsPerBedPerDay(270);
        config.setBaseRatePerBedPerDay(perBedPerDayRate);
        config.setEffectiveFrom(agreement.getStartDate());
        UUID actorUserId = TenantContext.getUserId();
        config.setCreatedBy(actorUserId);
        billingConfigRepository.save(config);

        // Send an internal notification instead? For now, we defer standard hcfApproval
        // emails to Top Management.

        // Audit log
        auditLogService.log("HCF", hcfId, "HCF_REGISTRATION_REQUESTED", actorUserId,
                "Agreement " + agreement.getAgreementNumber() + " created and forwarded to Top Management");
        log.info("HCF {} checked by CBWTF, agreement {} created and pending top management", hcfId,
                agreement.getAgreementNumber());

        return getHcfDetail(hcfId, facilityId);
    }

    /**
     * Reject a pending HCF registration.
     */
    @Transactional
    public void rejectHcf(UUID hcfId, UUID facilityId, HcfRejectionRequest request) {
        Agreement agreement = agreementForFacility(hcfId, facilityId);
        Hcf hcf = agreement.getHcf();

        if (!"PENDING_APPROVAL".equals(hcf.getStatus())) {
            throw new IllegalStateException("HCF is not pending approval");
        }

        hcf.setStatus("REJECTED");
        hcf.setApprovalStatus(ApprovalStatus.REJECTED);
        hcf.setRejectionReason(request.getReason());
        hcf.setRejectionCount((hcf.getRejectionCount() == null ? 0 : hcf.getRejectionCount()) + 1);
        hcf.setOtherNotes(request.getReason());
        hcf.setUpdatedAt(Instant.now());
        hcfRepository.save(hcf);

        agreement.setStatus(Agreement.Status.TERMINATED.name());
        agreement.setTerminationReason(request.getReason());
        agreement.setTerminatedAt(Instant.now());
        agreement.setTerminatedBy(TenantContext.getUserId());
        agreement.setUpdatedAt(Instant.now());
        agreementRepository.save(agreement);

        // Audit log
        auditLogService.log("HCF", hcfId, "HCF_REJECTED", TenantContext.getUserId(), "Reason: " + request.getReason());
        log.info("HCF {} rejected: {}", hcfId, request.getReason());

        // Send rejection email to HCF
        if (hcf.getContactEmail() != null && !hcf.getContactEmail().isBlank()) {
            try {
                String html = emailService.getTemplates().hcfRejected(hcf.getName(), request.getReason());
                emailService.sendHtmlEmail(hcf.getContactEmail(), "Registration Status Update - SmartCBWTF", html);
                log.info("Rejection email sent to HCF: {}", hcf.getContactEmail());
            } catch (Exception e) {
                log.warn("Failed to send rejection email to {}: {}", hcf.getContactEmail(), e.getMessage());
            }
        }
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
        if (request.getMonthlyCharges() != null) {
            hcf.setMonthlyCharges(request.getMonthlyCharges());
            hcfRepository.save(hcf); // Update HCF's monthly charges for future billing
        }

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));

        // Create NEW agreement
        Agreement newAgreement = new Agreement();
        newAgreement.setHcf(hcf);
        newAgreement.setFacility(facility);
        newAgreement.setAgreementNumber(generateAgreementNumber(facilityId, facility));
        if (request.getStartDate().isAfter(LocalDate.now())) {
            newAgreement.setStatus(Agreement.Status.UPCOMING.name());
        } else {
            newAgreement.setStatus(Agreement.Status.ACTIVE.name());
        }
        newAgreement.setDuesStatus(Agreement.DuesStatus.CLEAR.name());
        newAgreement.setStartDate(request.getStartDate());
        newAgreement.setEndDate(request.getEndDate());
        newAgreement.setPerBedPerDayRate(
                request.getPerBedPerDayRate() != null ? request.getPerBedPerDayRate() : BigDecimal.ZERO);
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
        UUID actorUserId = TenantContext.getUserId();
        config.setCreatedBy(actorUserId);
        billingConfigRepository.save(config);

        // Audit log
        auditLogService.log("HCF", hcfId, "AGREEMENT_RENEWED", actorUserId,
                String.format("New agreement %s created (version %d), replacing expired %s",
                        newAgreement.getAgreementNumber(),
                        newAgreement.getVersion(),
                        oldAgreement.getAgreementNumber()));
        log.info("Agreement renewed for HCF {}: {} -> {}",
                hcfId, oldAgreement.getAgreementNumber(), newAgreement.getAgreementNumber());

        return getHcfDetail(hcfId, facilityId);
    }

    /**
     * Save an HCF Draft (bypasses standard validation).
     */
    @Transactional
    public HcfDetailDTO saveDraftDirectly(UUID facilityId, UUID adminUserId, CbwtfAdminHcfRegistrationRequest request) {
        log.info("CBWTF Admin {} saving draft HCF: {}", adminUserId, request.getName());

        Hcf hcf;
        Agreement agreement;

        if (request.getId() != null) {
            hcf = hcfRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Draft not found"));
            agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcf.getId(), facilityId)
                .stream().findFirst().orElse(null);
            if (agreement == null) {
                 throw new IllegalArgumentException("Agreement not found for draft");
            }
        } else {
            hcf = new Hcf();
            hcf.setCode("HCF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            hcf.setStatus("DRAFT");
            hcf.setApprovalStatus(com.smartcbwtf.domain.ApprovalStatus.PENDING);
            hcf.setCreatedAt(Instant.now());

            agreement = new Agreement();
            Facility facility = facilityRepository.findById(facilityId)
                    .orElseThrow(() -> new IllegalArgumentException("Facility not found"));
            agreement.setFacility(facility);
            agreement.setHcf(hcf);
            agreement.setCreatedAt(Instant.now());
            agreement.setStatus("DRAFT");
            agreement.setPerBedPerDayRate(java.math.BigDecimal.ZERO);
            agreement.setAgreementNumber("DRAFT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            agreement.setStartDate(LocalDate.now());
        }

        // Set available fields with fallbacks for NOT NULL columns
        hcf.setName(request.getName() != null ? request.getName() : "Draft HCF");
        hcf.setAddress(request.getAddress() != null ? request.getAddress() : "Draft Address");
        hcf.setGpsLat(request.getGpsLat());
        hcf.setGpsLon(request.getGpsLon());

        // Ownership type has default OWNED
        if (request.getOwnershipType() != null) hcf.setOwnershipType(request.getOwnershipType());

        // Other nullable fields
        if (request.getPincode() != null) hcf.setPincode(request.getPincode());
        if (request.getState() != null) hcf.setState(request.getState());
        if (request.getDoctorName() != null) hcf.setDoctorName(request.getDoctorName());
        if (request.getContactPhone() != null) hcf.setContactPhone(request.getContactPhone());
        if (request.getContactEmail() != null) hcf.setContactEmail(request.getContactEmail());
        if (request.getPanNo() != null) hcf.setPanNo(request.getPanNo());
        if (request.getGstNo() != null) hcf.setGstNo(request.getGstNo());
        if (request.getAadharNo() != null) hcf.setAadharNo(request.getAadharNo());
        if (request.getRentAgreementUrl() != null) {
            hcf.setRentAgreementUrl(validateOptionalRentAgreementUrl(request.getRentAgreementUrl(), facilityId));
        }
        if (request.getBedded() != null) hcf.setBedded(request.getBedded());
        if (request.getNumberOfBeds() != null) hcf.setNumberOfBeds(request.getNumberOfBeds());
        if (request.getCity() != null) hcf.setCity(request.getCity());
        if (request.getSeatCount() != null) hcf.setSeatCount(request.getSeatCount());
        if (request.getMonthlyCharges() != null) hcf.setMonthlyCharges(request.getMonthlyCharges());
        if (request.getOtherNotes() != null) hcf.setOtherNotes(request.getOtherNotes());

        if (request.getHcfType() != null && !request.getHcfType().isBlank()) {
            try {
                hcf.setHcfType(com.smartcbwtf.domain.HcfType.valueOf(request.getHcfType()));
            } catch (IllegalArgumentException e) {
                hcf.setHcfType(com.smartcbwtf.domain.HcfType.HOSPITAL);
            }
        }

        hcf.setUpdatedAt(Instant.now());
        hcf = hcfRepository.save(hcf);

        if (request.getAgreementStartDate() != null) agreement.setStartDate(request.getAgreementStartDate());
        if (request.getAgreementEndDate() != null) agreement.setEndDate(request.getAgreementEndDate());
        if (request.getPerBedPerDayRate() != null) agreement.setPerBedPerDayRate(request.getPerBedPerDayRate());

        agreement.setUpdatedAt(Instant.now());
        agreementRepository.save(agreement);

        auditLogService.log("HCF", hcf.getId(), "HCF_DRAFT_SAVED", adminUserId, "Admin saved HCF draft");

        return getHcfDetail(hcf.getId(), facilityId);
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
        String rentAgreementUrl = validateOptionalRentAgreementUrl(request.getRentAgreementUrl(), facilityId);
        if ("RENTED".equalsIgnoreCase(request.getOwnershipType()) && rentAgreementUrl == null) {
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

        boolean submittingDraft = request.getId() != null;
        Hcf hcf;
        Agreement agreement;
        AgreementBillingConfig config;

        if (submittingDraft) {
            hcf = hcfRepository.findById(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Draft HCF not found"));
            agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcf.getId(), facilityId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Draft agreement not found"));
            if (!"DRAFT".equals(hcf.getStatus()) || !"DRAFT".equals(agreement.getStatus())) {
                throw new IllegalStateException("Only draft HCFs can be submitted from draft mode");
            }
            config = billingConfigRepository.findActiveByAgreementId(agreement.getId()).orElse(null);
        } else {
            hcf = new Hcf();
            hcf.setCode("HCF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            hcf.setCreatedAt(Instant.now());

            agreement = new Agreement();
            agreement.setHcf(hcf);
            agreement.setFacility(facility);
            agreement.setCreatedAt(Instant.now());
            agreement.setCreatedBy(adminUserId);

            config = null;
        }

        // 4. Create or update HCF entity
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
        hcf.setRentAgreementUrl(rentAgreementUrl);
        hcf.setBedded(request.getBedded());
        hcf.setNumberOfBeds(request.getNumberOfBeds());
        hcf.setMonthlyCharges(request.getMonthlyCharges());
        hcf.setOccupancy(request.getOccupancy());
        hcf.setOtherNotes(request.getOtherNotes());
        hcf.setGpsLat(request.getGpsLat());
        hcf.setGpsLon(request.getGpsLon());
        hcf.setIdentityHash(identityHash);
        hcf.setTaxRate(request.getTaxRate() != null ? request.getTaxRate() : 5.0);
        if (request.getExcessRatePerKg() != null) {
            hcf.setExcessRatePerKg(request.getExcessRatePerKg().doubleValue());
        }

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

        hcf.setStatus("PENDING_APPROVAL"); // Changed from ACTIVE
        hcf.setApprovalStatus(com.smartcbwtf.domain.ApprovalStatus.PENDING); // Awaiting approval
        hcf.setUpdatedAt(Instant.now());

        // Recalculate bed access category (considers hcfType)
        hcf.recalculateBedAccessCategory();

        hcf = hcfRepository.save(hcf);

        // 5. Check eligibility for new agreement (after HCF is created)
        agreementValidationService.assertCanCreateAgreement(hcf.getId());

        // 6. Create or update Agreement
        agreement.setHcf(hcf);
        agreement.setFacility(facility);

        // Determine agreement number: custom or auto-generated with facility settings
        String agreementNum;
        if (request.getCustomAgreementNumber() != null && !request.getCustomAgreementNumber().isBlank()) {
            // Custom agreement number - validate uniqueness
            String customNum = request.getCustomAgreementNumber().trim();
            UUID currentAgreementId = agreement.getId();
            if (agreementRepository.findByAgreementNumber(customNum)
                    .filter(existing -> !existing.getId().equals(currentAgreementId))
                    .isPresent()) {
                throw new IllegalArgumentException("Agreement number '" + customNum + "' already exists");
            }
            agreementNum = customNum;
            log.info("Using custom agreement number: {}", agreementNum);
        } else {
            // Auto-generate using per-facility format settings
            FacilitySettings settings = facilitySettingsRepository.findById(facilityId).orElse(null);
            if (settings != null) {
                agreementNum = agreementNumberGenerator.generateNextAgreementNumberWithSettings(
                        facility,
                        settings.getAgreementNumberPrefix(),
                        settings.getAgreementNumberSeparator(),
                        settings.getAgreementNumberSequenceDigits(),
                        settings.getAgreementNumberIncludeFacilityCode(),
                        settings.getAgreementNumberIncludeYear(),
                        settings.getAgreementNumberTemplate(),
                        settings.getAgreementNumberResetFrequency());
            } else {
                agreementNum = agreementNumberGenerator.generateNextAgreementNumber(facility);
            }
        }
        agreement.setAgreementNumber(agreementNum);
        agreement.setStatus(Agreement.Status.PENDING_APPROVAL.name()); // Forwarding to Top Management
        agreement.setDuesStatus(Agreement.DuesStatus.CLEAR.name());
        agreement.setStartDate(request.getAgreementStartDate());
        agreement.setEndDate(request.getAgreementEndDate());
        agreement.setPerBedPerDayRate(
                request.getPerBedPerDayRate() != null ? request.getPerBedPerDayRate() : BigDecimal.ZERO);
        agreement.setUpdatedAt(Instant.now());
        agreement = agreementRepository.save(agreement);

        // 7. Create or update default billing config
        if (config == null) {
            config = new AgreementBillingConfig();
            config.setAgreement(agreement);
            config.setCreatedBy(adminUserId);
        }
        config.setAgreement(agreement);
        config.setBaseGramsPerBedPerDay(277); // Standard 277g/bed/day waste allowance
        config.setBaseRatePerBedPerDay(request.getPerBedPerDayRate() != null
                ? request.getPerBedPerDayRate()
                : BigDecimal.ZERO);
        config.setEffectiveFrom(request.getAgreementStartDate());
        billingConfigRepository.save(config);

        // 8. Audit log with distinct event type
        String auditDetails = String.format(
                "Admin requested HCF Registration. Agreement: %s, Source: CBWTF_PORTAL, Admin: %s, SubmittedFromDraft: %s",
                agreement.getAgreementNumber(), adminUserId, submittingDraft);
        auditLogService.log("HCF", hcf.getId(), "HCF_REGISTRATION_REQUESTED", adminUserId, auditDetails);
        log.info("HCF {} registration requested by admin {}, agreement {} prepared for Top Management (fromDraft={})",
                hcf.getId(), adminUserId, agreement.getAgreementNumber(), submittingDraft);

        // Defer HcfApprovalEmail to TopManagement
        return getHcfDetail(hcf.getId(), facilityId);
    }

    private String generateAgreementNumber(UUID facilityId, Facility facility) {
        FacilitySettings settings = facilitySettingsRepository.findById(facilityId).orElse(null);
        if (settings == null) {
            return agreementNumberGenerator.generateNextAgreementNumber(facility);
        }
        return agreementNumberGenerator.generateNextAgreementNumberWithSettings(
                facility,
                settings.getAgreementNumberPrefix(),
                settings.getAgreementNumberSeparator(),
                settings.getAgreementNumberSequenceDigits(),
                settings.getAgreementNumberIncludeFacilityCode(),
                settings.getAgreementNumberIncludeYear(),
                settings.getAgreementNumberTemplate(),
                settings.getAgreementNumberResetFrequency());
    }

    private Agreement agreementForFacility(UUID hcfId, UUID facilityId) {
        return agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or not associated with this facility"));
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
     * Top Management Approval of HCF Registration.
     * Transitions HCF and Agreement from PENDING_APPROVAL to ACTIVE.
     * Generates portal credentials and dispatches welcome emails.
     */
    @Transactional
    public void approveHcfByTopManagement(UUID hcfId, UUID facilityId) {
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .filter(a -> "PENDING_APPROVAL".equals(a.getStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pending Agreement not found for HCF"));

        Hcf hcf = agreement.getHcf();

        if (!"PENDING_APPROVAL".equals(hcf.getStatus())) {
            throw new IllegalStateException("HCF is not pending approval");
        }

        // Update Statuses
        hcf.setStatus("ACTIVE");
        hcf.setApprovalStatus(com.smartcbwtf.domain.ApprovalStatus.APPROVED);
        hcf.setUpdatedAt(Instant.now());

        if (agreement.getStartDate() != null && agreement.getStartDate().isAfter(LocalDate.now())) {
            agreement.setStatus(Agreement.Status.UPCOMING.name());
        } else {
            agreement.setStatus(Agreement.Status.ACTIVE.name());
        }
        agreement.setUpdatedAt(Instant.now());

        hcfRepository.save(hcf);
        agreementRepository.save(agreement);

        // Create HCF portal admin user for eligible HCFs
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
            log.info("Created HCF_ADMIN user for portal after TopMgmt approval: username={}",
                    agreement.getAgreementNumber());

            // Send credentials email
            sendCredentialsEmail(hcf.getName(), hcf.getContactEmail(), agreement.getAgreementNumber(),
                    generatedPassword);
        }

        // Send HCF approval and registration confirmation email
        sendHcfApprovalEmail(hcf, agreement);

        // Audit log
        UUID adminUserId = com.smartcbwtf.config.TenantContext.getUserId();
        auditLogService.log("HCF", hcf.getId(), "HCF_APPROVED_TOP_MGMT", adminUserId, "HCF and Agreement Activated");
        log.info("HCF {} approved by Top Management {}", hcf.getId(), adminUserId);
    }

    /**
     * Top Management Rejection of HCF Registration.
     * Marks HCF as REJECTED and deletes staging Agreement records.
     */
    @Transactional
    public void rejectHcfByTopManagement(UUID hcfId, UUID facilityId, String reason) {
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .filter(a -> "PENDING_APPROVAL".equals(a.getStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pending Agreement not found for HCF"));

        Hcf hcf = agreement.getHcf();

        if (!"PENDING_APPROVAL".equals(hcf.getStatus())) {
            throw new IllegalStateException("HCF is not pending approval");
        }

        hcf.setRejectionCount(hcf.getRejectionCount() + 1);

        // Only delete on second rejection
        boolean shouldDeleteFull = hcf.getRejectionCount() >= 2;

        hcf.setStatus("REJECTED");
        hcf.setApprovalStatus(com.smartcbwtf.domain.ApprovalStatus.REJECTED);
        hcf.setRejectionReason(reason);
        hcf.setUpdatedAt(Instant.now());

        if (shouldDeleteFull) {
            hcfRepository.delete(hcf);
        } else {
            hcfRepository.save(hcf);
        }

        if (shouldDeleteFull) {
            billingConfigRepository.findByAgreementIdOrderByEffectiveFromDesc(agreement.getId())
                    .forEach(billingConfigRepository::delete);
            agreementRepository.delete(agreement);
        } else {
            agreement.setStatus("REJECTED");
            agreement.setUpdatedAt(Instant.now());
            agreementRepository.save(agreement);
        }

        UUID adminUserId = com.smartcbwtf.config.TenantContext.getUserId();
        auditLogService.log("HCF", hcfId, "HCF_REJECTED_TOP_MGMT", adminUserId, "Reason: " + reason);
        log.info("HCF {} rejected by Top Management {}: {}", hcfId, adminUserId, reason);

        if (hcf.getContactEmail() != null && !hcf.getContactEmail().isBlank()) {
            try {
                String html = emailService.getTemplates().hcfRejected(hcf.getName(), reason);
                emailService.sendHtmlEmail(hcf.getContactEmail(), "Registration Request Update - SmartCBWTF", html);
                log.info("Rejection email sent to HCF: {}", hcf.getContactEmail());
            } catch (Exception e) {
                log.warn("Failed to send rejection email to {}: {}", hcf.getContactEmail(), e.getMessage());
            }
        }
    }

    /**
     * Resubmit a rejected HCF. Only allowed when rejection count is 1.
     * Reuses CbwtfAdminHcfRegistrationRequest for updated payload.
     */
    @Transactional
    public HcfDetailDTO resubmitHcf(UUID hcfId, UUID facilityId, UUID adminUserId, CbwtfAdminHcfRegistrationRequest request) {
        Agreement agreement = agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("HCF not found"));

        Hcf hcf = agreement.getHcf();

        if (!"REJECTED".equals(hcf.getStatus()) || hcf.getRejectionCount() >= 2) {
            throw new IllegalStateException("HCF is not eligible for resubmission");
        }

        // Apply updates
        hcf.setName(request.getName());
        hcf.setAddress(request.getAddress());
        hcf.setPincode(request.getPincode());
        hcf.setState(request.getState());
        hcf.setCity(request.getCity());
        hcf.setDoctorName(request.getDoctorName());
        hcf.setContactPhone(request.getContactPhone());
        hcf.setContactEmail(request.getContactEmail());
        hcf.setPanNo(request.getPanNo());
        hcf.setGstNo(request.getGstNo());
        hcf.setAadharNo(request.getAadharNo());
        hcf.setOwnershipType(request.getOwnershipType());
        if (request.getRentAgreementUrl() != null) {
            hcf.setRentAgreementUrl(validateOptionalRentAgreementUrl(request.getRentAgreementUrl(), facilityId));
        } else if ("RENTED".equalsIgnoreCase(request.getOwnershipType())
                && (hcf.getRentAgreementUrl() == null || hcf.getRentAgreementUrl().isBlank())) {
            throw new IllegalArgumentException("Rent agreement document is required for rented properties");
        }
        hcf.setBedded(request.getBedded());
        hcf.setNumberOfBeds(request.getNumberOfBeds());
        hcf.setSeatCount(request.getSeatCount());
        hcf.setMonthlyCharges(request.getMonthlyCharges());
        hcf.setOccupancy(request.getOccupancy());
        hcf.setOtherNotes(request.getOtherNotes());
        hcf.setGpsLat(request.getGpsLat());
        hcf.setGpsLon(request.getGpsLon());
        hcf.setTaxRate(request.getTaxRate() != null ? request.getTaxRate() : 5.0);
        if (request.getExcessRatePerKg() != null) {
            hcf.setExcessRatePerKg(request.getExcessRatePerKg().doubleValue());
        }
        if (request.getHcfType() != null && !request.getHcfType().isBlank()) {
            try {
                hcf.setHcfType(com.smartcbwtf.domain.HcfType.valueOf(request.getHcfType()));
            } catch (IllegalArgumentException e) {
                hcf.setHcfType(com.smartcbwtf.domain.HcfType.HOSPITAL);
            }
        }

        hcf.recalculateBedAccessCategory();

        hcf.setStatus("PENDING_APPROVAL");
        hcf.setApprovalStatus(com.smartcbwtf.domain.ApprovalStatus.PENDING);
        hcf.setUpdatedAt(Instant.now());
        hcfRepository.save(hcf);

        // Update Agreement
        agreement.setStartDate(request.getAgreementStartDate());
        agreement.setEndDate(request.getAgreementEndDate());
        agreement.setPerBedPerDayRate(request.getPerBedPerDayRate() != null ? request.getPerBedPerDayRate() : BigDecimal.ZERO);
        agreement.setStatus("PENDING_APPROVAL");
        agreement.setUpdatedAt(Instant.now());
        agreementRepository.save(agreement);

        // Update Billing Config
        AgreementBillingConfig config = billingConfigRepository.findByAgreementIdOrderByEffectiveFromDesc(agreement.getId())
                .stream().findFirst().orElseThrow();
        config.setBaseRatePerBedPerDay(request.getPerBedPerDayRate() != null ? request.getPerBedPerDayRate() : BigDecimal.ZERO);
        config.setEffectiveFrom(request.getAgreementStartDate());
        billingConfigRepository.save(config);

        auditLogService.log("HCF", hcfId, "HCF_RESUBMITTED", adminUserId, "HCF resubmitted after correction");
        return getHcfDetail(hcfId, facilityId);
    }

    /**
     * Upload rent agreement document to local storage.
     * Returns the URL path to the uploaded file.
     */
    public String uploadRentAgreement(UUID facilityId, org.springframework.web.multipart.MultipartFile file) {
        String ext = UploadFileValidator.rentAgreementExtension(file);

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

        passwordPolicyValidator.validateOrThrow(newPassword);

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
