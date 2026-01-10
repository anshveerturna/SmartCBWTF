package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.QrAuthorization;
import com.smartcbwtf.repository.QrAuthorizationRepository;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.service.QrAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HCF QR Label Controller - HCF Admin portal endpoints for QR generation.
 * 
 * Security:
 * - Only HCF_ADMIN role can access
 * - HCF must pass bed count + approval requirements
 * - Operations scoped to authenticated HCF only
 */
@RestController
@RequestMapping("/api/hcf/qr-labels")
@PreAuthorize("hasRole('HCF_ADMIN')")
public class HcfQrLabelController {

        private static final Logger log = LoggerFactory.getLogger(HcfQrLabelController.class);

        private final QrAuthorizationService qrService;
        private final QrAuthorizationRepository qrRepo;
        private final HcfAccessGuard accessGuard;

        public HcfQrLabelController(QrAuthorizationService qrService,
                        QrAuthorizationRepository qrRepo,
                        HcfAccessGuard accessGuard) {
                this.qrService = qrService;
                this.qrRepo = qrRepo;
                this.accessGuard = accessGuard;
        }

        /**
         * Generate QR labels for waste collection.
         * 
         * @param request Contains wasteCategory, quantity, validityDays
         * @return List of generated QR IDs and payloads
         */
        @PostMapping("/generate")
        public ResponseEntity<?> generateLabels(@RequestBody GenerateLabelRequest request) {
                UUID hcfId = TenantContext.getHcfId();
                UUID userId = TenantContext.getUserId();

                // Enforce HCF access requirements
                accessGuard.assertPortalAccess(hcfId);

                // Validate request
                if (request.wasteCategory == null || request.wasteCategory.isBlank()) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", "INVALID_CATEGORY",
                                        "message", "Waste category is required"));
                }

                // Parse dates
                Instant validFrom;
                Instant validTo;
                try {
                        validFrom = request.validFrom != null ? Instant.parse(request.validFrom) : Instant.now();
                        validTo = request.validTo != null ? Instant.parse(request.validTo)
                                        : validFrom.plus(7, java.time.temporal.ChronoUnit.DAYS);
                } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", "INVALID_DATE",
                                        "message",
                                        "Invalid date format. Use ISO-8601 format (e.g., 2026-01-15T00:00:00Z)"));
                }

                // Validate date range
                if (validTo.isBefore(validFrom)) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", "INVALID_DATE_RANGE",
                                        "message", "Valid To date must be after Valid From date"));
                }

                try {
                        // Generate single QR label
                        QrAuthorizationService.QrGenerateResult result = qrService.generateQr(
                                        hcfId,
                                        request.wasteCategory.toUpperCase(),
                                        validFrom,
                                        validTo,
                                        userId);

                        log.info("HCF {} generated QR label for category {} from {} to {}",
                                        hcfId, request.wasteCategory, validFrom, validTo);

                        return ResponseEntity.ok(Map.of(
                                        "generated", 1,
                                        "label", Map.of(
                                                        "id", result.qrId().toString(),
                                                        "payload", result.qrPayloadJson()),
                                        "validFrom", validFrom.toString(),
                                        "validTo", validTo.toString()));

                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", "GENERATION_FAILED",
                                        "message", e.getMessage()));
                } catch (Exception e) {
                        log.error("QR generation failed for HCF {}: {}", hcfId, e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                                        "error", "GENERATION_ERROR",
                                        "message", "Failed to generate QR label"));
                }
        }

        /**
         * List QR labels for the authenticated HCF.
         */
        @GetMapping
        public ResponseEntity<?> listLabels(
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String category) {

                UUID hcfId = TenantContext.getHcfId();
                accessGuard.assertPortalAccess(hcfId);

                List<QrAuthorization> labels = qrService.listQrs(hcfId, status);

                // Filter by category if specified
                if (category != null && !category.isBlank()) {
                        labels = labels.stream()
                                        .filter(qr -> category.equalsIgnoreCase(qr.getWasteCategory()))
                                        .toList();
                }

                return ResponseEntity.ok(Map.of(
                                "labels", labels.stream().map(qr -> Map.of(
                                                "id", qr.getId().toString(),
                                                "wasteCategory", qr.getWasteCategory(),
                                                "status", qr.getStatus(),
                                                "validFrom", qr.getValidFrom().toString(),
                                                "validTo", qr.getValidTo().toString(),
                                                "createdAt", qr.getCreatedAt().toString(),
                                                "qrPayload", qr.getQrPayload(),
                                                "isActive", qr.isActive(),
                                                "isUsable", qr.isUsable())).toList(),
                                "total", labels.size()));
        }

        /**
         * Get QR label details.
         */
        @GetMapping("/{id}")
        public ResponseEntity<?> getLabel(@PathVariable UUID id) {
                UUID hcfId = TenantContext.getHcfId();
                accessGuard.assertPortalAccess(hcfId);

                return qrRepo.findById(id)
                                .filter(qr -> qr.getHcf().getId().equals(hcfId))
                                .map(qr -> ResponseEntity.ok(Map.of(
                                                "id", qr.getId().toString(),
                                                "wasteCategory", qr.getWasteCategory(),
                                                "status", qr.getStatus(),
                                                "validFrom", qr.getValidFrom().toString(),
                                                "validTo", qr.getValidTo().toString(),
                                                "createdAt", qr.getCreatedAt().toString(),
                                                "qrPayload", qr.getQrPayload(),
                                                "isActive", qr.isActive(),
                                                "isUsable", qr.isUsable())))
                                .orElse(ResponseEntity.notFound().build());
        }

        /**
         * Get available waste categories.
         */
        @GetMapping("/categories")
        public ResponseEntity<?> getCategories() {
                return ResponseEntity.ok(Map.of(
                                "categories", List.of(
                                                Map.of("code", "YELLOW", "name", "Infectious Waste", "color",
                                                                "#FFEB3B"),
                                                Map.of("code", "RED", "name", "Contaminated Recyclables", "color",
                                                                "#F44336"),
                                                Map.of("code", "BLUE", "name", "Glassware Waste", "color", "#2196F3"),
                                                Map.of("code", "WHITE", "name", "Sharps Waste", "color", "#FFFFFF"))));
        }

        /**
         * Deactivate (revoke) a QR label.
         */
        @PutMapping("/{id}/deactivate")
        public ResponseEntity<?> deactivateLabel(@PathVariable UUID id) {
                UUID hcfId = TenantContext.getHcfId();
                accessGuard.assertPortalAccess(hcfId);

                return qrRepo.findById(id)
                                .filter(qr -> qr.getHcf().getId().equals(hcfId))
                                .map(qr -> {
                                        if (!qr.isActive()) {
                                                return ResponseEntity.badRequest().body(Map.of(
                                                                "error", "ALREADY_INACTIVE",
                                                                "message", "QR label is already inactive"));
                                        }
                                        qr.setStatus("REVOKED");
                                        qrRepo.save(qr);
                                        log.info("HCF {} deactivated QR label {}", hcfId, id);
                                        return ResponseEntity.ok(Map.of(
                                                        "success", true,
                                                        "message", "QR label deactivated successfully"));
                                })
                                .orElse(ResponseEntity.notFound().build());
        }

        // Request DTOs
        public static class GenerateLabelRequest {
                public String wasteCategory;
                public String validFrom;
                public String validTo;
        }
}
