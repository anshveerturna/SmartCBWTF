package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.Invoice;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import com.smartcbwtf.service.HcfAccessGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.format.TextStyle;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hcf/dashboard")
@PreAuthorize("hasRole('HCF_ADMIN')")
public class HcfDashboardController {

        private static final Logger log = LoggerFactory.getLogger(HcfDashboardController.class);
        private final BagEventRepository bagEventRepository;
        private final InvoiceRepository invoiceRepository;
        private final BagLabelRepository bagLabelRepository;
        private final HcfRepository hcfRepository;
        private final AgreementRepository agreementRepository;
        private final HcfAccessGuard accessGuard;

        public HcfDashboardController(
                        BagEventRepository bagEventRepository,
                        InvoiceRepository invoiceRepository,
                        BagLabelRepository bagLabelRepository,
                        HcfRepository hcfRepository,
                        AgreementRepository agreementRepository,
                        HcfAccessGuard accessGuard) {
                this.bagEventRepository = bagEventRepository;
                this.invoiceRepository = invoiceRepository;
                this.bagLabelRepository = bagLabelRepository;
                this.hcfRepository = hcfRepository;
                this.agreementRepository = agreementRepository;
                this.accessGuard = accessGuard;
        }

        @GetMapping
        public ResponseEntity<DashboardStats> getStats() {
                UUID hcfId = TenantContext.getHcfId();
                accessGuard.assertPortalAccess(hcfId);

                Instant now = Instant.now();
                ZoneId zone = ZoneId.systemDefault();
                Instant todayStart = LocalDate.now().atStartOfDay(zone).toInstant();
                Instant todayEnd = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant();
                Instant monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay(zone).toInstant();

                // 1. Today's Waste (Total Kg)
                BigDecimal todayWeight = bagEventRepository.sumWeightByHcfIdAndEventTsBetween(hcfId, todayStart,
                                todayEnd);

                // 2. Month Pickups (Total Bags)
                long monthBags = bagEventRepository.countByHcfIdAndEventTsBetween(hcfId, monthStart, now);

                // 3. Dues Status
                List<Invoice> unpaid = invoiceRepository.findUnpaidByHcfIdOrderByDateAsc(hcfId);
                String duesStatus = unpaid.isEmpty() ? "Dues Clear" : "Dues Pending";
                String duesMessage = unpaid.isEmpty() ? "No pending invoices" : unpaid.size() + " invoice(s) pending";

                // Fetch events for charts (Last 30 days)
                Instant thirtyDaysAgo = LocalDate.now().minusDays(30).atStartOfDay(zone).toInstant();
                List<BagEvent> recentEvents = bagEventRepository.findByHcfIdAndEventTsAfter(hcfId, thirtyDaysAgo);

                // 4. Compliance Score (Overall)
                double complianceScore = 100.0;
                if (!recentEvents.isEmpty()) {
                        long okCount = recentEvents.stream()
                                        .filter(e -> e.getAnomalyState() == null || "OK".equals(e.getAnomalyState()))
                                        .count();
                        complianceScore = (double) okCount / recentEvents.size() * 100.0;
                }

                // 5. Recent Pickups
                Map<LocalDate, List<BagEvent>> groupedByDate = recentEvents.stream()
                                .collect(Collectors.groupingBy(e -> e.getEventTs().atZone(zone).toLocalDate()));

                List<RecentPickup> recentPickups = groupedByDate.entrySet().stream()
                                .sorted(Map.Entry.<LocalDate, List<BagEvent>>comparingByKey().reversed())
                                .limit(5)
                                .map(entry -> {
                                        List<BagEvent> bags = entry.getValue();
                                        double totalWeight = bags.stream()
                                                        .mapToDouble(b -> b.getWeightKg().doubleValue()).sum();
                                        boolean allOk = bags.stream().allMatch(b -> b.getAnomalyState() == null
                                                        || "OK".equals(b.getAnomalyState()));
                                        return new RecentPickup(
                                                        entry.getKey().toString(),
                                                        bags.size(),
                                                        String.format("%.1f kg", totalWeight),
                                                        allOk ? "Verified" : "Flagged");
                                })
                                .collect(Collectors.toList());

                // 6. Category Split (Donut Chart) - Last 30 days
                Map<String, Double> categorySplit = recentEvents.stream()
                                .collect(Collectors.groupingBy(
                                                e -> e.getBagLabel().getCategory(),
                                                Collectors.summingDouble(e -> e.getWeightKg().doubleValue())));

                // Ensure all categories present for consistent colors
                String[] allCategories = { "YELLOW", "RED", "WHITE", "BLUE" };
                for (String cat : allCategories) {
                        categorySplit.putIfAbsent(cat, 0.0);
                }

                // 7. Weekly Trend (Stacked Area) - Last 7 days
                List<Map<String, Object>> dailyTrend = new ArrayList<>();
                LocalDate weekStart = LocalDate.now().minusDays(6); // 7 days including today

                for (int i = 0; i < 7; i++) {
                        LocalDate date = weekStart.plusDays(i);
                        Map<String, Object> dayData = new HashMap<>();
                        dayData.put("day", date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                        dayData.put("fullDate", date.toString()); // useful for tooltip

                        // Filter events for this day
                        List<BagEvent> dayEvents = groupedByDate.getOrDefault(date, Collections.emptyList());

                        for (String cat : allCategories) {
                                double weight = dayEvents.stream()
                                                .filter(e -> cat.equals(e.getBagLabel().getCategory()))
                                                .mapToDouble(e -> e.getWeightKg().doubleValue())
                                                .sum();
                                dayData.put(cat, weight);
                        }
                        dailyTrend.add(dayData);
                }

                // 8. Blue Compliance
                double blueCompliance = 100.0;
                List<BagEvent> blueEvents = recentEvents.stream()
                                .filter(e -> "BLUE".equals(e.getBagLabel().getCategory()))
                                .toList();

                if (!blueEvents.isEmpty()) {
                        long okBlue = blueEvents.stream()
                                        .filter(e -> e.getAnomalyState() == null || "OK".equals(e.getAnomalyState()))
                                        .count();
                        blueCompliance = (double) okBlue / blueEvents.size() * 100.0;
                }

                return ResponseEntity.ok(new DashboardStats(
                                todayWeight.doubleValue(),
                                monthBags,
                                duesStatus,
                                duesMessage,
                                (int) complianceScore,
                                recentPickups,
                                categorySplit,
                                dailyTrend,
                                (int) blueCompliance));
        }

        @PostMapping("/seed")
        public ResponseEntity<String> seedData() {
                try {
                        UUID hcfId = TenantContext.getHcfId();
                        Hcf hcf = hcfRepository.findById(hcfId)
                                        .orElseThrow(() -> new RuntimeException("HCF not found"));

                        Agreement agreement = agreementRepository.findActiveByHcfId(hcfId)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "No active agreement found for seeding"));
                        Facility facility = agreement.getFacility();

                        String[] categories = { "YELLOW", "RED", "WHITE", "BLUE" };
                        Random rand = new Random();
                        UUID userId = TenantContext.getUserId();

                        List<BagLabel> labels = new ArrayList<>();
                        // Create 40 labels for better distribution
                        for (int i = 0; i < 40; i++) {
                                BagLabel label = new BagLabel();
                                label.setHcf(hcf);
                                label.setFacility(facility);
                                label.setCategory(categories[rand.nextInt(4)]);
                                label.setQrCode(UUID.randomUUID().toString());
                                label.setSerialNo("TEST-" + UUID.randomUUID().toString().substring(0, 8));
                                label.setStatus("USED");
                                label.setIssuedAt(Instant.now().minus(30, ChronoUnit.DAYS));
                                labels.add(bagLabelRepository.save(label));
                        }

                        for (BagLabel label : labels) {
                                BagEvent event = new BagEvent();
                                event.setBagLabel(label);
                                event.setFacility(facility);
                                event.setHcf(hcf);
                                event.setEventType("HCF_COLLECTION");

                                long offset = rand.nextInt(30 * 24 * 3600); // 30 days
                                // Force bias towards last 7 days for trend chart
                                if (rand.nextDouble() > 0.3) {
                                        offset = rand.nextInt(7 * 24 * 3600);
                                }

                                event.setEventTs(Instant.now().minus(offset, ChronoUnit.SECONDS));
                                event.setWeightKg(BigDecimal.valueOf(1.0 + rand.nextDouble() * 10.0));
                                event.setGpsLat(12.9716);
                                event.setGpsLon(77.5946);
                                event.setCollectedByUserId(userId);
                                event.setAnomalyState(rand.nextDouble() > 0.9 ? "MISMATCH" : "OK");
                                bagEventRepository.save(event);
                        }

                        Invoice inv = new Invoice();
                        inv.setHcf(hcf);
                        inv.setFacility(facility);
                        inv.setAgreement(agreement);
                        inv.setInvoiceNumber("INV-TEST-" + System.currentTimeMillis());
                        inv.setTotalAmount(BigDecimal.valueOf(12345.00));
                        inv.setStatus("PENDING");
                        inv.setInvoiceDate(LocalDate.now());
                        inv.setPeriodStart(LocalDate.now().withDayOfMonth(1));
                        inv.setPeriodEnd(LocalDate.now());
                        inv.setTotalAmount(BigDecimal.valueOf(45000));
                        inv.setBaseAmount(BigDecimal.valueOf(45000));
                        inv.setTaxAmount(BigDecimal.ZERO);
                        inv.setPerBedPerDayRate(BigDecimal.valueOf(10.0));
                        inv.setBeds(150);
                        inv.setFinancialYear("2025-2026");
                        invoiceRepository.save(inv);

                        return ResponseEntity.ok("Seeded");
                } catch (Exception e) {
                        log.error("Failed to seed dashboard data", e);
                        return ResponseEntity.internalServerError()
                                        .body("Error: " + e.getMessage() + " | Trace: " + e.toString());
                }
        }

        public record DashboardStats(
                        Double todayWaste,
                        Long monthPickups,
                        String duesStatus,
                        String duesMessage,
                        Integer complianceScore,
                        List<RecentPickup> recentPickups,
                        Map<String, Double> categorySplit, // New
                        List<Map<String, Object>> dailyTrend, // New
                        Integer blueCompliance // New
        ) {
        }

        public record RecentPickup(
                        String date,
                        Integer bags,
                        String weight,
                        String status) {
        }
}
