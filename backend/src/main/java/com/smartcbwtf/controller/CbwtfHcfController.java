package com.smartcbwtf.controller;

import com.smartcbwtf.dto.*;
import com.smartcbwtf.service.CbwtfHcfService;
import com.smartcbwtf.service.BillingConfigService;
import com.smartcbwtf.service.AgreementService;
import com.smartcbwtf.service.PdfService;
import com.smartcbwtf.service.UploadFileValidator;
import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.AgreementCorrectionRequest;
import com.smartcbwtf.repository.AgreementRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for CBWTF Admin HCF Management.
 *
 * All endpoints are tenant-scoped via TenantContext.
 * facility_id is NEVER accepted from request body.
 */
@RestController
@RequestMapping("/api/cbwtf/hcfs")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfHcfController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CbwtfHcfController.class);

    private final CbwtfHcfService hcfService;
    private final BillingConfigService billingConfigService;
    private final AgreementRepository agreementRepository;
    private final AgreementService agreementService;
    private final PdfService pdfService;

    public CbwtfHcfController(CbwtfHcfService hcfService, BillingConfigService billingConfigService,
            AgreementRepository agreementRepository, AgreementService agreementService, PdfService pdfService) {
        this.hcfService = hcfService;
        this.billingConfigService = billingConfigService;
        this.agreementRepository = agreementRepository;
        this.agreementService = agreementService;
        this.pdfService = pdfService;
    }

    /**
     * List all HCFs with active agreements for the current CBWTF.
     */
    @GetMapping
    public ResponseEntity<List<HcfListItemDTO>> listHcfs(
            @RequestParam(name = "limit", defaultValue = "500") int limit) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.listByFacility(facilityId, limit));
    }

    /**
     * Get HCF detail with agreement, billing config, and summary.
     */
    @GetMapping("/{id}")
    public ResponseEntity<HcfDetailDTO> getHcfDetail(@PathVariable("id") UUID id) {
        log.info("Controller request: getHcfDetail for ID: {}", id);
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.getHcfDetail(id, facilityId));
    }

    /**
     * Directly register a new HCF (admin action, no approval required).
     * Creates HCF, Agreement, and BillingConfig immediately.
     */
    @PostMapping
    public ResponseEntity<HcfDetailDTO> registerHcf(
            @Valid @RequestBody CbwtfAdminHcfRegistrationRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        // Get admin user ID from tenant context (set by security filter)
        UUID adminUserId = currentUserId();
        return ResponseEntity.ok(hcfService.registerHcfDirectly(facilityId, adminUserId, request));
    }

    /**
     * Save HCF registration as draft (no validation required).
     */
    @PostMapping("/draft")
    public ResponseEntity<HcfDetailDTO> saveDraft(
            @RequestBody CbwtfAdminHcfRegistrationRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        UUID adminUserId = currentUserId();
        return ResponseEntity.ok(hcfService.saveDraftDirectly(facilityId, adminUserId, request));
    }

    /**
     * List draft HCF registrations.
     */
    @GetMapping("/drafts")
    public ResponseEntity<List<HcfListItemDTO>> listDraftHcfs(
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.listDrafts(facilityId, limit));
    }

    /**
     * Upload rent agreement document (PDF or image).
     * Max file size: 20MB
     */
    @PostMapping(value = "/upload-rent-agreement", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadRentAgreement(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        UUID facilityId = TenantContext.getTenantId();

        try {
            UploadFileValidator.rentAgreementExtension(file);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", e.getMessage()));
        }

        String url = hcfService.uploadRentAgreement(facilityId, file);
        return ResponseEntity.ok(java.util.Map.of("url", url));
    }

    /**
     * Update HCF profile (name, email, phone, address).
     * Audit logged.
     */
    @PutMapping("/{id}")
    public ResponseEntity<HcfDetailDTO> updateHcf(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateHcfRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.updateHcf(id, facilityId, request));
    }

    /**
     * Update HCF registered location.
     * Only allowed if agreement is ACTIVE.
     * Audit logged.
     */
    @PutMapping("/{id}/location")
    public ResponseEntity<HcfDetailDTO> updateLocation(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateLocationRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.updateLocation(id, facilityId, request));
    }

    /**
     * Deactivate HCF by expiring/terminating its agreement.
     * Audit logged.
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateHcf(
            @PathVariable("id") UUID id,
            @Valid @RequestBody DeactivateHcfRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        hcfService.deactivate(id, facilityId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current billing configuration for HCF's agreement.
     */
    @GetMapping("/{id}/billing")
    public ResponseEntity<HcfDetailDTO.BillingConfigInfo> getBillingConfig(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(billingConfigService.getCurrentConfig(id, facilityId));
    }

    /**
     * Update billing configuration (creates new, expires previous).
     * Only allowed if agreement is ACTIVE.
     * Audit logged.
     */
    @PutMapping("/{id}/billing")
    public ResponseEntity<HcfDetailDTO.BillingConfigInfo> updateBillingConfig(
            @PathVariable("id") UUID id,
            @Valid @RequestBody BillingConfigRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(billingConfigService.createConfig(id, facilityId, request));
    }

    @PutMapping("/{id}/billing-model")
    public ResponseEntity<HcfListItemDTO> updatePendingBillingModel(
            @PathVariable("id") UUID id,
            @Valid @RequestBody HcfUpdateRequest request) {
        request.validate();
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.updatePendingBillingModel(id, facilityId, request));
    }

    /**
     * List HCFs pending approval (registered via Android app).
     */
    @GetMapping("/pending")
    public ResponseEntity<List<HcfListItemDTO>> listPendingHcfs(
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.listPending(facilityId, limit));
    }

    /**
     * Approve a pending HCF registration.
     * Creates agreement + default billing config.
     * Audit logged.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<HcfDetailDTO> approveHcf(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CbwtfHcfApprovalRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.approveHcf(id, facilityId, request));
    }

    /**
     * Reject a pending HCF registration.
     * Audit logged with reason.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectHcf(
            @PathVariable("id") UUID id,
            @Valid @RequestBody HcfRejectionRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        hcfService.rejectHcf(id, facilityId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resubmit a rejected HCF.
     */
    @PostMapping("/{id}/resubmit")
    public ResponseEntity<HcfDetailDTO> resubmitHcf(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CbwtfAdminHcfRegistrationRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        UUID adminUserId = currentUserId();
        return ResponseEntity.ok(hcfService.resubmitHcf(id, facilityId, adminUserId, request));
    }

    /**
     * Renew an expired agreement by creating a NEW agreement.
     * Old agreement remains immutable.
     */
    @PostMapping("/{id}/agreements/renew")
    public ResponseEntity<HcfDetailDTO> renewAgreement(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RenewAgreementRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.renewAgreement(id, facilityId, request));
    }

    /**
     * Get HCF portal admin user info.
     * Only for HCFs with 30+ beds (portal eligible).
     */
    @GetMapping("/{id}/portal-admin")
    public ResponseEntity<?> getPortalAdmin(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.getPortalAdminInfo(id, facilityId));
    }

    @PostMapping("/{id}/portal-admin/create")
    public ResponseEntity<?> createPortalAdmin(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return privateCredentialResponse(hcfService.createPortalAdmin(id, facilityId));
    }

    /**
     * Reset HCF portal admin password.
     * Only for HCFs with 30+ beds (portal eligible).
     */
    @PostMapping("/{id}/portal-admin/reset-password")
    public ResponseEntity<?> resetPortalAdminPassword(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ResetPasswordRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        return privateCredentialResponse(hcfService.resetPortalAdminPassword(id, facilityId, request.newPassword));
    }

    public static class ResetPasswordRequest {
        @NotBlank(message = "newPassword is required")
        @Size(min = 8, max = 128, message = "newPassword must be between 8 and 128 characters")
        public String newPassword;
    }

    /**
     * Enable portal access for a small HCF (0-30 beds).
     * Creates HCF admin user with generated password.
     * This is a manual override for HCFs that don't automatically qualify.
     */
    @PostMapping("/{id}/enable-portal-access")
    public ResponseEntity<?> enablePortalAccess(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return privateCredentialResponse(hcfService.enablePortalAccessForSmallHcf(id, facilityId));
    }

    /**
     * Download agreement PDF for a specific HCF.
     * CBWTF Admin can download agreement PDFs for any of their HCFs.
     */
    @GetMapping("/{id}/agreement/pdf")
    public ResponseEntity<Resource> downloadAgreementPdf(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();

        // Find active or upcoming agreement for this HCF under this facility
        Agreement agreement = agreementRepository.findActiveOrUpcomingByHcfAndFacility(id, facilityId)
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
                String safeNumber = PdfService.safeDownloadToken(agreement.getAgreementNumber());
                String filename = "Agreement_" + safeNumber + ".pdf";
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .body(new FileSystemResource(filePath));
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Download printable (letterhead) agreement PDF for a specific HCF.
     * Omits header/footer branding for printing on pre-printed letterhead.
     * Includes declaration and signature blocks.
     */
    @GetMapping("/{id}/agreement/print-pdf")
    public ResponseEntity<Resource> downloadAgreementPrintPdf(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();

        Agreement agreement = agreementRepository.findActiveOrUpcomingByHcfAndFacility(id, facilityId)
                .orElse(null);

        if (agreement == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String pdfPath = agreementService.generatePrintPdf(agreement);

            Path filePath = pdfService.storedGeneratedFilePath(pdfPath);
            if (Files.exists(filePath) && Files.isRegularFile(filePath) && Files.isReadable(filePath)) {
                String safeNumber = PdfService.safeDownloadToken(agreement.getAgreementNumber());
                String filename = "Agreement_Print_" + safeNumber + ".pdf";
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .body(new FileSystemResource(filePath));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * View the uploaded rent agreement for a tenant-owned HCF.
     */
    @GetMapping("/{id}/rent-agreement")
    public ResponseEntity<Resource> downloadRentAgreement(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        HcfDetailDTO detail = hcfService.getHcfDetail(id, facilityId);
        return rentAgreementResponse(detail.getRentAgreementUrl(), id);
    }

    /**
     * Submit an agreement correction request.
     */
    @PostMapping("/{id}/agreement/correction-request")
    public ResponseEntity<Void> submitCorrectionRequest(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AgreementCorrectionRequestDTO request) {
        UUID facilityId = TenantContext.getTenantId();
        UUID adminUserId = currentUserId();

        hcfService.submitCorrectionRequest(facilityId, id, request, adminUserId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get Correction Requests for an HCF
     */
    @GetMapping("/{id}/agreement/correction-requests")
    public ResponseEntity<List<AgreementCorrectionRequest>> getCorrectionRequests(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.getCorrectionRequests(facilityId, id));
    }

    private UUID currentUserId() {
        UUID userId = TenantContext.getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated user context");
        }
        return userId;
    }

    private ResponseEntity<Map<String, Object>> privateCredentialResponse(Map<String, Object> body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    private ResponseEntity<Resource> rentAgreementResponse(String rentAgreementUrl, UUID hcfId) {
        if (rentAgreementUrl == null || rentAgreementUrl.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path filePath = UploadFileValidator.uploadedAssetPath(rentAgreementUrl, "/uploads/rent-agreements/");
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {
                return ResponseEntity.notFound().build();
            }
            String filename = "Rent_Agreement_" + hcfId + extensionFor(filePath);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(mediaTypeFor(filePath))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new FileSystemResource(filePath));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private MediaType mediaTypeFor(Path filePath) {
        try {
            String contentType = Files.probeContentType(filePath);
            return contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String extensionFor(Path filePath) {
        String filename = filePath.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
