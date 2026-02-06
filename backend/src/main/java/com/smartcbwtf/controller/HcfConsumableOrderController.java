package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.HcfAccessGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HCF Consumable Order Controller - Order consumables from CBWTF.
 * 
 * Regulatory Note: Consumable ordering is INDEPENDENT of dues status.
 */
@RestController
@RequestMapping("/api/hcf/consumables")
@PreAuthorize("hasRole('HCF_ADMIN')")
public class HcfConsumableOrderController {

    private static final Logger log = LoggerFactory.getLogger(HcfConsumableOrderController.class);

    private final ConsumableOrderRepository orderRepo;
    private final ConsumableItemRepository itemRepo;
    private final ConsumablePricingRepository pricingRepo;
    private final AgreementRepository agreementRepo;
    private final HcfAccessGuard accessGuard;
    private final com.smartcbwtf.service.EmailService emailService;

    public HcfConsumableOrderController(
            ConsumableOrderRepository orderRepo,
            ConsumableItemRepository itemRepo,
            ConsumablePricingRepository pricingRepo,
            AgreementRepository agreementRepo,
            HcfAccessGuard accessGuard,
            com.smartcbwtf.service.EmailService emailService) {
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
        this.pricingRepo = pricingRepo;
        this.agreementRepo = agreementRepo;
        this.accessGuard = accessGuard;
        this.emailService = emailService;
    }

    private UUID getFacilityIdForHcf(UUID hcfId) {
        return agreementRepo.findByHcfIdAndStatus(hcfId, Agreement.Status.ACTIVE.name())
                .stream()
                .findFirst()
                .map(a -> a.getFacility().getId())
                .orElse(null);
    }

    /**
     * Browse available consumable items.
     */
    @GetMapping("/catalog")
    public ResponseEntity<?> getCatalog() {
        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        UUID facilityId = getFacilityIdForHcf(hcfId);
        if (facilityId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "NO_FACILITY",
                    "message", "No active agreement found"));
        }

        List<ConsumableItem> items = itemRepo.findActiveByFacility(facilityId);

        return ResponseEntity.ok(Map.of(
                "items", items.stream().map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("id", item.getId().toString());
                    itemMap.put("code", item.getConsumableCode());
                    itemMap.put("name", item.getName());
                    itemMap.put("description", item.getDescription());
                    itemMap.put("category", item.getCategory().getName());
                    itemMap.put("unit", item.getUnitOfMeasure());
                    itemMap.put("imageUrl", item.getImageUrl());

                    // Get current pricing
                    pricingRepo.findActiveByConsumableItemId(item.getId())
                            .ifPresent(pricing -> {
                                itemMap.put("price", pricing.getPricePerUnit());
                                itemMap.put("gstRate", pricing.getGstRate());
                            });
                    return itemMap;
                }).toList()));
    }

    /**
     * Place a new order.
     */
    @PostMapping("/order")
    public ResponseEntity<?> placeOrder(@RequestBody PlaceOrderRequest request) {
        UUID hcfId = TenantContext.getHcfId();
        UUID userId = TenantContext.getUserId();
        accessGuard.assertPortalAccess(hcfId);

        if (request.items == null || request.items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "EMPTY_ORDER",
                    "message", "Order must contain at least one item"));
        }

        // Get facility from agreement
        Agreement agreement = agreementRepo.findByHcfIdAndStatus(hcfId, Agreement.Status.ACTIVE.name())
                .stream().findFirst().orElse(null);
        if (agreement == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "NO_AGREEMENT",
                    "message", "No active agreement found"));
        }

        Facility facility = agreement.getFacility();

        // Generate order number
        long orderCount = orderRepo.countByFacilityId(facility.getId());
        String orderNumber = String.format("ORD-%s-%06d",
                facility.getCode(), orderCount + 1);

        ConsumableOrder order = new ConsumableOrder();
        order.setHcf(agreement.getHcf());
        order.setFacility(facility);
        order.setOrderNumber(orderNumber);
        order.setOrderedBy(userId);
        order.setHcfNotes(request.notes);

        // Add items
        for (OrderItemRequest itemReq : request.items) {
            ConsumableItem item = itemRepo.findById(itemReq.itemId).orElse(null);
            if (item == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "INVALID_ITEM",
                        "message", "Item not found: " + itemReq.itemId));
            }

            ConsumablePricing pricing = pricingRepo.findActiveByConsumableItemId(item.getId())
                    .orElse(null);
            if (pricing == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "NO_PRICING",
                        "message", "Pricing not available for: " + item.getName()));
            }

            ConsumableOrderItem orderItem = new ConsumableOrderItem();
            orderItem.setConsumableItem(item);
            orderItem.setItemName(item.getName());
            orderItem.setUnitOfMeasure(item.getUnitOfMeasure());
            orderItem.setQuantity(itemReq.quantity);
            orderItem.setPricePerUnit(pricing.getPricePerUnit());
            orderItem.setGstRate(pricing.getGstRate());
            orderItem.calculateTotals();

            order.addItem(orderItem);
        }

        orderRepo.save(order);
        log.info("HCF {} placed order {} with {} items, total: {}",
                hcfId, orderNumber, order.getItems().size(), order.getTotalAmount());

        // Send email notifications
        sendOrderConfirmationEmails(order, agreement.getHcf(), facility);

        return ResponseEntity.ok(Map.of(
                "id", order.getId().toString(),
                "orderNumber", order.getOrderNumber(),
                "status", order.getStatus(),
                "totalAmount", order.getTotalAmount(),
                "message", "Order placed successfully"));
    }

    private void sendOrderConfirmationEmails(ConsumableOrder order, Hcf hcf, Facility facility) {
        try {
            // Build items HTML for email
            String itemsHtml = "<table style='width:100%; border-collapse:collapse;'>" +
                    "<tr style='background:#1a8754; color:#fff;'><th style='padding:10px;text-align:left;'>Item</th><th style='padding:10px;'>Qty</th><th style='padding:10px;'>Amount</th></tr>"
                    +
                    order.getItems().stream()
                            .map(i -> String.format(
                                    "<tr style='border-bottom:1px solid #e9ecef;'><td style='padding:10px;'>%s</td><td style='padding:10px;text-align:center;'>%d</td><td style='padding:10px;text-align:right;'>₹%.2f</td></tr>",
                                    i.getItemName(), i.getQuantity(), i.getLineTotal()))
                            .collect(java.util.stream.Collectors.joining(""))
                    +
                    "</table>";

            String total = String.format("%.2f", order.getTotalAmount());

            // Email to HCF (order confirmation)
            String hcfEmail = hcf.getContactEmail();
            if (hcfEmail != null && !hcfEmail.isEmpty()) {
                String html = emailService.getTemplates().orderPlacedHcf(
                        hcf.getName(),
                        order.getOrderNumber(),
                        facility.getName(),
                        itemsHtml,
                        total);
                emailService.sendHtmlEmail(hcfEmail, "Order Confirmation - " + order.getOrderNumber(), html);
                log.info("Order confirmation email sent to HCF: {}", hcfEmail);
            }

            // Email to CBWTF (new order notification)
            String cbwtfEmail = facility.getContactEmail();
            if (cbwtfEmail != null && !cbwtfEmail.isEmpty()) {
                String html = emailService.getTemplates().orderPlacedCbwtf(
                        order.getOrderNumber(),
                        hcf.getName(),
                        hcf.getCode(),
                        itemsHtml,
                        total,
                        order.getHcfNotes());
                emailService.sendHtmlEmail(cbwtfEmail, "New Consumable Order - " + order.getOrderNumber(), html);
                log.info("New order notification email sent to CBWTF: {}", cbwtfEmail);
            }
        } catch (Exception e) {
            log.warn("Failed to send order confirmation emails: {}", e.getMessage());
            // Don't fail the order just because emails couldn't be sent
        }
    }

    /**
     * Get order history.
     */
    @GetMapping("/orders")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getOrders(@RequestParam(name = "status", required = false) String status) {
        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        List<ConsumableOrder> orders = status != null
                ? orderRepo.findByHcfIdAndStatusOrderByOrderedAtDesc(hcfId, status)
                : orderRepo.findByHcfIdOrderByOrderedAtDesc(hcfId);

        return ResponseEntity.ok(Map.of(
                "orders", orders.stream().map(order -> {
                    Map<String, Object> orderMap = new HashMap<>();
                    orderMap.put("id", order.getId().toString());
                    orderMap.put("orderNumber", order.getOrderNumber());
                    orderMap.put("status", order.getStatus());
                    orderMap.put("itemCount", order.getItems().size());
                    orderMap.put("totalAmount", order.getTotalAmount());
                    orderMap.put("orderedAt", order.getOrderedAt().toString());
                    if (order.getConfirmedAt() != null) {
                        orderMap.put("confirmedAt", order.getConfirmedAt().toString());
                    }
                    if (order.getDeliveredAt() != null) {
                        orderMap.put("deliveredAt", order.getDeliveredAt().toString());
                    }
                    return orderMap;
                }).toList(),
                "total", orders.size()));
    }

    /**
     * Get order details.
     */
    @GetMapping("/orders/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrderDetails(@PathVariable("id") UUID id) {
        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        return orderRepo.findById(id)
                .filter(order -> order.getHcf().getId().equals(hcfId))
                .map(order -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", order.getId().toString());
                    result.put("orderNumber", order.getOrderNumber());
                    result.put("status", order.getStatus());
                    result.put("subtotal", order.getSubtotal());
                    result.put("gstAmount", order.getGstAmount());
                    result.put("totalAmount", order.getTotalAmount());
                    result.put("hcfNotes", order.getHcfNotes());
                    result.put("cbwtfNotes", order.getCbwtfNotes());
                    result.put("orderedAt", order.getOrderedAt().toString());

                    result.put("items", order.getItems().stream().map(item -> {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("name", item.getItemName());
                        itemMap.put("quantity", item.getQuantity());
                        itemMap.put("unit", item.getUnitOfMeasure());
                        itemMap.put("pricePerUnit", item.getPricePerUnit());
                        itemMap.put("gstRate", item.getGstRate());
                        itemMap.put("lineTotal", item.getLineTotal());
                        if (item.getConsumableItem() != null && item.getConsumableItem().getImageUrl() != null) {
                            itemMap.put("imageUrl", item.getConsumableItem().getImageUrl());
                        }
                        return itemMap;
                    }).toList());

                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancel an order (only if PENDING or CONFIRMED).
     */
    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable UUID id) {
        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        ConsumableOrder order = orderRepo.findById(id)
                .filter(o -> o.getHcf().getId().equals(hcfId))
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
        order.setCancellationReason("Cancelled by HCF");
        orderRepo.save(order);

        log.info("HCF {} cancelled order {}", hcfId, order.getOrderNumber());

        // Send cancellation email to CBWTF
        try {
            Facility facility = order.getFacility();
            if (facility != null && facility.getContactEmail() != null && !facility.getContactEmail().isBlank()) {
                String html = emailService.getTemplates().orderCancelled(
                        facility.getName(),
                        order.getOrderNumber(),
                        "Cancelled by HCF",
                        false);
                emailService.sendHtmlEmail(facility.getContactEmail(),
                        "Order Cancelled - " + order.getOrderNumber(), html);
                log.info("Order cancellation email sent to CBWTF: {}", facility.getContactEmail());
            }
        } catch (Exception e) {
            log.warn("Failed to send order cancellation email: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "id", order.getId().toString(),
                "status", order.getStatus(),
                "message", "Order cancelled successfully"));
    }

    public static class PlaceOrderRequest {
        public List<OrderItemRequest> items;
        public String notes;
    }

    public static class OrderItemRequest {
        public UUID itemId;
        public Integer quantity;
    }
}
