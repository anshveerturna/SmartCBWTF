package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.QrLabelOrder;
import com.smartcbwtf.service.QrOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    private final QrOrderService qrOrderService;

    public HcfQrOrderController(QrOrderService qrOrderService) {
        this.qrOrderService = qrOrderService;
    }

    /**
     * Get QR pricing information.
     */
    @GetMapping("/pricing")
    public ResponseEntity<QrOrderService.QrPricing> getPricing() {
        return ResponseEntity.ok(qrOrderService.getPricing());
    }

    /**
     * Request QR labels from CBWTF (higher price).
     */
    @PostMapping("/request")
    public ResponseEntity<?> requestFromCbwtf(@RequestBody QrOrderRequest request) {
        try {
            UUID hcfId = TenantContext.getHcfId();

            if (hcfId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "HCF context not found"));
            }

            QrLabelOrder order = qrOrderService.createCbwtfRequest(
                    hcfId,
                    request.wasteCategory,
                    request.quantity,
                    request.notes);

            log.info("QR request created: orderId={}, hcfId={}, category={}, qty={}",
                    order.getId(), hcfId, request.wasteCategory, request.quantity);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", order.getId(),
                    "message",
                    String.format(
                            "Request submitted for %d %s QR labels. Total: ₹%.2f. CBWTF will fulfill your request.",
                            request.quantity, request.wasteCategory, order.getTotalAmount())));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("QR request failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Self-generate QR labels (lower price, immediate).
     */
    @PostMapping("/generate")
    public ResponseEntity<?> selfGenerate(@RequestBody QrOrderRequest request) {
        try {
            UUID hcfId = TenantContext.getHcfId();

            if (hcfId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "HCF context not found"));
            }

            var result = qrOrderService.selfGenerate(
                    hcfId,
                    request.wasteCategory,
                    request.quantity);

            log.info("QR self-generated: orderId={}, hcfId={}, category={}, qty={}",
                    result.order().getId(), hcfId, request.wasteCategory, request.quantity);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", result.order().getId(),
                    "qrCodes", result.qrCodes(),
                    "pdfUrl", result.pdfUrl(),
                    "totalCharge", result.order().getTotalAmount(),
                    "message", String.format("Generated %d %s QR labels. Charge: ₹%.2f",
                            request.quantity, request.wasteCategory, result.order().getTotalAmount())));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("QR generate failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List QR orders for this HCF.
     */
    @GetMapping
    public ResponseEntity<List<QrOrderDTO>> listOrders() {
        UUID hcfId = TenantContext.getHcfId();
        if (hcfId == null) {
            return ResponseEntity.badRequest().build();
        }
        List<QrLabelOrder> orders = qrOrderService.listHcfOrders(hcfId);
        return ResponseEntity.ok(orders.stream().map(QrOrderDTO::from).toList());
    }

    // DTOs
    public record QrOrderRequest(
            String wasteCategory,
            Integer quantity,
            String notes) {
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
