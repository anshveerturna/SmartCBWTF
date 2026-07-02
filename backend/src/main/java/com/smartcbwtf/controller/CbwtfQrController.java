package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.QrAuthorization;
import com.smartcbwtf.dto.*;
import com.smartcbwtf.service.PdfService;
import com.smartcbwtf.service.QrAuthorizationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * QR Authorization Controller for CBWTF Admin Portal.
 * 
 * ACCESS: CBWTF_ADMIN only
 * All operations are tenant-scoped to the logged-in admin's facility.
 */
@RestController
@RequestMapping("/api/cbwtf/qr")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfQrController {

    private static final Logger log = LoggerFactory.getLogger(CbwtfQrController.class);

    private final QrAuthorizationService qrService;
    private final PdfService pdfService;

    public CbwtfQrController(QrAuthorizationService qrService, PdfService pdfService) {
        this.qrService = qrService;
        this.pdfService = pdfService;
    }

    /**
     * Generate a new QR code for waste pickup authorization.
     * 
     * Validations:
     * - Agreement must be ACTIVE
     * - Validity period must be within agreement period
     * - No overlapping QR for same agreement + category + period
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateQr(@Valid @RequestBody QrGenerateRequest request) {
        try {
            UUID userId = TenantContext.getUserId();

            QrAuthorizationService.QrGenerateResult result = qrService.generateQr(
                    request.getHcfId(),
                    request.getWasteCategory(),
                    request.getValidFrom(),
                    request.getValidTo(),
                    userId);

            log.info("QR generated: {} by user {}", result.qrId(), userId);

            return ResponseEntity.ok(new QrGenerateResponse(result.qrId(), result.qrPayloadJson()));
        } catch (IllegalArgumentException e) {
            log.warn("QR generation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("QR generation error", e);
            return ResponseEntity.internalServerError()
                    .body(java.util.Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Get QR details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<QrDetailDTO> getQr(@PathVariable("id") UUID id) {
        return qrService.getQr(id)
                .map(qr -> ResponseEntity.ok(QrDetailDTO.from(qr)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List QRs with optional filters.
     * 
     * @param hcfId  Optional: filter by HCF
     * @param status Optional: filter by status (ACTIVE, USED, VERIFIED, etc.)
     */
    @GetMapping
    public ResponseEntity<List<QrDetailDTO>> listQrs(
            @RequestParam(name = "hcfId", required = false) UUID hcfId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {

        List<QrDetailDTO> qrs = qrService.listQrs(hcfId, status, limit)
                .stream()
                .map(QrDetailDTO::from)
                .toList();

        return ResponseEntity.ok(qrs);
    }

    /**
     * Manually revoke a QR code.
     */
    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revokeQr(
            @PathVariable("id") UUID id,
            @RequestParam(name = "reason", required = false) String reason) {

        UUID userId = TenantContext.getUserId();
        qrService.revokeQr(id, userId, reason);

        log.info("QR {} revoked by user {}", id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Download a single QR label as PDF — uses the exact same layout as batch label
     * PDF.
     */
    @GetMapping("/{id}/label-pdf")
    public ResponseEntity<Resource> downloadLabelPdf(@PathVariable("id") UUID id) {
        QrAuthorization qr = qrService.getQr(id)
                .orElseThrow(() -> new IllegalArgumentException("QR not found"));

        String pdfUrl = pdfService.generateSingleLabelPdf(
                qr.getHcf(), qr.getFacility(), qr.getWasteCategory(), qr.getQrPayload(),
                java.time.LocalDate.ofInstant(qr.getValidTo(), java.time.ZoneId.of("UTC")));

        Path file = pdfService.generatedFilePath(pdfUrl);
        if (!Files.exists(file) || !Files.isRegularFile(file) || !Files.isReadable(file)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
