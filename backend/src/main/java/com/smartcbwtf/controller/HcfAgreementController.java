package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.service.AgreementService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public HcfAgreementController(AgreementRepository agreementRepository, HcfAccessGuard accessGuard,
            AgreementService agreementService) {
        this.agreementRepository = agreementRepository;
        this.accessGuard = accessGuard;
        this.agreementService = agreementService;
    }

    @GetMapping
    public ResponseEntity<HcfAgreementDTO> getAgreement() {
        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        // Use same pattern as HcfDuesClearanceController for consistency
        java.util.List<Agreement> agreements = agreementRepository.findByHcfIdAndStatus(
                hcfId, Agreement.Status.ACTIVE.name());

        if (agreements.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(HcfAgreementDTO.from(agreements.get(0)));
    }

    @GetMapping("/pdf")
    public ResponseEntity<Resource> downloadPdf() {
        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        Agreement agreement = agreementRepository.findActiveByHcfId(hcfId)
                .orElse(null);

        if (agreement == null) {
            return ResponseEntity.notFound().build();
        }

        // Lazy regeneration: if PDF is missing, generate it now
        agreement = agreementService.regeneratePdfIfMissing(agreement);

        if (agreement.getPdfUrl() == null || agreement.getPdfUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Handle both absolute paths and relative paths (legacy)
            String pdfUrl = agreement.getPdfUrl();
            Path filePath;
            if (pdfUrl.startsWith("/")) {
                filePath = Paths.get(pdfUrl);
            } else {
                filePath = Paths.get(pdfUrl.replaceFirst("^/", ""));
            }
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String filename = "Agreement_" + agreement.getAgreementNumber() + ".pdf";
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
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
