package com.smartcbwtf.controller;

import com.smartcbwtf.dto.*;
import com.smartcbwtf.service.CbwtfHcfService;
import com.smartcbwtf.service.BillingConfigService;
import com.smartcbwtf.service.AgreementService;
import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.repository.AgreementRepository;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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

    public CbwtfHcfController(CbwtfHcfService hcfService, BillingConfigService billingConfigService,
            AgreementRepository agreementRepository, AgreementService agreementService) {
        this.hcfService = hcfService;
        this.billingConfigService = billingConfigService;
        this.agreementRepository = agreementRepository;
        this.agreementService = agreementService;
    }

    /**
     * List all HCFs with active agreements for the current CBWTF.
     */
    @GetMapping
    public ResponseEntity<List<HcfListItemDTO>> listHcfs() {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.listByFacility(facilityId));
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
        UUID adminUserId = TenantContext.getUserId();
        if (adminUserId == null) {
            log.warn("Could not extract user ID from tenant context, using fallback");
            adminUserId = UUID.randomUUID();
        }
        return ResponseEntity.ok(hcfService.registerHcfDirectly(facilityId, adminUserId, request));
    }

    /**
     * Upload rent agreement document (PDF or image).
     * Max file size: 20MB
     */
    @PostMapping(value = "/upload-rent-agreement", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadRentAgreement(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        UUID facilityId = TenantContext.getTenantId();

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("application/pdf") &&
                        !contentType.startsWith("image/"))) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "Only PDF and image files are allowed"));
        }

        // Validate file size (20MB max)
        if (file.getSize() > 20 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "File size cannot exceed 20MB"));
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

    /**
     * List HCFs pending approval (registered via Android app).
     */
    @GetMapping("/pending")
    public ResponseEntity<List<HcfListItemDTO>> listPendingHcfs() {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.listPending(facilityId));
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
            @RequestBody HcfRejectionRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        hcfService.rejectHcf(id, facilityId, request);
        return ResponseEntity.noContent().build();
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
        return ResponseEntity.ok(hcfService.createPortalAdmin(id, facilityId));
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
        return ResponseEntity.ok(hcfService.resetPortalAdminPassword(id, facilityId, request.newPassword));
    }

    public static class ResetPasswordRequest {
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
        return ResponseEntity.ok(hcfService.enablePortalAccessForSmallHcf(id, facilityId));
    }

    /**
     * Download agreement PDF for a specific HCF.
     * CBWTF Admin can download agreement PDFs for any of their HCFs.
     */
    @GetMapping("/{id}/agreement/pdf")
    public ResponseEntity<Resource> downloadAgreementPdf(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();

        // Find active agreement for this HCF under this facility
        Agreement agreement = agreementRepository.findActiveByHcfAndFacility(id, facilityId)
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
            String pdfUrl = agreement.getPdfUrl();
            Path filePath;
            if (pdfUrl.startsWith("/")) {
                filePath = Paths.get(pdfUrl);
            } else {
                filePath = Paths.get(pdfUrl.replaceFirst("^/", ""));
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String safeNumber = agreement.getAgreementNumber().replace("/", "_");
                String filename = "Agreement_" + safeNumber + ".pdf";
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
