package com.smartcbwtf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final BagEventRepository bagEventRepository;
    private final HcfRepository hcfRepository;
    private final VehicleRepository vehicleRepository;
    private final ObjectMapper objectMapper;

    public ComplianceDataAggregator(
            BagEventRepository bagEventRepository,
            HcfRepository hcfRepository,
            VehicleRepository vehicleRepository,
            ObjectMapper objectMapper) {
        this.bagEventRepository = bagEventRepository;
        this.hcfRepository = hcfRepository;
        this.vehicleRepository = vehicleRepository;
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
            String category = event.getBagLabel().getCategory();
            BigDecimal weight = event.getWeightKg();

            totalWaste = totalWaste.add(weight);
            categoryWise.merge(category, weight, BigDecimal::add);
            hcfIds.add(event.getHcf().getId());
            collectedBagIds.add(event.getBagLabel().getId());

            // Check for GPS anomalies
            if ("OUT_OF_GEOFENCE".equals(event.getAnomalyState())) {
                violations.add("GPS_ANOMALY:" + event.getBagLabel().getQrCode());
            }
        }

        for (var event : verificationEvents) {
            verifiedBagIds.add(event.getBagLabel().getId());
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
            collectors.add(event.getCollectedByUserId());
        }

        // Determine status
        boolean hasViolations = !violations.isEmpty();

        return new DailyAggregation(
                date,
                from,
                to,
                totalWaste,
                categoryWise,
                hcfIds.size(),
                collectors.size(),
                0, // missedPickups - needs separate logic
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
            String category = event.getBagLabel().getCategory();
            BigDecimal weight = event.getWeightKg();

            totalWaste = totalWaste.add(weight);
            categoryWise.merge(category, weight, BigDecimal::add);
            hcfWaste.merge(event.getHcf().getId(), weight, BigDecimal::add);
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
}
