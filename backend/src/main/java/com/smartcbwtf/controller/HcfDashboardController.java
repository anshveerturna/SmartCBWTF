package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import com.smartcbwtf.service.HcfAccessGuard;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.format.TextStyle;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/hcf/dashboard")
@PreAuthorize("hasRole('HCF_ADMIN')")
public class HcfDashboardController {
        private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Kolkata");
        private static final String UNKNOWN_CATEGORY = "UNKNOWN";

        private final BagEventRepository bagEventRepository;
        private final InvoiceRepository invoiceRepository;
        private final HcfAccessGuard accessGuard;

        public HcfDashboardController(
                        BagEventRepository bagEventRepository,
                        InvoiceRepository invoiceRepository,
                        HcfAccessGuard accessGuard) {
                this.bagEventRepository = bagEventRepository;
                this.invoiceRepository = invoiceRepository;
                this.accessGuard = accessGuard;
        }

        @GetMapping
        public ResponseEntity<DashboardStats> getStats() {
                UUID hcfId = TenantContext.getHcfId();
                UUID facilityId = TenantContext.getTenantId();
                accessGuard.assertPortalAccess(hcfId, facilityId);

                Instant now = Instant.now();
                ZoneId zone = REPORT_ZONE;
                LocalDate today = LocalDate.now(zone);
                Instant todayStart = today.atStartOfDay(zone).toInstant();
                Instant todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();
                Instant monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();

                // 1. Today's Waste (Total Kg)
                BigDecimal todayWeight = bagEventRepository.sumWeightByFacilityIdAndHcfIdAndEventTsBetween(
                                facilityId, hcfId, todayStart, todayEnd);

                // 2. Month Pickups (Total Bags)
                long monthBags = bagEventRepository.countByFacilityIdAndHcfIdAndEventTsBetween(
                                facilityId, hcfId, monthStart, now);

                // 3. Dues Status
                long unpaidCount = invoiceRepository.countUnpaidByFacilityIdAndHcfId(facilityId, hcfId);
                String duesStatus = unpaidCount == 0 ? "Dues Clear" : "Dues Pending";
                String duesMessage = unpaidCount == 0 ? "No pending invoices" : unpaidCount + " invoice(s) pending";

                // Fetch dashboard aggregates (Last 30 days)
                Instant thirtyDaysAgo = today.minusDays(30).atStartOfDay(zone).toInstant();
                long recentEventCount = bagEventRepository.countByFacilityIdAndHcfIdAndEventTsBetween(
                                facilityId, hcfId, thirtyDaysAgo, now);
                long okEventCount = bagEventRepository.countOkByFacilityIdAndHcfIdAndEventTsBetween(
                                facilityId, hcfId, thirtyDaysAgo, now);

                // 4. Compliance Score (Overall)
                double complianceScore = percentage(okEventCount, recentEventCount);

                // 5. Recent Pickups
                List<RecentPickup> recentPickups = bagEventRepository
                                .summarizePickupsByDayForFacilityAndHcf(facilityId, hcfId, thirtyDaysAgo, now)
                                .stream()
                                .limit(5)
                                .map(HcfDashboardController::recentPickup)
                                .toList();

                // 6. Category Split (Donut Chart) - Last 30 days
                Map<String, Double> categorySplit = categorySplit(
                                bagEventRepository.sumWeightGroupedByCategoryForFacilityAndHcfBetween(
                                                facilityId, hcfId, thirtyDaysAgo, now));

                // Ensure all categories present for consistent colors
                String[] allCategories = { "YELLOW", "RED", "WHITE", "BLUE" };
                for (String cat : allCategories) {
                        categorySplit.putIfAbsent(cat, 0.0);
                }

                // 7. Weekly Trend (Stacked Area) - Last 7 days
                List<Map<String, Object>> dailyTrend = new ArrayList<>();
                LocalDate weekStart = today.minusDays(6); // 7 days including today
                Instant weekStartInstant = weekStart.atStartOfDay(zone).toInstant();
                Map<LocalDate, Map<String, Double>> dailyWeights = dailyCategoryWeights(
                                bagEventRepository.sumWeightGroupedByDayAndCategoryForFacilityAndHcf(
                                                facilityId, hcfId, weekStartInstant, todayEnd));

                for (int i = 0; i < 7; i++) {
                        LocalDate date = weekStart.plusDays(i);
                        Map<String, Object> dayData = new HashMap<>();
                        dayData.put("day", date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                        dayData.put("fullDate", date.toString()); // useful for tooltip

                        Map<String, Double> dayWeights = dailyWeights.getOrDefault(date, Collections.emptyMap());

                        for (String cat : allCategories) {
                                dayData.put(cat, dayWeights.getOrDefault(cat, 0.0));
                        }
                        dailyTrend.add(dayData);
                }

                // 8. Blue Compliance
                long blueEventCount = bagEventRepository.countByFacilityIdAndHcfIdAndCategoryAndEventTsBetween(
                                facilityId, hcfId, "BLUE", thirtyDaysAgo, now);
                long okBlueEventCount = bagEventRepository.countOkByFacilityIdAndHcfIdAndCategoryAndEventTsBetween(
                                facilityId, hcfId, "BLUE", thirtyDaysAgo, now);
                double blueCompliance = percentage(okBlueEventCount, blueEventCount);

                return ResponseEntity.ok(new DashboardStats(
                                weightKg(todayWeight),
                                monthBags,
                                duesStatus,
                                duesMessage,
                                (int) complianceScore,
                                recentPickups,
                                categorySplit,
                                dailyTrend,
                                (int) blueCompliance));
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

        private static Map<String, Double> categorySplit(List<Object[]> rows) {
                Map<String, Double> result = new LinkedHashMap<>();
                for (Object[] row : rows) {
                        result.merge(categoryOf(row[0]), weightKg(row[1]), Double::sum);
                }
                return result;
        }

        private static Map<LocalDate, Map<String, Double>> dailyCategoryWeights(List<Object[]> rows) {
                Map<LocalDate, Map<String, Double>> result = new HashMap<>();
                for (Object[] row : rows) {
                        LocalDate date = localDateOf(row[0]);
                        String category = categoryOf(row[1]);
                        double weight = weightKg(row[2]);
                        result.computeIfAbsent(date, ignored -> new HashMap<>())
                                        .merge(category, weight, Double::sum);
                }
                return result;
        }

        private static RecentPickup recentPickup(Object[] row) {
                long anomalies = longValue(row[3]);
                return new RecentPickup(
                                localDateOf(row[0]).toString(),
                                Math.toIntExact(longValue(row[1])),
                                String.format("%.1f kg", weightKg(row[2])),
                                anomalies == 0 ? "Verified" : "Flagged");
        }

        private static String categoryOf(Object category) {
                if (category == null || category.toString().isBlank()) {
                        return UNKNOWN_CATEGORY;
                }
                return category.toString();
        }

        private static LocalDate localDateOf(Object value) {
                if (value instanceof LocalDate localDate) {
                        return localDate;
                }
                if (value instanceof java.sql.Date sqlDate) {
                        return sqlDate.toLocalDate();
                }
                if (value instanceof java.util.Date date) {
                        return date.toInstant().atZone(REPORT_ZONE).toLocalDate();
                }
                return LocalDate.parse(value.toString().substring(0, 10));
        }

        private static double weightKg(BigDecimal weight) {
                return weight != null ? weight.doubleValue() : 0.0;
        }

        private static double weightKg(Object value) {
                if (value == null) {
                        return 0.0;
                }
                if (value instanceof BigDecimal decimal) {
                        return weightKg(decimal);
                }
                if (value instanceof Number number) {
                        return number.doubleValue();
                }
                return Double.parseDouble(value.toString());
        }

        private static long longValue(Object value) {
                if (value == null) {
                        return 0;
                }
                if (value instanceof Number number) {
                        return number.longValue();
                }
                return Long.parseLong(value.toString());
        }

        private static double percentage(long okCount, long totalCount) {
                return totalCount == 0 ? 100.0 : (double) okCount / totalCount * 100.0;
        }
}
