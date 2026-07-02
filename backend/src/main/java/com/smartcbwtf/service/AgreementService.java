package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.FacilityBranding;
import com.smartcbwtf.domain.FacilitySettings;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.HcfApprovalRequest;
import com.smartcbwtf.dto.HcfApprovalResponse;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.FacilityBrandingRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.FacilitySettingsRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
public class AgreementService {

    private static final Logger log = LoggerFactory.getLogger(AgreementService.class);

    private final AgreementRepository agreementRepository;
    private final FacilityRepository facilityRepository;
    private final FacilityBrandingRepository brandingRepository;
    private final FacilitySettingsRepository settingsRepository;
    private final HcfRepository hcfRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public AgreementService(AgreementRepository agreementRepository,
            FacilityRepository facilityRepository,
            FacilityBrandingRepository brandingRepository,
            FacilitySettingsRepository settingsRepository,
            HcfRepository hcfRepository,
            PdfService pdfService,
            EmailService emailService,
            AuditLogService auditLogService) {
        this.agreementRepository = agreementRepository;
        this.facilityRepository = facilityRepository;
        this.brandingRepository = brandingRepository;
        this.settingsRepository = settingsRepository;
        this.hcfRepository = hcfRepository;
        this.pdfService = pdfService;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public HcfApprovalResponse approveHcf(UUID hcfId, HcfApprovalRequest request) {
        Hcf hcf = hcfRepository.findById(hcfId).orElseThrow();
        Facility facility = facilityRepository.findById(request.getFacilityId()).orElseThrow();

        String agreementNumber = generateAgreementNumber(facility.getCode());
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber(agreementNumber);
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setStartDate(request.getStartDate());
        agreement.setEndDate(request.getEndDate());
        agreement.setPerBedPerDayRate(request.getPerBedPerDayRate());
        agreement.setStatus("ACTIVE");

        // Set terms text from facility settings if available
        FacilitySettings settings = settingsRepository.findById(facility.getId()).orElse(null);
        if (settings != null && settings.getAgreementTermsTemplate() != null) {
            agreement.setTermsText(settings.getAgreementTermsTemplate());
        }

        agreementRepository.save(agreement);

        hcf.setStatus("ACTIVE");
        hcfRepository.save(hcf);

        // Generate professional agreement PDF with branding
        FacilityBranding branding = brandingRepository.findById(facility.getId()).orElse(null);
        String pdfPath = pdfService.generateAgreementPdf(agreement, null, null, branding, settings);
        agreement.setPdfUrl(pdfPath);
        agreementRepository.save(agreement);

        auditLogService.log("AGREEMENT", agreement.getId(), "APPROVE", null, null);

        // Send agreement email with PDF attachment
        try {
            java.util.Map<String, String> emailData = new java.util.HashMap<>();
            emailData.put("hcfName", hcf.getName());
            emailData.put("facilityName", facility.getName());
            emailData.put("agreementNumber", agreementNumber);
            emailData.put("effectiveDate", agreement.getStartDate().toString());
            emailData.put("expiryDate",
                    agreement.getEndDate() != null ? agreement.getEndDate().toString() : "Until Terminated");

            // Send with PDF attachment
            List<String> attachments = pdfPath != null ? List.of(pdfPath) : null;
            emailService.sendTemplateEmail(hcf.getContactEmail(), "AGREEMENT_APPROVED", emailData, attachments);

            log.info("[AGREEMENT] Sent agreement PDF to HCF email={} agreementNumber={}", hcf.getContactEmail(),
                    agreementNumber);

            // Also notify facility if configured
            if (facility.getContactEmail() != null) {
                emailService.sendEmail(facility.getContactEmail(), "Agreement Approved Notification",
                        "Agreement " + agreementNumber + " for " + hcf.getName() + " has been approved.");
            }
        } catch (Exception e) {
            // Log but don't fail transaction
            log.error("[AGREEMENT] Failed to send approval email for {}: {}", agreementNumber, e.getMessage());
            auditLogService.log("EMAIL", agreement.getId(), "SEND_FAILURE", null,
                    "Failed to send approval email: " + e.getMessage());
        }

        return new HcfApprovalResponse(hcf.getId(), hcf.getStatus(), agreementNumber);
    }

    private String generateAgreementNumber(String facilityCode) {
        long seq = agreementRepository.count() + 1;
        return facilityCode + "-" + Year.now().getValue() + "-" + String.format("%05d", seq);
    }

    /**
     * Regenerate agreement PDF if it's missing (null/blank pdfUrl or file doesn't
     * exist on disk).
     * Used for lazy PDF generation on first download for agreements created before
     * PDF generation existed.
     * Returns the (possibly updated) agreement.
     */
    @Transactional
    public Agreement regeneratePdfIfMissing(Agreement agreement) {
        // Check if PDF already exists on disk
        if (agreement.getPdfUrl() != null && !agreement.getPdfUrl().isBlank()) {
            try {
                if (Files.exists(pdfService.storedGeneratedFilePath(agreement.getPdfUrl()))) {
                    return agreement; // PDF exists, nothing to do
                }
            } catch (Exception e) {
                // Path invalid, regenerate
            }
        }
        return regeneratePdf(agreement);
    }

    /**
     * Always regenerate agreement PDF with latest data from branding/settings.
     * Used on every download to ensure the PDF reflects current facility details.
     */
    @Transactional
    public Agreement regeneratePdf(Agreement agreement) {
        log.info("[AGREEMENT] Regenerating fresh PDF for agreement={}", agreement.getAgreementNumber());
        FacilityBranding branding = brandingRepository.findById(agreement.getFacility().getId()).orElse(null);
        FacilitySettings settings = settingsRepository.findById(agreement.getFacility().getId()).orElse(null);

        // Update terms text from latest settings if available
        if (settings != null && settings.getAgreementTermsTemplate() != null) {
            agreement.setTermsText(settings.getAgreementTermsTemplate());
        }

        String pdfPath = pdfService.generateAgreementPdf(agreement, null, null, branding, settings);
        agreement.setPdfUrl(pdfPath);
        agreementRepository.save(agreement);
        log.info("[AGREEMENT] Fresh PDF generated at path={}", pdfPath);
        return agreement;
    }

    /**
     * Generate a printable (letterhead) agreement PDF — no header/footer branding,
     * includes declaration and signature blocks.
     * Returns the absolute path to the generated file.
     */
    public String generatePrintPdf(Agreement agreement) {
        log.info("[AGREEMENT] Generating print PDF for agreement={}", agreement.getAgreementNumber());
        FacilityBranding branding = brandingRepository.findById(agreement.getFacility().getId()).orElse(null);
        FacilitySettings settings = settingsRepository.findById(agreement.getFacility().getId()).orElse(null);

        // Update terms text from latest settings if available
        if (settings != null && settings.getAgreementTermsTemplate() != null) {
            agreement.setTermsText(settings.getAgreementTermsTemplate());
        }

        String pdfPath = pdfService.generatePrintableAgreementPdf(agreement, branding, settings);
        log.info("[AGREEMENT] Print PDF generated at path={}", pdfPath);
        return pdfPath;
    }
}
