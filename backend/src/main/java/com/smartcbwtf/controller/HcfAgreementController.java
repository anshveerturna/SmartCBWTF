package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.service.AgreementService;
import com.smartcbwtf.service.PdfService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller for HCF Agreement page - read-only view of active agreement.
 */
@RestController
@RequestMapping("/api/hcf/agreement")
@PreAuthorize("hasRole('HCF_ADMIN')")
public class HcfAgreementController {

    private final AgreementRepository agreementRepository;
    private final HcfAccessGuard accessGuard;
    private final AgreementService agreementService;
    private final PdfService pdfService;

    public HcfAgreementController(AgreementRepository agreementRepository, HcfAccessGuard accessGuard,
            AgreementService agreementService, PdfService pdfService) {
        this.agreementRepository = agreementRepository;
        this.accessGuard = accessGuard;
        this.agreementService = agreementService;
        this.pdfService = pdfService;
    }

    @GetMapping
    public ResponseEntity<HcfAgreementDTO> getAgreement() {
        UUID hcfId = TenantContext.getHcfId();
        UUID facilityId = TenantContext.getTenantId();
        accessGuard.assertPortalAccess(hcfId, facilityId);

        return agreementRepository.findActiveByHcfAndFacility(hcfId, facilityId)
                .map(agreement -> ResponseEntity.ok(HcfAgreementDTO.from(agreement)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pdf")
    public ResponseEntity<Resource> downloadPdf() {
        UUID hcfId = TenantContext.getHcfId();
        UUID facilityId = TenantContext.getTenantId();
        accessGuard.assertPortalAccess(hcfId, facilityId);

        Agreement agreement = agreementRepository.findActiveByHcfAndFacility(hcfId, facilityId)
                .orElse(null);

        if (agreement == null) {
            return ResponseEntity.notFound().build();
        }

        // Always regenerate fresh PDF with latest branding/settings
        agreement = agreementService.regeneratePdf(agreement);

        if (agreement.getPdfUrl() == null || agreement.getPdfUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = pdfService.storedGeneratedFilePath(agreement.getPdfUrl());
            if (Files.exists(filePath) && Files.isRegularFile(filePath) && Files.isReadable(filePath)) {
                String filename = "Agreement_" + PdfService.safeDownloadToken(agreement.getAgreementNumber()) + ".pdf";
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .body(new FileSystemResource(filePath));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DTO for HCF Agreement details.
     */
    public record HcfAgreementDTO(
            UUID id,
            String agreementNumber,
            String status,
            String duesStatus,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal perBedPerDayRate,
            Integer version,
            Boolean termsAccepted,
            String termsVersion,
            Instant termsAcceptedAt,
            Instant createdAt,
            // Facility (CBWTF) info
            String facilityCode,
            String facilityName,
            String facilityAddress,
            String facilityEmail,
            String facilityPhone,
            // HCF info
            String hcfCode,
            String hcfName,
            String hcfAddress,
            String hcfState,
            String hcfPincode,
            Integer hcfBeds,
            String billingModel,
            // PDF availability
            boolean pdfAvailable) {
        public static HcfAgreementDTO from(Agreement agreement) {
            Facility facility = agreement.getFacility();
            Hcf hcf = agreement.getHcf();

            return new HcfAgreementDTO(
                    agreement.getId(),
                    agreement.getAgreementNumber(),
                    agreement.getStatus(),
                    agreement.getDuesStatus(),
                    agreement.getStartDate(),
                    agreement.getEndDate(),
                    agreement.getPerBedPerDayRate(),
                    agreement.getVersion(),
                    agreement.getTermsAccepted(),
                    agreement.getTermsVersion(),
                    agreement.getTermsAcceptedAt(),
                    agreement.getCreatedAt(),
                    // Facility (CBWTF)
                    facility != null ? facility.getCode() : null,
                    facility != null ? facility.getName() : null,
                    facility != null ? facility.getAddress() : null,
                    facility != null ? facility.getContactEmail() : null,
                    facility != null ? facility.getContactPhone() : null,
                    // HCF
                    hcf != null ? hcf.getCode() : null,
                    hcf != null ? hcf.getName() : null,
                    hcf != null ? hcf.getAddress() : null,
                    hcf != null ? hcf.getState() : null,
                    hcf != null ? hcf.getPincode() : null,
                    hcf != null ? hcf.getNumberOfBeds() : null,
                    hcf != null && hcf.getBillingModel() != null ? hcf.getBillingModel().name() : null,
                    // PDF
                    agreement.getPdfUrl() != null && !agreement.getPdfUrl().isBlank());
        }
    }
}
