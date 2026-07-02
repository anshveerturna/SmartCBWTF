package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.EmailService;
import com.smartcbwtf.util.PaginationUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
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
    private static final int DEFAULT_ORDER_LIST_LIMIT = 100;
    private static final int MAX_ORDER_LIST_LIMIT = 250;
    private static final int MAX_EXPORT_ORDER_ROWS = 5_000;
    private static final int MAX_ORDER_NOTE_LENGTH = 1000;

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
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        UUID facilityId = TenantContext.getTenantId();
        String normalizedStatus = normalizeStatus(status);
        if (normalizedStatus == null && status != null && !status.isBlank()) {
            return ResponseEntity.ok(Map.of("orders", List.of(), "total", 0L));
        }

        PageRequest pageable = firstPage(limit);
        List<ConsumableOrder> orders = normalizedStatus != null
                ? orderRepo.findByFacilityIdAndStatusOrderByOrderedAtDesc(facilityId, normalizedStatus, pageable)
                : orderRepo.findByFacilityIdOrderByOrderedAtDesc(facilityId, pageable);
        long total = normalizedStatus != null
                ? orderRepo.countByFacilityIdAndStatus(facilityId, normalizedStatus)
                : orderRepo.countByFacilityId(facilityId);

        Map<UUID, Long> itemCountsByOrderId = itemCountsByOrderId(orders);
        Map<UUID, String> agreementNumbersByHcfId = agreementNumbersByHcfId(facilityId, orders);
        return ResponseEntity.ok(Map.of(
                "orders", orders.stream()
                        .map(order -> toOrderDTO(order, itemCountsByOrderId, agreementNumbersByHcfId))
                        .toList(),
                "total", total));
    }

    /**
     * Get order details.
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrderDetails(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();

        return orderRepo.findByIdAndFacilityId(id, facilityId)
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
            @Valid @RequestBody(required = false) NotesRequest request) {
        UUID facilityId = TenantContext.getTenantId();

        ConsumableOrder order = orderRepo.findByIdAndFacilityId(id, facilityId).orElse(null);

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
        String notes = normalizeOptionalNotes(request != null ? request.notes : null);
        if (notes != null) {
            order.setCbwtfNotes(notes);
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
            @Valid @RequestBody(required = false) NotesRequest request) {
        UUID facilityId = TenantContext.getTenantId();

        ConsumableOrder order = orderRepo.findByIdAndFacilityId(id, facilityId).orElse(null);

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
        String notes = normalizeOptionalNotes(request != null ? request.notes : null);
        if (notes != null) {
            order.setCbwtfNotes(appendNotes(order.getCbwtfNotes(), notes));
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
            @Valid @RequestBody(required = false) NotesRequest request) {
        UUID facilityId = TenantContext.getTenantId();

        ConsumableOrder order = orderRepo.findByIdAndFacilityId(id, facilityId).orElse(null);

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
        String notes = normalizeOptionalNotes(request != null ? request.notes : null);
        if (notes != null) {
            order.setCbwtfNotes(appendNotes(order.getCbwtfNotes(), notes));
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
            @Valid @RequestBody(required = false) NotesRequest request) {
        UUID facilityId = TenantContext.getTenantId();

        ConsumableOrder order = orderRepo.findByIdAndFacilityId(id, facilityId).orElse(null);

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
        String notes = normalizeOptionalNotes(request != null ? request.notes : null);
        order.setCancellationReason(notes != null ? notes : "Cancelled by CBWTF");
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
        String normalizedPeriod = normalizePeriod(period);
        java.time.Instant startDate = startDateForPeriod(normalizedPeriod, now);
        long exportRows = orderRepo.countByFacilityIdAndOrderedAtAfter(facilityId, startDate);
        if (exportRows > MAX_EXPORT_ORDER_ROWS) {
            return ResponseEntity.status(413)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of(
                            "error", "RESULT_SET_TOO_LARGE",
                            "message", "Narrow the period before loading consumable order analytics.",
                            "totalRows", exportRows,
                            "maxRows", MAX_EXPORT_ORDER_ROWS));
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

        Map<UUID, Long> itemCountsByOrderId = itemCountsByOrderId(orders);
        Map<UUID, String> agreementNumbersByHcfId = agreementNumbersByHcfId(facilityId, orders);
        return ResponseEntity.ok(Map.of(
                "period", normalizedPeriod,
                "totalOrders", totalOrders,
                "totalAmount", totalAmount,
                "statusBreakdown", statusCounts,
                "dailyBreakdown", dailyAmounts,
                "orders", orders.stream()
                        .map(order -> toOrderDTO(order, itemCountsByOrderId, agreementNumbersByHcfId))
                        .toList()));
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
        String normalizedPeriod = normalizePeriod(period);
        java.time.Instant startDate = startDateForPeriod(normalizedPeriod, now);
        long exportRows = orderRepo.countByFacilityIdAndOrderedAtAfter(facilityId, startDate);
        if (exportRows > MAX_EXPORT_ORDER_ROWS) {
            return ResponseEntity.status(413)
                    .cacheControl(CacheControl.noStore())
                    .body("Export contains " + exportRows + " orders; narrow the period before exporting.");
        }

        List<ConsumableOrder> orders = orderRepo.findExportRowsByFacilityIdAndOrderedAtAfter(facilityId, startDate);
        Map<UUID, String> agreementNumbersByHcfId = agreementNumbersByHcfId(facilityId, orders);

        StringBuilder csv = new StringBuilder();
        // Orders Summary section
        csv.append("=== ORDERS SUMMARY ===\n");
        csv.append(
                "Order Number,HCF Name,Agreement Number,Status,Total Items,Subtotal,GST,Total,Ordered At,Confirmed At,Dispatched At,Delivered At\n");

        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm")
                .withZone(java.time.ZoneId.systemDefault());

        for (ConsumableOrder order : orders) {
            String agreementNum = agreementNumbersByHcfId.getOrDefault(order.getHcf().getId(), "");

            csv.append(String.format(Locale.ROOT,
                    "\"%s\",\"%s\",\"%s\",\"%s\",%d,%.2f,%.2f,%.2f,\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    csvCell(order.getOrderNumber()),
                    csvCell(order.getHcf().getName()),
                    csvCell(agreementNum),
                    csvCell(order.getStatus()),
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
                csv.append(String.format(Locale.ROOT,
                        "\"%s\",\"%s\",\"%s\",%d,\"%s\",%.2f,%.2f,%.2f,%.2f,%.2f\n",
                        csvCell(order.getOrderNumber()),
                        csvCell(order.getHcf().getName()),
                        csvCell(item.getItemName()),
                        item.getQuantity(),
                        csvCell(item.getUnitOfMeasure()),
                        item.getPricePerUnit(),
                        item.getGstRate(),
                        item.getLineSubtotal(),
                        item.getLineGst(),
                        item.getLineTotal()));
            }
        }

        String filename = "consumable_orders_" + normalizedPeriod + "_" +
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").withZone(java.time.ZoneId.systemDefault())
                        .format(now)
                + ".csv";

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", "text/csv; charset=utf-8")
                .body(csv.toString());
    }

    private static String csvCell(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ');
        String trimmedLeading = normalized.stripLeading();
        if (!trimmedLeading.isEmpty() && isSpreadsheetFormulaPrefix(trimmedLeading.charAt(0))) {
            normalized = "'" + normalized;
        }
        return normalized.replace("\"", "\"\"");
    }

    private static String normalizePeriod(String period) {
        if (period == null) {
            return "month";
        }
        return switch (period.toLowerCase(Locale.ROOT)) {
            case "day" -> "day";
            case "week" -> "week";
            default -> "month";
        };
    }

    private static Instant startDateForPeriod(String period, Instant now) {
        return switch (period) {
            case "day" -> now.minus(1, java.time.temporal.ChronoUnit.DAYS);
            case "week" -> now.minus(7, java.time.temporal.ChronoUnit.DAYS);
            default -> now.minus(30, java.time.temporal.ChronoUnit.DAYS);
        };
    }

    private static boolean isSpreadsheetFormulaPrefix(char value) {
        return value == '=' || value == '+' || value == '-' || value == '@' || value == '\t';
    }

    private Map<String, Object> toOrderDTO(ConsumableOrder order) {
        Map<UUID, String> agreementNumbersByHcfId = agreementRepo
                .findByHcfIdAndStatus(order.getHcf().getId(), Agreement.Status.ACTIVE.name())
                .stream()
                .findFirst()
                .map(agreement -> Map.of(order.getHcf().getId(), agreement.getAgreementNumber()))
                .orElseGet(Map::of);
        return toOrderDTO(order, Map.of(order.getId(), (long) order.getItems().size()), agreementNumbersByHcfId);
    }

    private Map<String, Object> toOrderDTO(
            ConsumableOrder order,
            Map<UUID, Long> itemCountsByOrderId,
            Map<UUID, String> agreementNumbersByHcfId) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", order.getId().toString());
        dto.put("orderNumber", order.getOrderNumber());
        dto.put("status", order.getStatus());
        dto.put("itemCount", itemCountsByOrderId.getOrDefault(order.getId(), 0L));
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

        String agreementNumber = agreementNumbersByHcfId.get(hcf.getId());
        if (agreementNumber != null) {
            dto.put("agreementNumber", agreementNumber);
        }

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

    private Map<UUID, String> agreementNumbersByHcfId(UUID facilityId, List<ConsumableOrder> orders) {
        List<UUID> hcfIds = orders.stream()
                .map(order -> order.getHcf().getId())
                .distinct()
                .toList();
        if (hcfIds.isEmpty()) {
            return Map.of();
        }
        return agreementRepo.findActiveAgreementNumbersByFacilityAndHcfIds(facilityId, hcfIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (String) row[1],
                        (first, ignored) -> first));
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
        @Size(max = MAX_ORDER_NOTE_LENGTH, message = "Notes must be 1000 characters or fewer")
        public String notes;
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

    private static String normalizeOptionalNotes(String notes) {
        if (notes == null) {
            return null;
        }
        String normalized = notes.strip();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > MAX_ORDER_NOTE_LENGTH) {
            throw new IllegalArgumentException("Notes must be " + MAX_ORDER_NOTE_LENGTH + " characters or fewer");
        }
        return normalized;
    }

    private static String appendNotes(String existing, String notes) {
        if (existing == null || existing.isBlank()) {
            return notes;
        }
        return existing + "\n" + notes;
    }
}
