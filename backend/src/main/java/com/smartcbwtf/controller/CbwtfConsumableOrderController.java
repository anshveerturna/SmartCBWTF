package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * CBWTF Consumable Orders Controller - Manage orders from HCFs.
 */
@RestController
@RequestMapping("/api/cbwtf/consumable-orders")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfConsumableOrderController {

    private static final Logger log = LoggerFactory.getLogger(CbwtfConsumableOrderController.class);

    private final ConsumableOrderRepository orderRepo;
    private final AgreementRepository agreementRepo;
    private final EmailService emailService;

    public CbwtfConsumableOrderController(
            ConsumableOrderRepository orderRepo,
            AgreementRepository agreementRepo,
            EmailService emailService) {
        this.orderRepo = orderRepo;
        this.agreementRepo = agreementRepo;
        this.emailService = emailService;
    }

    /**
     * List all orders for this facility.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> listOrders(
            @RequestParam(name = "status", required = false) String status) {
        UUID facilityId = TenantContext.getTenantId();

        List<ConsumableOrder> orders = status != null
                ? orderRepo.findByFacilityIdAndStatusOrderByOrderedAtDesc(facilityId, status)
                : orderRepo.findByFacilityIdOrderByOrderedAtDesc(facilityId);

        return ResponseEntity.ok(Map.of(
                "orders", orders.stream().map(this::toOrderDTO).toList(),
                "total", orders.size()));
    }

    /**
     * Get order details.
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrderDetails(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();

        return orderRepo.findById(id)
                .filter(order -> order.getFacility().getId().equals(facilityId))
                .map(order -> {
                    Map<String, Object> result = toOrderDTO(order);
                    result.put("items", order.getItems().stream().map(item -> {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("name", item.getItemName());
                        itemMap.put("quantity", item.getQuantity());
                        itemMap.put("unit", item.getUnitOfMeasure());
                        itemMap.put("pricePerUnit", item.getPricePerUnit());
                        itemMap.put("gstRate", item.getGstRate());
                        itemMap.put("lineTotal", item.getLineTotal());
                        // Include image URL from the consumable item
                        if (item.getConsumableItem() != null && item.getConsumableItem().getImageUrl() != null) {
                            itemMap.put("imageUrl", item.getConsumableItem().getImageUrl());
                        }
                        return itemMap;
                    }).toList());
                    result.put("hcfNotes", order.getHcfNotes());
                    result.put("cbwtfNotes", order.getCbwtfNotes());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Confirm an order.
     */
    @PostMapping("/{id}/confirm")
    @Transactional
    public ResponseEntity<?> confirmOrder(@PathVariable("id") UUID id,
            @RequestBody(required = false) NotesRequest request) {
        UUID facilityId = TenantContext.getTenantId();

        ConsumableOrder order = orderRepo.findById(id)
                .filter(o -> o.getFacility().getId().equals(facilityId))
                .orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!order.getStatus().equals("PENDING")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_STATUS",
                    "message", "Order can only be confirmed when PENDING"));
        }

        order.setStatusEnum(ConsumableOrder.Status.CONFIRMED);
        order.setConfirmedAt(Instant.now());
        if (request != null && request.notes != null) {
            order.setCbwtfNotes(request.notes);
        }
        orderRepo.save(order);

        log.info("Order {} confirmed by CBWTF {}", order.getOrderNumber(), facilityId);

        // Send notification to HCF
        sendStatusEmail(order, "Order Confirmed", "Your order has been confirmed and is being prepared.");

        return ResponseEntity.ok(toOrderDTO(order));
    }

    /**
     * Mark order as dispatched.
     */
    @PostMapping("/{id}/dispatch")
    @Transactional
    public ResponseEntity<?> dispatchOrder(@PathVariable("id") UUID id,
            @RequestBody(required = false) NotesRequest request) {
        UUID facilityId = TenantContext.getTenantId();

        ConsumableOrder order = orderRepo.findById(id)
                .filter(o -> o.getFacility().getId().equals(facilityId))
                .orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!order.getStatus().equals("CONFIRMED")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_STATUS",
                    "message", "Order can only be dispatched when CONFIRMED"));
        }

        order.setStatusEnum(ConsumableOrder.Status.DISPATCHED);
        order.setDispatchedAt(Instant.now());
        if (request != null && request.notes != null) {
            order.setCbwtfNotes((order.getCbwtfNotes() != null ? order.getCbwtfNotes() + "\n" : "") + request.notes);
        }
        orderRepo.save(order);

        log.info("Order {} dispatched by CBWTF {}", order.getOrderNumber(), facilityId);

        sendStatusEmail(order, "Order Dispatched", "Your order has been dispatched and is on the way.");

        return ResponseEntity.ok(toOrderDTO(order));
    }

    /**
     * Mark order as delivered.
     */
    @PostMapping("/{id}/deliver")
    @Transactional
    public ResponseEntity<?> deliverOrder(@PathVariable("id") UUID id,
            @RequestBody(required = false) NotesRequest request) {
        UUID facilityId = TenantContext.getTenantId();

        ConsumableOrder order = orderRepo.findById(id)
                .filter(o -> o.getFacility().getId().equals(facilityId))
                .orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!order.getStatus().equals("DISPATCHED")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_STATUS",
                    "message", "Order can only be delivered when DISPATCHED"));
        }

        order.setStatusEnum(ConsumableOrder.Status.DELIVERED);
        order.setDeliveredAt(Instant.now());
        if (request != null && request.notes != null) {
            order.setCbwtfNotes((order.getCbwtfNotes() != null ? order.getCbwtfNotes() + "\n" : "") + request.notes);
        }
        orderRepo.save(order);

        log.info("Order {} delivered by CBWTF {}", order.getOrderNumber(), facilityId);

        sendStatusEmail(order, "Order Delivered", "Your order has been delivered successfully.");

        return ResponseEntity.ok(toOrderDTO(order));
    }

    /**
     * Cancel an order.
     */
    @PostMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancelOrder(@PathVariable("id") UUID id,
            @RequestBody(required = false) NotesRequest request) {
        UUID facilityId = TenantContext.getTenantId();

        ConsumableOrder order = orderRepo.findById(id)
                .filter(o -> o.getFacility().getId().equals(facilityId))
                .orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!order.canCancel()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "CANNOT_CANCEL",
                    "message", "Order cannot be cancelled in current status"));
        }

        order.setStatusEnum(ConsumableOrder.Status.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setCancellationReason(request != null && request.notes != null ? request.notes : "Cancelled by CBWTF");
        orderRepo.save(order);

        log.info("Order {} cancelled by CBWTF {}", order.getOrderNumber(), facilityId);

        sendStatusEmail(order, "Order Cancelled", "Your order has been cancelled. " +
                (order.getCancellationReason() != null ? "Reason: " + order.getCancellationReason() : ""));

        return ResponseEntity.ok(toOrderDTO(order));
    }

    /**
     * Get pending orders count for dashboard alert.
     */
    @GetMapping("/pending-count")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPendingCount() {
        UUID facilityId = TenantContext.getTenantId();
        long count = orderRepo.countByFacilityIdAndStatus(facilityId, "PENDING");
        return ResponseEntity.ok(Map.of("count", count));
    }

    private Map<String, Object> toOrderDTO(ConsumableOrder order) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", order.getId().toString());
        dto.put("orderNumber", order.getOrderNumber());
        dto.put("status", order.getStatus());
        dto.put("itemCount", order.getItems().size());
        dto.put("subtotal", order.getSubtotal());
        dto.put("gstAmount", order.getGstAmount());
        dto.put("totalAmount", order.getTotalAmount());
        dto.put("orderedAt", order.getOrderedAt().toString());

        // HCF details
        Hcf hcf = order.getHcf();
        dto.put("hcfId", hcf.getId().toString());
        dto.put("hcfName", hcf.getName());
        dto.put("hcfCode", hcf.getCode());
        dto.put("hcfAddress", hcf.getAddress());

        // Get agreement number for this HCF
        agreementRepo.findByHcfIdAndStatus(hcf.getId(), Agreement.Status.ACTIVE.name())
                .stream().findFirst()
                .ifPresent(agr -> dto.put("agreementNumber", agr.getAgreementNumber()));

        if (order.getConfirmedAt() != null) {
            dto.put("confirmedAt", order.getConfirmedAt().toString());
        }
        if (order.getDispatchedAt() != null) {
            dto.put("dispatchedAt", order.getDispatchedAt().toString());
        }
        if (order.getDeliveredAt() != null) {
            dto.put("deliveredAt", order.getDeliveredAt().toString());
        }
        if (order.getCancelledAt() != null) {
            dto.put("cancelledAt", order.getCancelledAt().toString());
            dto.put("cancellationReason", order.getCancellationReason());
        }

        return dto;
    }

    private void sendStatusEmail(ConsumableOrder order, String statusTitle, String statusMessage) {
        try {
            String hcfEmail = order.getHcf().getContactEmail();
            if (hcfEmail != null && !hcfEmail.isEmpty()) {
                String subject = statusTitle + " - " + order.getOrderNumber();
                String body = String.format(
                        "<h2>%s</h2>" +
                                "<p>Dear %s,</p>" +
                                "<p>%s</p>" +
                                "<p><strong>Order Number:</strong> %s<br>" +
                                "<strong>Total Amount:</strong> ₹%.2f</p>" +
                                "<p>Thank you,<br>SmartCBWTF Team</p>",
                        statusTitle, order.getHcf().getName(), statusMessage,
                        order.getOrderNumber(), order.getTotalAmount());
                emailService.sendEmail(hcfEmail, subject, body);
                log.info("Status update email sent to HCF: {}", hcfEmail);
            }
        } catch (Exception e) {
            log.warn("Failed to send status update email: {}", e.getMessage());
        }
    }

    public static class NotesRequest {
        public String notes;
    }
}
