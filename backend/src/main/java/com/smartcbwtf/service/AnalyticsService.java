package com.smartcbwtf.service;

import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.dto.AnalyticsResponse;
import com.smartcbwtf.repository.BagEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final BagEventRepository bagEventRepository;
    private final long missingBagHours;

    public AnalyticsService(BagEventRepository bagEventRepository,
                            @Value("${app.alerts.missing-bag-hours:24}") long missingBagHours) {
        this.bagEventRepository = bagEventRepository;
        this.missingBagHours = missingBagHours;
    }

    public AnalyticsResponse hcfAnalytics(UUID hcfId, LocalDate start, LocalDate end) {
        Instant from = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<BagEvent> events = bagEventRepository.findByHcfIdAndEventTsBetween(hcfId, from, to);
        return aggregate(events, from, to);
    }

    public AnalyticsResponse facilityAnalytics(UUID facilityId, LocalDate start, LocalDate end) {
        Instant from = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
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

        for (BagEvent e : events) {
            String cat = e.getBagLabel().getCategory();
            BigDecimal w = e.getWeightKg();
            total = total.add(w);
            weightByCategory.merge(cat, w, BigDecimal::add);
            bagCountByCategory.merge(cat, 1L, Long::sum);

            LocalDate day = e.getEventTs().atZone(ZoneOffset.UTC).toLocalDate();
            String dayKey = day.toString();
            weightTrendByDate.merge(dayKey, w, BigDecimal::add);
            bagCountTrendByDate.merge(dayKey, 1L, Long::sum);

            if ("CBWTF_VERIFICATION".equalsIgnoreCase(e.getEventType()) && "MISMATCH".equalsIgnoreCase(e.getAnomalyState())) {
                mismatchCount++;
                verifiedLabels.add(e.getBagLabel().getId());
            }
            if ("CBWTF_VERIFICATION".equalsIgnoreCase(e.getEventType())) {
                verifiedLabels.add(e.getBagLabel().getId());
            }
            if ("HCF_COLLECTION".equalsIgnoreCase(e.getEventType())) {
                collectedLabels.add(e.getBagLabel().getId());
            }
        }

        // missing if collected in window and not verified within window or before cutoff horizon
        Instant cutoff = Instant.now().minusSeconds(missingBagHours * 3600);
        long missingCount = collectedLabels.stream()
                .filter(id -> !verifiedLabels.contains(id))
                .filter(id -> events.stream().anyMatch(ev -> "HCF_COLLECTION".equalsIgnoreCase(ev.getEventType())
                        && ev.getBagLabel().getId().equals(id)
                        && ev.getEventTs().isBefore(cutoff)))
                .count();

        return new AnalyticsResponse(total, bagCount, weightByCategory, bagCountByCategory, mismatchCount, missingCount, weightTrendByDate, bagCountTrendByDate);
    }
}
