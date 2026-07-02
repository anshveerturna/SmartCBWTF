package com.smartcbwtf.service;

import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.dto.AnalyticsResponse;
import com.smartcbwtf.repository.BagEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AnalyticsService {

    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Kolkata");
    private static final String UNKNOWN_CATEGORY = "UNKNOWN";

    private final BagEventRepository bagEventRepository;
    private final FeatureGuardService featureGuardService;
    private final long missingBagHours;

    public AnalyticsService(BagEventRepository bagEventRepository,
            FeatureGuardService featureGuardService,
            @Value("${app.alerts.missing-bag-hours:24}") long missingBagHours) {
        this.bagEventRepository = bagEventRepository;
        this.featureGuardService = featureGuardService;
        this.missingBagHours = missingBagHours;
    }

    /**
     * Get analytics for a specific HCF.
     * Requires ADVANCED_ANALYTICS feature to be enabled.
     */
    public AnalyticsResponse hcfAnalytics(UUID hcfId, UUID facilityId, LocalDate start, LocalDate end) {
        // Feature flag enforcement at service layer (MANDATORY)
        featureGuardService.assertEnabled(facilityId, FeatureGuardService.ADVANCED_ANALYTICS);

        Instant from = start.atStartOfDay(REPORT_ZONE).toInstant();
        Instant to = end.plusDays(1).atStartOfDay(REPORT_ZONE).toInstant();
        List<BagEvent> events = bagEventRepository.findByFacilityIdAndHcfIdAndEventTsBetween(
                facilityId, hcfId, from, to);
        return aggregate(events, from, to);
    }

    /**
     * Get analytics for a facility.
     * Requires ADVANCED_ANALYTICS feature to be enabled.
     */
    public AnalyticsResponse facilityAnalytics(UUID facilityId, LocalDate start, LocalDate end) {
        // Feature flag enforcement at service layer (MANDATORY)
        featureGuardService.assertEnabled(facilityId, FeatureGuardService.ADVANCED_ANALYTICS);

        Instant from = start.atStartOfDay(REPORT_ZONE).toInstant();
        Instant to = end.plusDays(1).atStartOfDay(REPORT_ZONE).toInstant();
        List<BagEvent> events = bagEventRepository.findByFacilityIdAndEventTsBetween(facilityId, from, to);
        return aggregate(events, from, to);
    }

    private AnalyticsResponse aggregate(List<BagEvent> events, Instant from, Instant to) {
        BigDecimal total = BigDecimal.ZERO;
        long bagCount = events.size();
        Map<String, BigDecimal> weightByCategory = new HashMap<>();
        Map<String, Long> bagCountByCategory = new HashMap<>();
        Map<String, BigDecimal> weightTrendByDate = new HashMap<>();
        Map<String, Long> bagCountTrendByDate = new HashMap<>();
        long mismatchCount = 0;

        Set<UUID> verifiedLabels = new HashSet<>();
        Set<UUID> collectedLabels = new HashSet<>();
        Map<UUID, Instant> collectedAtByLabel = new HashMap<>();

        for (BagEvent e : events) {
            String cat = categoryOf(e);
            BigDecimal w = weightOf(e);
            total = total.add(w);
            weightByCategory.merge(cat, w, BigDecimal::add);
            bagCountByCategory.merge(cat, 1L, Long::sum);

            Instant eventTs = e.getEventTs();
            if (eventTs != null) {
                LocalDate day = eventTs.atZone(REPORT_ZONE).toLocalDate();
                String dayKey = day.toString();
                weightTrendByDate.merge(dayKey, w, BigDecimal::add);
                bagCountTrendByDate.merge(dayKey, 1L, Long::sum);
            }

            if ("CBWTF_VERIFICATION".equalsIgnoreCase(e.getEventType())
                    && "MISMATCH".equalsIgnoreCase(e.getAnomalyState())) {
                mismatchCount++;
            }

            UUID labelId = labelIdOf(e);
            if (labelId != null && "CBWTF_VERIFICATION".equalsIgnoreCase(e.getEventType())) {
                verifiedLabels.add(labelId);
            }
            if (labelId != null && "HCF_COLLECTION".equalsIgnoreCase(e.getEventType())) {
                collectedLabels.add(labelId);
                if (eventTs != null) {
                    collectedAtByLabel.merge(labelId, eventTs,
                            (existing, candidate) -> existing.isBefore(candidate) ? existing : candidate);
                }
            }
        }

        // missing if collected in window and not verified within window or before
        // cutoff horizon
        Instant cutoff = Instant.now().minusSeconds(missingBagHours * 3600);
        long missingCount = collectedLabels.stream()
                .filter(id -> !verifiedLabels.contains(id))
                .filter(id -> {
                    Instant collectedAt = collectedAtByLabel.get(id);
                    return collectedAt != null && collectedAt.isBefore(cutoff);
                })
                .count();

        return new AnalyticsResponse(total, bagCount, weightByCategory, bagCountByCategory, mismatchCount, missingCount,
                weightTrendByDate, bagCountTrendByDate);
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
}
