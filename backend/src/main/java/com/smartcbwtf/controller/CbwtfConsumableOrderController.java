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

    /**
     * Get order analytics - statistics by day, week, month.
     */
    @GetMapping("/analytics")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAnalytics(
            @RequestParam(name = "period", defaultValue = "month") String period) {
        UUID facilityId = TenantContext.getTenantId();

        java.time.Instant now = java.time.Instant.now();
        java.time.Instant startDate;

        switch (period) {
            case "day":
                startDate = now.minus(1, java.time.temporal.ChronoUnit.DAYS);
                break;
            case "week":
                startDate = now.minus(7, java.time.temporal.ChronoUnit.DAYS);
                break;
            case "month":
            default:
                startDate = now.minus(30, java.time.temporal.ChronoUnit.DAYS);
                break;
        }

        List<ConsumableOrder> orders = orderRepo.findByFacilityIdAndOrderedAtAfterOrderByOrderedAtDesc(
                facilityId, startDate);

        // Calculate statistics
        long totalOrders = orders.size();
        java.math.BigDecimal totalAmount = orders.stream()
                .map(ConsumableOrder::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // Status breakdown
        Map<String, Long> statusCounts = orders.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ConsumableOrder::getStatus,
                        java.util.stream.Collectors.counting()));

        // Daily breakdown
        Map<String, java.math.BigDecimal> dailyAmounts = new java.util.LinkedHashMap<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd")
                .withZone(java.time.ZoneId.systemDefault());

        orders.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        o -> formatter.format(o.getOrderedAt()),
                        java.util.stream.Collectors.reducing(
                                java.math.BigDecimal.ZERO,
                                ConsumableOrder::getTotalAmount,
                                java.math.BigDecimal::add)))
                .entrySet().stream()
                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .forEach(e -> dailyAmounts.put(e.getKey(), e.getValue()));

        return ResponseEntity.ok(Map.of(
                "period", period,
                "totalOrders", totalOrders,
                "totalAmount", totalAmount,
                "statusBreakdown", statusCounts,
                "dailyBreakdown", dailyAmounts,
                "orders", orders.stream().map(this::toOrderDTO).toList()));
    }

    /**
     * Export orders to CSV (Excel compatible).
     */
    @GetMapping(value = "/export", produces = "text/csv")
    @Transactional(readOnly = true)
    public ResponseEntity<String> exportOrders(
            @RequestParam(name = "period", defaultValue = "month") String period) {
        UUID facilityId = TenantContext.getTenantId();

        java.time.Instant now = java.time.Instant.now();
        java.time.Instant startDate;

        switch (period) {
            case "day":
                startDate = now.minus(1, java.time.temporal.ChronoUnit.DAYS);
                break;
            case "week":
                startDate = now.minus(7, java.time.temporal.ChronoUnit.DAYS);
                break;
            case "month":
            default:
                startDate = now.minus(30, java.time.temporal.ChronoUnit.DAYS);
                break;
        }

        List<ConsumableOrder> orders = orderRepo.findByFacilityIdAndOrderedAtAfterOrderByOrderedAtDesc(
                facilityId, startDate);

        StringBuilder csv = new StringBuilder();
        // Orders Summary section
        csv.append("=== ORDERS SUMMARY ===\n");
        csv.append(
                "Order Number,HCF Name,Agreement Number,Status,Total Items,Subtotal,GST,Total,Ordered At,Confirmed At,Dispatched At,Delivered At\n");

        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm")
                .withZone(java.time.ZoneId.systemDefault());

        for (ConsumableOrder order : orders) {
            String agreementNum = "";
            List<Agreement> agreements = agreementRepo.findByHcfIdAndStatus(
                    order.getHcf().getId(), Agreement.Status.ACTIVE.name());
            if (!agreements.isEmpty()) {
                agreementNum = agreements.get(0).getAgreementNumber();
            }

            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%d,%.2f,%.2f,%.2f,\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    order.getOrderNumber(),
                    order.getHcf().getName().replace("\"", "\"\""),
                    agreementNum,
                    order.getStatus(),
                    order.getItems().size(),
                    order.getSubtotal(),
                    order.getGstAmount(),
                    order.getTotalAmount(),
                    dateFormatter.format(order.getOrderedAt()),
                    order.getConfirmedAt() != null ? dateFormatter.format(order.getConfirmedAt()) : "",
                    order.getDispatchedAt() != null ? dateFormatter.format(order.getDispatchedAt()) : "",
                    order.getDeliveredAt() != null ? dateFormatter.format(order.getDeliveredAt()) : ""));
        }

        // Order Items Detail section
        csv.append("\n=== ORDER ITEMS DETAIL ===\n");
        csv.append(
                "Order Number,HCF Name,Item Name,Quantity,Unit,Price Per Unit,GST Rate (%),Line Subtotal,Line GST,Line Total\n");

        for (ConsumableOrder order : orders) {
            for (ConsumableOrderItem item : order.getItems()) {
                csv.append(String.format("\"%s\",\"%s\",\"%s\",%d,\"%s\",%.2f,%.2f,%.2f,%.2f,%.2f\n",
                        order.getOrderNumber(),
                        order.getHcf().getName().replace("\"", "\"\""),
                        item.getItemName().replace("\"", "\"\""),
                        item.getQuantity(),
                        item.getUnitOfMeasure(),
                        item.getPricePerUnit(),
                        item.getGstRate(),
                        item.getLineSubtotal(),
                        item.getLineGst(),
                        item.getLineTotal()));
            }
        }

        String filename = "consumable_orders_" + period + "_" +
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").withZone(java.time.ZoneId.systemDefault())
                        .format(now)
                + ".csv";

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "text/csv; charset=utf-8")
                .body(csv.toString());
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
                String html = emailService.getTemplates().orderStatusUpdate(
                        order.getHcf().getName(),
                        order.getOrderNumber(),
                        order.getStatus(),
                        statusMessage);
                emailService.sendHtmlEmail(hcfEmail, statusTitle + " - " + order.getOrderNumber(), html);
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
