package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.HcfAccessGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    public HcfConsumableOrderController(
            ConsumableOrderRepository orderRepo,
            ConsumableItemRepository itemRepo,
            ConsumablePricingRepository pricingRepo,
            AgreementRepository agreementRepo,
            HcfAccessGuard accessGuard) {
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
        this.pricingRepo = pricingRepo;
        this.agreementRepo = agreementRepo;
        this.accessGuard = accessGuard;
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

        return ResponseEntity.ok(Map.of(
                "id", order.getId().toString(),
                "orderNumber", order.getOrderNumber(),
                "status", order.getStatus(),
                "totalAmount", order.getTotalAmount(),
                "message", "Order placed successfully"));
    }

    /**
     * Get order history.
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(@RequestParam(required = false) String status) {
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
    public ResponseEntity<?> getOrderDetails(@PathVariable UUID id) {
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

                    result.put("items", order.getItems().stream().map(item -> Map.of(
                            "name", item.getItemName(),
                            "quantity", item.getQuantity(),
                            "unit", item.getUnitOfMeasure(),
                            "pricePerUnit", item.getPricePerUnit(),
                            "lineTotal", item.getLineTotal())).toList());

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
