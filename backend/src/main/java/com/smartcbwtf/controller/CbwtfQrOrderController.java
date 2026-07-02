package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.QrAuthorization;
import com.smartcbwtf.domain.QrAuthorization.WasteCategory;
import com.smartcbwtf.domain.QrLabelOrder;
import com.smartcbwtf.service.PdfService;
import com.smartcbwtf.service.QrOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * CBWTF QR Label Order Controller.
 * 
 * Shows QR requests alongside consumable orders for CBWTF admin.
 */
@RestController
@RequestMapping("/api/cbwtf/qr-orders")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfQrOrderController {

    private static final Logger log = LoggerFactory.getLogger(CbwtfQrOrderController.class);
    private static final int MAX_QR_QUANTITY_PER_CATEGORY = 100_000;
    private static final int MAX_WASTE_CATEGORIES_PER_ORDER = 4;
    private static final int MAX_REJECTION_REASON_LENGTH = 1000;

    private final QrOrderService qrOrderService;
    private final PdfService pdfService;

    public CbwtfQrOrderController(QrOrderService qrOrderService, PdfService pdfService) {
        this.qrOrderService = qrOrderService;
        this.pdfService = pdfService;
    }

    /**
     * CBWTF admin directly generates QR labels for an HCF.
     * No charge — admin-initiated generation with PDF download.
     */
    @PostMapping("/generate-for-hcf")
    public ResponseEntity<?> generateForHcf(@Valid @RequestBody GenerateForHcfRequest request) {
        try {
            UUID adminUserId = TenantContext.getUserId();
            UUID hcfId = requireHcfId(request.hcfId());
            Map<String, Integer> categoryQuantities = requireCategoryQuantities(request);

            int totalQty = categoryQuantities.values().stream().mapToInt(Integer::intValue).sum();

            var result = qrOrderService.adminDirectGenerateMulti(
                    hcfId, categoryQuantities, adminUserId, request.validUntil());

            log.info("CBWTF admin generated {} QR labels ({}) for HCF {}", totalQty, categoryQuantities.keySet(),
                    hcfId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", result.order().getId(),
                    "pdfUrl", result.pdfUrl(),
                    "quantity", result.qrCodes().size(),
                    "message", result.qrCodes().size() + " QR labels generated successfully"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Admin QR generation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Admin QR generation error", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal error: " + e.getMessage()));
        }
    }

    /**
     * List pending QR orders for this facility.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<QrOrderDTO>> listPendingOrders(
            @RequestParam(defaultValue = "100") int limit) {
        UUID facilityId = TenantContext.getTenantId();
        List<QrLabelOrder> orders = qrOrderService.listPendingOrders(facilityId, limit);
        return ResponseEntity.ok(orders.stream().map(QrOrderDTO::from).toList());
    }

    /**
     * List all QR orders (for history).
     */
    @GetMapping
    public ResponseEntity<List<QrOrderDTO>> listAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {
        UUID facilityId = TenantContext.getTenantId();
        List<QrLabelOrder> orders = qrOrderService.listAllOrders(facilityId, status, limit);
        return ResponseEntity.ok(orders.stream().map(QrOrderDTO::from).toList());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> downloadOrderPdf(@PathVariable("id") UUID id) {
        try {
            UUID facilityId = TenantContext.getTenantId();
            QrLabelOrder order = qrOrderService.getOrderPdfForFacility(id, facilityId);

            Path file = pdfService.generatedFilePath(order.getPdfUrl());
            if (!Files.exists(file) || !Files.isRegularFile(file) || !Files.isReadable(file)) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"QR-labels-" + order.getId() + ".pdf\"")
                    .body(new FileSystemResource(file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Fulfill a QR order - generates labels and charges HCF.
     */
    @PostMapping("/{id}/fulfill")
    public ResponseEntity<?> fulfillOrder(@PathVariable("id") UUID id) {
        try {
            UUID adminUserId = TenantContext.getUserId();
            UUID facilityId = TenantContext.getTenantId();
            QrLabelOrder fulfilled = qrOrderService.fulfillRequest(id, facilityId, adminUserId);

            log.info("QR order {} fulfilled by admin {}", id, adminUserId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", fulfilled.getId(),
                    "status", fulfilled.getStatus().name(),
                    "pdfUrl", fulfilled.getPdfUrl() != null ? fulfilled.getPdfUrl() : "",
                    "message", "Order fulfilled successfully. " + fulfilled.getQuantity() + " QR labels generated."));
        } catch (IllegalStateException e) {
            log.warn("QR order fulfill failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("QR order fulfill failed with unexpected error", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Reject a QR order.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectOrder(
            @PathVariable("id") UUID id,
            @Valid @RequestBody(required = false) RejectRequest request) {
        try {
            UUID adminUserId = TenantContext.getUserId();
            UUID facilityId = TenantContext.getTenantId();
            String reason = request != null ? trimToDefault(request.reason(), "No reason provided")
                    : "No reason provided";

            QrLabelOrder rejected = qrOrderService.rejectRequest(id, facilityId, adminUserId, reason);

            log.info("QR order {} rejected by admin {}: {}", id, adminUserId, reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", rejected.getId(),
                    "status", rejected.getStatus().name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get QR pricing info.
     */
    @GetMapping("/pricing")
    public ResponseEntity<QrOrderService.QrPricing> getPricing() {
        return ResponseEntity.ok(qrOrderService.getPricing());
    }

    private static UUID requireHcfId(UUID hcfId) {
        if (hcfId == null) {
            throw new IllegalArgumentException("HCF ID is required");
        }
        return hcfId;
    }

    private static Map<String, Integer> requireCategoryQuantities(GenerateForHcfRequest request) {
        Map<String, Integer> categoryQuantities = new LinkedHashMap<>();
        if (request.categoryQuantities() != null && !request.categoryQuantities().isEmpty()) {
            request.categoryQuantities().forEach((category, quantity) ->
                    addCategoryQuantity(categoryQuantities, category, quantity));
        } else if (request.wasteCategory() != null || request.quantity() != null) {
            addCategoryQuantity(categoryQuantities, request.wasteCategory(), request.quantity());
        }

        if (categoryQuantities.isEmpty()) {
            throw new IllegalArgumentException("At least one category with quantity > 0 required");
        }
        return categoryQuantities;
    }

    private static void addCategoryQuantity(Map<String, Integer> categoryQuantities, String category, Integer quantity) {
        String normalizedCategory = requireWasteCategory(category);
        int normalizedQuantity = requireQuantity(quantity);
        categoryQuantities.merge(normalizedCategory, normalizedQuantity, Integer::sum);
    }

    private static String requireWasteCategory(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Waste category is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            WasteCategory.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Waste category must be one of YELLOW, RED, BLUE, WHITE");
        }
    }

    private static int requireQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > MAX_QR_QUANTITY_PER_CATEGORY) {
            throw new IllegalArgumentException("Quantity is too large");
        }
        return quantity;
    }

    private static String trimToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    // DTOs
    public record RejectRequest(@Size(max = MAX_REJECTION_REASON_LENGTH) String reason) {
    }

    public record GenerateForHcfRequest(
            @NotNull UUID hcfId,
            @Pattern(regexp = QrAuthorization.WASTE_CATEGORY_PATTERN, message = "must be one of YELLOW, RED, BLUE, WHITE") String wasteCategory,
            @Positive @Max(MAX_QR_QUANTITY_PER_CATEGORY) Integer quantity,
            @Valid @Size(max = MAX_WASTE_CATEGORIES_PER_ORDER) Map<@Pattern(regexp = QrAuthorization.WASTE_CATEGORY_PATTERN, message = "must be one of YELLOW, RED, BLUE, WHITE") String, @NotNull @Positive @Max(MAX_QR_QUANTITY_PER_CATEGORY) Integer> categoryQuantities,
            @FutureOrPresent LocalDate validUntil) {
    }

    public record QrOrderDTO(
            UUID id,
            UUID hcfId,
            String hcfName,
            String hcfCode,
            String wasteCategory,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            String orderType,
            String status,
            String notes,
            Instant requestedAt,
            Instant fulfilledAt,
            String pdfUrl) {
        public static QrOrderDTO from(QrLabelOrder order) {
            return new QrOrderDTO(
                    order.getId(),
                    order.getHcf().getId(),
                    order.getHcf().getName(),
                    order.getHcf().getCode(),
                    order.getWasteCategory(),
                    order.getQuantity(),
                    order.getUnitPrice(),
                    order.getTotalAmount(),
                    order.getOrderType().name(),
                    order.getStatus().name(),
                    order.getNotes(),
                    order.getRequestedAt(),
                    order.getFulfilledAt(),
                    order.getPdfUrl());
        }
    }
}
