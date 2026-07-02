package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.QrAuthorization;
import com.smartcbwtf.domain.QrAuthorization.WasteCategory;
import com.smartcbwtf.domain.QrLabelOrder;
import com.smartcbwtf.service.HcfAccessGuard;
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
 * HCF QR Label Order Controller.
 * 
 * Allows HCFs to:
 * - Request QR labels from CBWTF (more expensive)
 * - Self-generate QR labels (cheaper)
 */
@RestController
@RequestMapping("/api/hcf/qr-orders")
@PreAuthorize("hasRole('HCF_ADMIN')")
public class HcfQrOrderController {

    private static final Logger log = LoggerFactory.getLogger(HcfQrOrderController.class);
    private static final int MAX_QR_QUANTITY_PER_CATEGORY = 100_000;
    private static final int MAX_WASTE_CATEGORIES_PER_ORDER = 4;
    private static final int MAX_QR_NOTES_LENGTH = 1000;

    private final QrOrderService qrOrderService;
    private final PdfService pdfService;
    private final HcfAccessGuard accessGuard;

    public HcfQrOrderController(QrOrderService qrOrderService, PdfService pdfService, HcfAccessGuard accessGuard) {
        this.qrOrderService = qrOrderService;
        this.pdfService = pdfService;
        this.accessGuard = accessGuard;
    }

    /**
     * Get QR pricing information.
     */
    @GetMapping("/pricing")
    public ResponseEntity<QrOrderService.QrPricing> getPricing() {
        requirePortalHcfIds();
        return ResponseEntity.ok(qrOrderService.getPricing());
    }

    /**
     * Request QR labels from CBWTF (higher price).
     */
    @PostMapping("/request")
    public ResponseEntity<?> requestFromCbwtf(@Valid @RequestBody QrOrderRequest request) {
        try {
            PortalHcfIds ids = requirePortalHcfIds();
            String wasteCategory = requireWasteCategory(request.wasteCategory());
            int quantity = requireQuantity(request.quantity());

            QrLabelOrder order = qrOrderService.createCbwtfRequest(
                    ids.hcfId(),
                    ids.facilityId(),
                    wasteCategory,
                    quantity,
                    trimToNull(request.notes()));

            log.info("QR request created: orderId={}, hcfId={}, category={}, qty={}",
                    order.getId(), ids.hcfId(), wasteCategory, quantity);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", order.getId(),
                    "message",
                    String.format(
                            "Request submitted for %d %s QR labels. Total: ₹%.2f. CBWTF will fulfill your request.",
                            quantity, wasteCategory, order.getTotalAmount())));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("QR request failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Self-generate QR labels (lower price, immediate).
     * Supports single category (wasteCategory + quantity) or multi-category
     * (categoryQuantities map).
     */
    @PostMapping("/generate")
    public ResponseEntity<?> selfGenerate(@Valid @RequestBody QrOrderRequest request) {
        try {
            PortalHcfIds ids = requirePortalHcfIds();
            Map<String, Integer> categoryQuantities = requireCategoryQuantities(request);

            int totalQty = categoryQuantities.values().stream().mapToInt(Integer::intValue).sum();

            var result = qrOrderService.selfGenerateMulti(
                    ids.hcfId(), ids.facilityId(), categoryQuantities, request.validUntil());

            log.info("QR self-generated: orderId={}, hcfId={}, categories={}, totalQty={}",
                    result.order().getId(), ids.hcfId(), categoryQuantities.keySet(), totalQty);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", result.order().getId(),
                    "qrCodes", result.qrCodes(),
                    "pdfUrl", result.pdfUrl(),
                    "totalCharge", result.order().getTotalAmount(),
                    "message", String.format("Generated %d QR labels. Charge: ₹%.2f",
                            totalQty, result.order().getTotalAmount())));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("QR generate failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List QR orders for this HCF.
     */
    @GetMapping
    public ResponseEntity<List<QrOrderDTO>> listOrders(
            @RequestParam(defaultValue = "100") int limit) {
        PortalHcfIds ids = requirePortalHcfIds();
        List<QrLabelOrder> orders = qrOrderService.listHcfOrders(ids.hcfId(), ids.facilityId(), limit);
        return ResponseEntity.ok(orders.stream().map(QrOrderDTO::from).toList());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> downloadOrderPdf(@PathVariable("id") UUID id) {
        try {
            PortalHcfIds ids = requirePortalHcfIds();

            QrLabelOrder order = qrOrderService.getOrderPdfForHcf(id, ids.hcfId(), ids.facilityId());
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

    private PortalHcfIds requirePortalHcfIds() {
        UUID hcfId = TenantContext.getHcfId();
        UUID facilityId = TenantContext.getTenantId();
        accessGuard.assertPortalAccess(hcfId, facilityId);
        return new PortalHcfIds(hcfId, facilityId);
    }

    private record PortalHcfIds(UUID hcfId, UUID facilityId) {
    }

    private static Map<String, Integer> requireCategoryQuantities(QrOrderRequest request) {
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

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    // DTOs
    public record QrOrderRequest(
            @Pattern(regexp = QrAuthorization.WASTE_CATEGORY_PATTERN, message = "must be one of YELLOW, RED, BLUE, WHITE") String wasteCategory,
            @Positive @Max(MAX_QR_QUANTITY_PER_CATEGORY) Integer quantity,
            @Size(max = MAX_QR_NOTES_LENGTH) String notes,
            @Valid @Size(max = MAX_WASTE_CATEGORIES_PER_ORDER) Map<@Pattern(regexp = QrAuthorization.WASTE_CATEGORY_PATTERN, message = "must be one of YELLOW, RED, BLUE, WHITE") String, @NotNull @Positive @Max(MAX_QR_QUANTITY_PER_CATEGORY) Integer> categoryQuantities,
            @FutureOrPresent LocalDate validUntil) {
    }

    public record QrOrderDTO(
            UUID id,
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
