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
 * CBWTF QR Label Order Controller.
 * 
 * Shows QR requests alongside consumable orders for CBWTF admin.
 */
@RestController
@RequestMapping("/api/cbwtf/qr-orders")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfQrOrderController {

    private static final Logger log = LoggerFactory.getLogger(CbwtfQrOrderController.class);

    private final QrOrderService qrOrderService;

    public CbwtfQrOrderController(QrOrderService qrOrderService) {
        this.qrOrderService = qrOrderService;
    }

    /**
     * List pending QR orders for this facility.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<QrOrderDTO>> listPendingOrders() {
        UUID facilityId = TenantContext.getTenantId();
        List<QrLabelOrder> orders = qrOrderService.listPendingOrders(facilityId);
        return ResponseEntity.ok(orders.stream().map(QrOrderDTO::from).toList());
    }

    /**
     * List all QR orders (for history).
     */
    @GetMapping
    public ResponseEntity<List<QrOrderDTO>> listAllOrders(
            @RequestParam(required = false) String status) {
        UUID facilityId = TenantContext.getTenantId();
        // For now, return pending orders; can extend with status filter
        List<QrLabelOrder> orders = qrOrderService.listPendingOrders(facilityId);
        return ResponseEntity.ok(orders.stream().map(QrOrderDTO::from).toList());
    }

    /**
     * Fulfill a QR order - generates labels and charges HCF.
     */
    @PostMapping("/{id}/fulfill")
    public ResponseEntity<?> fulfillOrder(@PathVariable("id") UUID id) {
        try {
            UUID adminUserId = TenantContext.getUserId();
            QrLabelOrder fulfilled = qrOrderService.fulfillRequest(id, adminUserId);

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
            @RequestBody(required = false) RejectRequest request) {
        try {
            UUID adminUserId = TenantContext.getUserId();
            String reason = request != null && request.reason != null ? request.reason : "No reason provided";

            QrLabelOrder rejected = qrOrderService.rejectRequest(id, adminUserId, reason);

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

    // DTOs
    public record RejectRequest(String reason) {
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
