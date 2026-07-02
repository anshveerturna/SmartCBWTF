package com.smartcbwtf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.RouteCycleHistoryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Compliance Data Aggregator - Pure aggregation logic.
 * 
 * CRITICAL:
 * - Deterministic: Same inputs ALWAYS produce same outputs
 * - No side effects
 * - No database writes
 */
@Service
public class ComplianceDataAggregator {

    private static final String UNKNOWN_CATEGORY = "UNKNOWN";
    private static final String UNKNOWN_QR = "UNKNOWN_QR";

    private final BagEventRepository bagEventRepository;
    private final RouteCycleHistoryRepository routeCycleHistoryRepository;
    private final ObjectMapper objectMapper;

    public ComplianceDataAggregator(
            BagEventRepository bagEventRepository,
            RouteCycleHistoryRepository routeCycleHistoryRepository,
            ObjectMapper objectMapper) {
        this.bagEventRepository = bagEventRepository;
        this.routeCycleHistoryRepository = routeCycleHistoryRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Aggregate daily data for a facility.
     */
    public DailyAggregation aggregateDaily(UUID facilityId, LocalDate date, Instant from, Instant to) {
        // Get all collection events for the day
        var collectionEvents = bagEventRepository.findByFacilityIdAndEventTypeAndEventTsBetween(
                facilityId, "HCF_COLLECTION", from, to);

        // Get all verification events for the day
        var verificationEvents = bagEventRepository.findByFacilityIdAndEventTypeAndEventTsBetween(
                facilityId, "CBWTF_VERIFICATION", from, to);

        // Aggregate by category
        Map<String, BigDecimal> categoryWise = new HashMap<>();
        categoryWise.put("YELLOW", BigDecimal.ZERO);
        categoryWise.put("RED", BigDecimal.ZERO);
        categoryWise.put("BLUE", BigDecimal.ZERO);
        categoryWise.put("WHITE", BigDecimal.ZERO);

        BigDecimal totalWaste = BigDecimal.ZERO;
        Set<UUID> hcfIds = new HashSet<>();
        Set<UUID> collectedBagIds = new HashSet<>();
        Set<UUID> verifiedBagIds = new HashSet<>();
        List<String> violations = new ArrayList<>();

        for (var event : collectionEvents) {
            String category = categoryOf(event);
            BigDecimal weight = weightOf(event);

            totalWaste = totalWaste.add(weight);
            categoryWise.merge(category, weight, BigDecimal::add);
            UUID hcfId = hcfIdOf(event);
            if (hcfId != null) {
                hcfIds.add(hcfId);
            }
            UUID labelId = labelIdOf(event);
            if (labelId != null) {
                collectedBagIds.add(labelId);
            }

            // Check for GPS anomalies
            if ("OUT_OF_GEOFENCE".equals(event.getAnomalyState())) {
                violations.add("GPS_ANOMALY:" + qrCodeOf(event));
            }
        }

        for (var event : verificationEvents) {
            UUID labelId = labelIdOf(event);
            if (labelId != null) {
                verifiedBagIds.add(labelId);
            }
        }

        // Find unverified bags (collected but not verified)
        Set<UUID> unverifiedBagIds = new HashSet<>(collectedBagIds);
        unverifiedBagIds.removeAll(verifiedBagIds);

        if (!unverifiedBagIds.isEmpty()) {
            violations.add("UNVERIFIED_BAGS:" + unverifiedBagIds.size());
        }

        // Count vehicles deployed (distinct collectors)
        Set<UUID> collectors = new HashSet<>();
        for (var event : collectionEvents) {
            if (event.getCollectedByUserId() != null) {
                collectors.add(event.getCollectedByUserId());
            }
        }

        // Determine status
        boolean hasViolations = !violations.isEmpty();
        int missedPickups = Math.toIntExact(
                routeCycleHistoryRepository.sumMissedWaypointsByFacilityAndDate(facilityId, date));

        return new DailyAggregation(
                date,
                from,
                to,
                totalWaste,
                categoryWise,
                hcfIds.size(),
                collectors.size(),
                missedPickups,
                unverifiedBagIds.size(),
                violations,
                hasViolations);
    }

    /**
     * Convert daily aggregation to JSON string.
     */
    public String toJson(DailyAggregation data) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("reportDate", data.reportDate().toString());
            root.put("totalWasteKg", data.totalWasteKg());

            ObjectNode categories = root.putObject("categoryWise");
            data.categoryWiseKg().forEach(categories::put);

            root.put("hcfsServiced", data.hcfsServiced());
            root.put("vehiclesDeployed", data.vehiclesDeployed());
            root.put("missedPickups", data.missedPickups());
            root.put("unverifiedBags", data.unverifiedBags());

            ArrayNode violationsArray = root.putArray("violations");
            data.violations().forEach(violationsArray::add);

            ObjectNode sourceWindow = root.putObject("sourceWindow");
            sourceWindow.put("from", data.sourceFrom().toString());
            sourceWindow.put("to", data.sourceTo().toString());

            root.put("generatedAt", Instant.now().toString());

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize daily aggregation", e);
        }
    }

    /**
     * Result record for daily aggregation.
     */
    public record DailyAggregation(
            LocalDate reportDate,
            Instant sourceFrom,
            Instant sourceTo,
            BigDecimal totalWasteKg,
            Map<String, BigDecimal> categoryWiseKg,
            int hcfsServiced,
            int vehiclesDeployed,
            int missedPickups,
            int unverifiedBags,
            List<String> violations,
            boolean hasViolations) {
    }

    /**
     * Aggregate monthly data.
     */
    public MonthlyAggregation aggregateMonthly(UUID facilityId, LocalDate monthStart, Instant from, Instant to) {
        // Similar to daily but for entire month
        var collectionEvents = bagEventRepository.findByFacilityIdAndEventTypeAndEventTsBetween(
                facilityId, "HCF_COLLECTION", from, to);

        Map<String, BigDecimal> categoryWise = new HashMap<>();
        categoryWise.put("YELLOW", BigDecimal.ZERO);
        categoryWise.put("RED", BigDecimal.ZERO);
        categoryWise.put("BLUE", BigDecimal.ZERO);
        categoryWise.put("WHITE", BigDecimal.ZERO);

        BigDecimal totalWaste = BigDecimal.ZERO;
        Map<UUID, BigDecimal> hcfWaste = new HashMap<>();

        for (var event : collectionEvents) {
            String category = categoryOf(event);
            BigDecimal weight = weightOf(event);

            totalWaste = totalWaste.add(weight);
            categoryWise.merge(category, weight, BigDecimal::add);
            UUID hcfId = hcfIdOf(event);
            if (hcfId != null) {
                hcfWaste.merge(hcfId, weight, BigDecimal::add);
            }
        }

        return new MonthlyAggregation(
                monthStart,
                from, to,
                totalWaste,
                categoryWise,
                hcfWaste,
                hcfWaste.size());
    }

    public record MonthlyAggregation(
            LocalDate reportMonth,
            Instant sourceFrom,
            Instant sourceTo,
            BigDecimal totalWasteKg,
            Map<String, BigDecimal> categoryWiseKg,
            Map<UUID, BigDecimal> hcfWiseWaste,
            int hcfCount) {
    }

    /**
     * Convert monthly aggregation to JSON.
     */
    public String toJson(MonthlyAggregation data) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("reportMonth", data.reportMonth().toString());
            root.put("totalWasteKg", data.totalWasteKg());

            ObjectNode categories = root.putObject("categoryWise");
            data.categoryWiseKg().forEach(categories::put);

            root.put("hcfCount", data.hcfCount());

            ObjectNode sourceWindow = root.putObject("sourceWindow");
            sourceWindow.put("from", data.sourceFrom().toString());
            sourceWindow.put("to", data.sourceTo().toString());

            root.put("generatedAt", Instant.now().toString());

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize monthly aggregation", e);
        }
    }

    private static BigDecimal weightOf(BagEvent event) {
        return event.getWeightKg() != null ? event.getWeightKg() : BigDecimal.ZERO;
    }

    private static String categoryOf(BagEvent event) {
        if (event.getBagLabel() == null || event.getBagLabel().getCategory() == null
                || event.getBagLabel().getCategory().isBlank()) {
            return UNKNOWN_CATEGORY;
        }
        return event.getBagLabel().getCategory();
    }

    private static UUID labelIdOf(BagEvent event) {
        return event.getBagLabel() != null ? event.getBagLabel().getId() : null;
    }

    private static UUID hcfIdOf(BagEvent event) {
        return event.getHcf() != null ? event.getHcf().getId() : null;
    }

    private static String qrCodeOf(BagEvent event) {
        UUID labelId = labelIdOf(event);
        if (event.getBagLabel() == null || event.getBagLabel().getQrCode() == null
                || event.getBagLabel().getQrCode().isBlank()) {
            return labelId != null ? labelId.toString() : UNKNOWN_QR;
        }
        return event.getBagLabel().getQrCode();
    }
}
