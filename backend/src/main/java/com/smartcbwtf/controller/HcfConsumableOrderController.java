package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.util.PaginationUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private static final int DEFAULT_ORDER_LIST_LIMIT = 100;
    private static final int MAX_ORDER_LIST_LIMIT = 250;
    private static final int MAX_ITEMS_PER_ORDER = 100;
    private static final int MAX_ORDER_QUANTITY = 100_000;
    private static final int MAX_ORDER_NOTES_LENGTH = 1000;

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

    private Agreement findActiveAgreementForTenant(UUID hcfId, UUID facilityId) {
        return agreementRepo.findActiveByHcfAndFacility(hcfId, facilityId).orElse(null);
    }

    /**
     * Browse available consumable items.
     */
    @GetMapping("/catalog")
    public ResponseEntity<?> getCatalog() {
        UUID hcfId = TenantContext.getHcfId();
        UUID facilityId = TenantContext.getTenantId();
        accessGuard.assertPortalAccess(hcfId, facilityId);

        Agreement agreement = findActiveAgreementForTenant(hcfId, facilityId);
        if (agreement == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "NO_FACILITY",
                    "message", "No active agreement found"));
        }

        List<ConsumableItem> items = itemRepo.findActiveByFacility(facilityId);
        Map<UUID, ConsumablePricing> pricingByItemId = items.isEmpty()
                ? Map.of()
                : pricingRepo.findActiveByConsumableItemIdIn(items.stream().map(ConsumableItem::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(
                                pricing -> pricing.getConsumableItem().getId(),
                                pricing -> pricing,
                                (first, ignored) -> first));

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

                    ConsumablePricing pricing = pricingByItemId.get(item.getId());
                    if (pricing != null) {
                        itemMap.put("price", pricing.getPricePerUnit());
                        itemMap.put("gstRate", pricing.getGstRate());
                    }
                    return itemMap;
                }).toList()));
    }

    /**
     * Place a new order.
     */
    @PostMapping("/order")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        UUID hcfId = TenantContext.getHcfId();
        UUID facilityId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();
        accessGuard.assertPortalAccess(hcfId, facilityId);

        if (request.items == null || request.items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "EMPTY_ORDER",
                    "message", "Order must contain at least one item"));
        }

        // Get facility from agreement
        Agreement agreement = findActiveAgreementForTenant(hcfId, facilityId);
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
        order.setHcfNotes(trimToNull(request.notes));

        // Add items
        for (OrderItemRequest itemReq : request.items) {
            ConsumableItem item = itemRepo.findByIdAndFacilityId(itemReq.itemId, facility.getId())
                    .filter(consumable -> Boolean.TRUE.equals(consumable.getIsActive()))
                    .orElse(null);
            if (item == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "INVALID_ITEM",
                        "message", "Item not found or unavailable"));
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
    public ResponseEntity<?> getOrders(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        UUID hcfId = TenantContext.getHcfId();
        UUID facilityId = TenantContext.getTenantId();
        accessGuard.assertPortalAccess(hcfId, facilityId);
        String normalizedStatus = normalizeStatus(status);
        if (normalizedStatus == null && status != null && !status.isBlank()) {
            return privateResponse(Map.of("orders", List.of(), "total", 0L));
        }

        PageRequest pageable = firstPage(limit);
        List<ConsumableOrder> orders = normalizedStatus != null
                ? orderRepo.findByHcfIdAndFacilityIdAndStatusOrderByOrderedAtDesc(
                        hcfId, facilityId, normalizedStatus, pageable)
                : orderRepo.findByHcfIdAndFacilityIdOrderByOrderedAtDesc(hcfId, facilityId, pageable);
        long total = normalizedStatus != null
                ? orderRepo.countByHcfIdAndFacilityIdAndStatus(hcfId, facilityId, normalizedStatus)
                : orderRepo.countByHcfIdAndFacilityId(hcfId, facilityId);

        Map<UUID, Long> itemCountsByOrderId = itemCountsByOrderId(orders);
        return privateResponse(Map.of(
                "orders", orders.stream().map(order -> {
                    Map<String, Object> orderMap = new HashMap<>();
                    orderMap.put("id", order.getId().toString());
                    orderMap.put("orderNumber", order.getOrderNumber());
                    orderMap.put("status", order.getStatus());
                    orderMap.put("itemCount", itemCountsByOrderId.getOrDefault(order.getId(), 0L));
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
                "total", total));
    }

    /**
     * Get order details.
     */
    @GetMapping("/orders/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrderDetails(@PathVariable("id") UUID id) {
        UUID hcfId = TenantContext.getHcfId();
        UUID facilityId = TenantContext.getTenantId();
        accessGuard.assertPortalAccess(hcfId, facilityId);

        return orderRepo.findByIdAndHcfIdAndFacilityId(id, hcfId, facilityId)
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

                    return privateResponse(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private static <T> ResponseEntity<T> privateResponse(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    /**
     * Cancel an order (only if PENDING or CONFIRMED).
     */
    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable UUID id) {
        UUID hcfId = TenantContext.getHcfId();
        UUID facilityId = TenantContext.getTenantId();
        accessGuard.assertPortalAccess(hcfId, facilityId);

        ConsumableOrder order = orderRepo.findByIdAndHcfIdAndFacilityId(id, hcfId, facilityId).orElse(null);

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

    private Map<UUID, Long> itemCountsByOrderId(List<ConsumableOrder> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        return orderRepo.countItemsByOrderIds(orders.stream().map(ConsumableOrder::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1],
                        (first, ignored) -> first));
    }

    public static class PlaceOrderRequest {
        @NotEmpty
        @Size(max = MAX_ITEMS_PER_ORDER)
        @Valid
        public List<OrderItemRequest> items;
        @Size(max = MAX_ORDER_NOTES_LENGTH)
        public String notes;
    }

    public static class OrderItemRequest {
        @NotNull
        public UUID itemId;
        @NotNull
        @Positive
        @Max(MAX_ORDER_QUANTITY)
        public Integer quantity;
    }

    private static PageRequest firstPage(int requestedLimit) {
        int limit = PaginationUtils.normalizeSize(requestedLimit, DEFAULT_ORDER_LIST_LIMIT, MAX_ORDER_LIST_LIMIT);
        return PageRequest.of(0, limit);
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ConsumableOrder.Status.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
