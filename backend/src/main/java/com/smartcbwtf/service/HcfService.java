package com.smartcbwtf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.dto.HcfRegistrationRequest;
import com.smartcbwtf.dto.HcfRegistrationResponse;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HcfService {

    private static final Logger log = LoggerFactory.getLogger(HcfService.class);

    private final HcfRepository hcfRepository;
    private final AgreementRepository agreementRepository;
    private final FacilityRepository facilityRepository;
    private final AppUserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AgreementNumberGeneratorService agreementNumberGenerator;
    private final FacilityTermsService facilityTermsService;
    private final FacilityTemplateService facilityTemplateService;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final double hcfProximityRadiusM;

    public HcfService(
            HcfRepository hcfRepository,
            AgreementRepository agreementRepository,
            FacilityRepository facilityRepository,
            AppUserRepository userRepository,
            AuditLogService auditLogService,
            AgreementNumberGeneratorService agreementNumberGenerator,
            FacilityTermsService facilityTermsService,
            FacilityTemplateService facilityTemplateService,
            PdfService pdfService,
            EmailService emailService,
            ObjectMapper objectMapper,
            @Value("${app.geofence.hcf-proximity-radius-m:25}") double hcfProximityRadiusM) {
        this.hcfRepository = hcfRepository;
        this.agreementRepository = agreementRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.agreementNumberGenerator = agreementNumberGenerator;
        this.facilityTermsService = facilityTermsService;
        this.facilityTemplateService = facilityTemplateService;
        this.pdfService = pdfService;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
        this.hcfProximityRadiusM = hcfProximityRadiusM;
    }

    /**
     * Register a new HCF with full agreement generation flow.
     * Validates GPS, terms acceptance, creates HCF and Agreement, generates PDF.
     */
    @Transactional
    public HcfRegistrationResponse register(HcfRegistrationRequest request) {
        // Validation
        validateRegistrationRequest(request);

        // Get facility (CBWTF)
        Facility facility = getFacility(request.getFacilityId());

        // Get registered by user (optional)
        AppUser registeredBy = null;
        if (request.getRegisteredByUserId() != null) {
            registeredBy = userRepository.findById(request.getRegisteredByUserId()).orElse(null);
        }

        // Create HCF
        Hcf hcf = createHcf(request, registeredBy);
        hcfRepository.save(hcf);

        // Get active terms for the facility
        FacilityTerms terms = facilityTermsService.getActiveTermsEntity(facility.getId()).orElse(null);
        String termsVersion = terms != null ? terms.getVersion() : "default";

        // Generate agreement number
        String agreementNumber = agreementNumberGenerator.generateNextAgreementNumber(facility);

        // Create Agreement
        Agreement agreement = createAgreement(request, hcf, facility, agreementNumber, termsVersion, registeredBy);

        // Get active template
        FacilityTemplate template = facilityTemplateService.getActiveTemplate(facility.getId()).orElse(null);
        String templateContent = null;
        if (template != null) {
            try {
                templateContent = facilityTemplateService.readTemplateContent(template);
            } catch (IOException e) {
                // Log but continue with default template
                templateContent = null;
            }
        }

        // Set template info on agreement
        if (template != null) {
            agreement.setTemplate(template);
            agreement.setTemplateVersion(template.getVersion());
        }

        agreementRepository.save(agreement);

        // Generate PDF
        String pdfUrl = pdfService.generateAgreementPdf(agreement, template, templateContent);
        agreement.setPdfUrl(pdfUrl);
        agreementRepository.save(agreement);

        // Create audit log
        createAuditLog(request, hcf, agreement, termsVersion);

        // Send email (non-blocking)
        sendRegistrationEmail(hcf, agreement, facility);

        // Build response
        HcfRegistrationResponse response = new HcfRegistrationResponse("PENDING_APPROVAL", hcf.getId(), hcf.getCode())
                .withAgreement(agreement.getId(), agreement.getAgreementNumber())
                .withPdfUrl(pdfUrl)
                .withMessage("HCF registered and agreement generated successfully");

        if (template != null) {
            response.withTemplate(new HcfRegistrationResponse.TemplateInfo(
                    template.getId(),
                    template.getVersion(),
                    facility.getId()));
        }

        return response;
    }

    /**
     * List HCFs with active agreements for mobile app attendance marking.
     * Returns HCFs with GPS coordinates for geofence validation.
     */
    public List<com.smartcbwtf.controller.HcfController.MobileHcfDto> listActiveHcfsForMobile(UUID facilityId) {
        List<Hcf> hcfs = agreementRepository.findHcfsByFacilityId(facilityId);
        return hcfs.stream()
                .filter(hcf -> hcf.getGpsLat() != null && hcf.getGpsLon() != null) // Only include HCFs with GPS
                .map(hcf -> new com.smartcbwtf.controller.HcfController.MobileHcfDto(
                        hcf.getId().toString(),
                        hcf.getName(),
                        hcf.getAddress(),
                        null, // city not in Hcf entity
                        hcf.getState(),
                        hcf.getPincode(),
                        hcf.getContactPhone(),
                        hcf.getGpsLat(),
                        hcf.getGpsLon(),
                        "ACTIVE".equals(hcf.getStatus())))
                .toList();
    }

    private void validateRegistrationRequest(HcfRegistrationRequest request) {
        // GPS validation
        if (request.getRegistrationGpsLat() == null || request.getRegistrationGpsLon() == null) {
            throw new IllegalArgumentException("GPS coordinates are required");
        }
        if (request.getRegistrationGpsAccuracy() == null) {
            throw new IllegalArgumentException("GPS accuracy is required");
        }

        // Terms acceptance validation
        if (!Boolean.TRUE.equals(request.getTermsAccepted())) {
            throw new IllegalArgumentException("Terms and Conditions must be accepted");
        }

        // Bedded validation
        if (Boolean.TRUE.equals(request.getBedded()) &&
                (request.getNumberOfBeds() == null || request.getNumberOfBeds() <= 0)) {
            throw new IllegalArgumentException("Number of beds is required for bedded facilities");
        }

        // Ownership validation: rent agreement required if RENTED
        if ("RENTED".equalsIgnoreCase(request.getOwnershipType()) &&
                (request.getRentAgreementUrl() == null || request.getRentAgreementUrl().isBlank())) {
            throw new IllegalArgumentException("Rent agreement document is required for rented premises");
        }

        // Mandatory fields checks
        if (request.getPincode() == null || request.getPincode().isBlank()) {
            throw new IllegalArgumentException("Pincode is required");
        }

        if (request.getState() == null || request.getState().isBlank()) {
            throw new IllegalArgumentException("State is required");
        }

        if (request.getContactEmail() == null || request.getContactEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (request.getPanNo() == null || request.getPanNo().isBlank()) {
            throw new IllegalArgumentException("PAN Number is required");
        }

        if (request.getGstNo() == null || request.getGstNo().isBlank()) {
            throw new IllegalArgumentException("GST Number is required");
        }

        if (request.getAadharNo() == null || request.getAadharNo().isBlank()) {
            throw new IllegalArgumentException("Aadhar Number is required");
        }

        // ============================================================================
        // ENTERPRISE DUPLICATE DETECTION - Agreement Status Aware
        // Only blocks registration if existing HCF has an ACTIVE agreement
        // HCFs with expired/terminated agreements can be re-registered with new CBWTF
        // ============================================================================

        validateNoDuplicateWithActiveAgreement(request);

        // GPS PROXIMITY CHECK - Anti-fraud detection
        validateGpsProximity(request);
    }

    /**
     * Validates that no HCF with matching government IDs has an active agreement.
     * This allows HCFs to switch CBWTFs after their agreement expires.
     */
    private void validateNoDuplicateWithActiveAgreement(HcfRegistrationRequest request) {
        // PAN Number check
        if (request.getPanNo() != null && !request.getPanNo().isBlank()) {
            hcfRepository.findByPanNoWithActiveAgreement(request.getPanNo())
                    .ifPresent(existing -> {
                        throw new com.smartcbwtf.exception.DuplicateHcfException(
                                "An HCF with PAN number " + maskIdentifier(request.getPanNo()) +
                                        " is already registered with an active service agreement. " +
                                        "Registration will be allowed once the existing agreement expires.",
                                "PAN",
                                existing.getCode());
                    });
        }

        // GST Number check
        if (request.getGstNo() != null && !request.getGstNo().isBlank()) {
            hcfRepository.findByGstNoWithActiveAgreement(request.getGstNo())
                    .ifPresent(existing -> {
                        throw new com.smartcbwtf.exception.DuplicateHcfException(
                                "An HCF with GST number " + maskIdentifier(request.getGstNo()) +
                                        " is already registered with an active service agreement. " +
                                        "Registration will be allowed once the existing agreement expires.",
                                "GST",
                                existing.getCode());
                    });
        }

        // Aadhar Number check
        if (request.getAadharNo() != null && !request.getAadharNo().isBlank()) {
            hcfRepository.findByAadharNoWithActiveAgreement(request.getAadharNo())
                    .ifPresent(existing -> {
                        throw new com.smartcbwtf.exception.DuplicateHcfException(
                                "An HCF with this Aadhar number is already registered with an active service agreement. "
                                        +
                                        "Registration will be allowed once the existing agreement expires.",
                                "AADHAR",
                                existing.getCode());
                    });
        }

        // Phone Number check
        if (request.getContactPhone() != null && !request.getContactPhone().isBlank()) {
            hcfRepository.findByContactPhoneWithActiveAgreement(request.getContactPhone())
                    .ifPresent(existing -> {
                        throw new com.smartcbwtf.exception.DuplicateHcfException(
                                "An HCF with phone number " + maskPhone(request.getContactPhone()) +
                                        " is already registered with an active service agreement. " +
                                        "Registration will be allowed once the existing agreement expires.",
                                "PHONE",
                                existing.getCode());
                    });
        }

        // Email check
        if (request.getContactEmail() != null && !request.getContactEmail().isBlank()) {
            hcfRepository.findByContactEmailWithActiveAgreement(request.getContactEmail())
                    .ifPresent(existing -> {
                        throw new com.smartcbwtf.exception.DuplicateHcfException(
                                "An HCF with email " + maskEmail(request.getContactEmail()) +
                                        " is already registered with an active service agreement. " +
                                        "Registration will be allowed once the existing agreement expires.",
                                "EMAIL",
                                existing.getCode());
                    });
        }
    }

    /**
     * Validates GPS proximity - prevents registration within configured radius of
     * existing HCFs with active agreements.
     * This is an anti-fraud measure to prevent same location being registered
     * with different identities.
     * Note: CBWTFs can adjust HCF location after registration if needed.
     * Radius is configurable via app.geofence.hcf-proximity-radius-m (default: 25m)
     */
    private void validateGpsProximity(HcfRegistrationRequest request) {
        List<com.smartcbwtf.domain.Hcf> nearbyHcfs = hcfRepository.findNearbyWithActiveAgreement(
                request.getRegistrationGpsLat(),
                request.getRegistrationGpsLon(),
                hcfProximityRadiusM);

        if (!nearbyHcfs.isEmpty()) {
            com.smartcbwtf.domain.Hcf nearest = nearbyHcfs.get(0);

            // Calculate exact distance for error message
            Double distance = hcfRepository.calculateDistance(
                    request.getRegistrationGpsLat(),
                    request.getRegistrationGpsLon(),
                    nearest.getGpsLat(),
                    nearest.getGpsLon());

            String distanceStr = distance != null ? String.format("%.1f", distance)
                    : "less than " + (int) hcfProximityRadiusM;

            throw new com.smartcbwtf.exception.DuplicateHcfException(
                    "Another healthcare facility (" + nearest.getName() + ") with an active service agreement " +
                            "is located " + distanceStr + " meters from this location. " +
                            "Please verify this is a different facility or contact support if the existing registration needs to be updated.",
                    "GPS_LOCATION",
                    nearest.getCode(),
                    distance);
        }
    }

    /**
     * Masks PAN/GST number for display (shows first 3 and last 2 characters).
     */
    private String maskIdentifier(String id) {
        if (id == null || id.length() < 6)
            return "***";
        return id.substring(0, 3) + "***" + id.substring(id.length() - 2);
    }

    /**
     * Masks phone number for display (shows last 4 digits).
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4)
            return "***";
        return "***" + phone.substring(phone.length() - 4);
    }

    /**
     * Masks email for display (shows first 2 chars + domain).
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@"))
            return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 2)
            return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    private Facility getFacility(UUID facilityId) {
        if (facilityId != null) {
            return facilityRepository.findById(facilityId)
                    .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));
        }
        // Get default facility if none specified
        return facilityRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No facility configured in the system"));
    }

    private Hcf createHcf(HcfRegistrationRequest request, AppUser registeredBy) {
        Hcf hcf = new Hcf();
        hcf.setCode("HCF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        hcf.setName(request.getName());
        hcf.setAddress(request.getAddress());
        hcf.setPincode(request.getPincode());
        hcf.setState(request.getState());
        hcf.setContactEmail(request.getContactEmail());
        hcf.setContactPhone(request.getContactPhone());
        hcf.setNumberOfBeds(request.getNumberOfBeds());
        hcf.setDoctorName(request.getDoctorName());
        hcf.setPanNo(request.getPanNo());
        hcf.setGstNo(request.getGstNo());
        hcf.setAadharNo(request.getAadharNo());
        hcf.setMonthlyCharges(request.getMonthlyCharges());
        hcf.setBedded(request.getBedded());
        hcf.setPcbAuthorizationNo(request.getPcbAuthorizationNo());
        hcf.setOtherNotes(request.getOtherNotes());

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

        // Ownership fields
        hcf.setOwnershipType(request.getOwnershipType() != null ? request.getOwnershipType() : "OWNED");
        hcf.setRentAgreementUrl(request.getRentAgreementUrl());

        // Use registration GPS as main GPS coordinates
        hcf.setGpsLat(request.getRegistrationGpsLat());
        hcf.setGpsLon(request.getRegistrationGpsLon());
        hcf.setRegistrationGpsLat(request.getRegistrationGpsLat());
        hcf.setRegistrationGpsLon(request.getRegistrationGpsLon());
        hcf.setRegistrationGpsAccuracy(request.getRegistrationGpsAccuracy());

        hcf.setRegisteredByUser(registeredBy);
        hcf.setStatus("PENDING_APPROVAL");
        hcf.setCreatedAt(Instant.now());
        hcf.setUpdatedAt(Instant.now());

        // Recalculate bed access category (considers hcfType)
        hcf.recalculateBedAccessCategory();

        return hcf;
    }

    private Agreement createAgreement(HcfRegistrationRequest request, Hcf hcf, Facility facility,
            String agreementNumber, String termsVersion, AppUser acceptedBy) {
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber(agreementNumber);
        agreement.setHcf(hcf);
        agreement.setFacility(facility);

        // Set dates
        LocalDate startDate = request.getAgreementStartDate() != null ? request.getAgreementStartDate()
                : LocalDate.now();
        LocalDate endDate = request.getAgreementEndDate() != null ? request.getAgreementEndDate()
                : startDate.plusYears(1);

        agreement.setStartDate(startDate);
        agreement.setEndDate(endDate);

        // Default rate (could be made configurable)
        agreement.setPerBedPerDayRate(BigDecimal.valueOf(150));

        // Terms acceptance
        agreement.setTermsAccepted(true);
        agreement.setTermsVersion(termsVersion);
        agreement.setTermsAcceptedAt(Instant.now());
        agreement.setTermsAcceptedBy(acceptedBy);

        agreement.setStatus("ACTIVE");
        agreement.setCreatedAt(Instant.now());
        agreement.setUpdatedAt(Instant.now());

        return agreement;
    }

    private void createAuditLog(HcfRegistrationRequest request, Hcf hcf, Agreement agreement, String termsVersion) {
        try {
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("hcfId", hcf.getId());
            auditData.put("hcfCode", hcf.getCode());
            auditData.put("agreementId", agreement.getId());
            auditData.put("agreementNumber", agreement.getAgreementNumber());
            auditData.put("termsVersion", termsVersion);
            auditData.put("termsAcceptedAt", Instant.now().toString());
            auditData.put("gpsLat", request.getRegistrationGpsLat());
            auditData.put("gpsLon", request.getRegistrationGpsLon());
            auditData.put("gpsAccuracy", request.getRegistrationGpsAccuracy());
            auditData.put("registeredByUserId", request.getRegisteredByUserId());

            String jsonData = objectMapper.writeValueAsString(auditData);
            String dataHash = sha256(jsonData);

            auditLogService.logWithData("HCF", hcf.getId(), "HCF_REGISTER",
                    request.getRegisteredByUserId(), jsonData, dataHash);
        } catch (Exception e) {
            log.error("Failed to write HCF registration audit log for {}", hcf.getId(), e);
        }
    }

    private void sendRegistrationEmail(Hcf hcf, Agreement agreement, Facility facility) {
        try {
            // Body for facility admin notification
            String body = String.format(
                    "Dear Admin,\n\n" +
                            "A new Health Care Facility has been registered.\n\n" +
                            "Agreement Number: %s\n" +
                            "HCF Name: %s\n" +
                            "Address: %s\n\n" +
                            "Please find the attached agreement document.\n\n" +
                            "Regards,\nSmartCBWTF System",
                    agreement.getAgreementNumber(),
                    hcf.getName(),
                    hcf.getAddress());

            if (hcf.getContactEmail() != null && !hcf.getContactEmail().isEmpty()) {
                // Use the new method that includes PDF attachment
                emailService.sendHcfRegistrationEmail(
                        hcf.getContactEmail(),
                        hcf.getName(),
                        agreement.getAgreementNumber(),
                        facility.getName(),
                        agreement.getPdfUrl());
            }

            // Also notify facility admin
            if (facility.getContactEmail() != null && !facility.getContactEmail().isEmpty()) {
                emailService.sendEmailWithAttachment(
                        facility.getContactEmail(),
                        "New HCF Registration - " + hcf.getName(),
                        body,
                        agreement.getPdfUrl());
            }
        } catch (Exception e) {
            log.warn("Failed to send HCF registration emails for {}", hcf.getId(), e);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Hcf> listPending() {
        return hcfRepository.findAll().stream()
                .filter(h -> "PENDING_APPROVAL".equalsIgnoreCase(h.getStatus()))
                .toList();
    }

    public List<Hcf> listAll() {
        return hcfRepository.findAll();
    }

    /**
     * Find HCF by ID.
     * 
     * @throws IllegalArgumentException if HCF not found
     */
    public Hcf findById(UUID hcfId) {
        return hcfRepository.findById(hcfId)
                .orElseThrow(() -> new IllegalArgumentException("HCF not found: " + hcfId));
    }
}
