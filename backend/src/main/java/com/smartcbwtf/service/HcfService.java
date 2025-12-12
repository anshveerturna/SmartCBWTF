package com.smartcbwtf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.dto.HcfRegistrationRequest;
import com.smartcbwtf.dto.HcfRegistrationResponse;
import com.smartcbwtf.repository.*;
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
            ObjectMapper objectMapper) {
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
        
        return hcf;
    }

    private Agreement createAgreement(HcfRegistrationRequest request, Hcf hcf, Facility facility,
                                       String agreementNumber, String termsVersion, AppUser acceptedBy) {
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber(agreementNumber);
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        
        // Set dates
        LocalDate startDate = request.getAgreementStartDate() != null ? 
                request.getAgreementStartDate() : LocalDate.now();
        LocalDate endDate = request.getAgreementEndDate() != null ? 
                request.getAgreementEndDate() : startDate.plusYears(1);
        
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
            // Log error but don't fail registration
            e.printStackTrace();
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
            // Log but don't fail
            e.printStackTrace();
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
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
}
