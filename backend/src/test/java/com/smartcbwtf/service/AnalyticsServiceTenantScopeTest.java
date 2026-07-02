package com.smartcbwtf.service;

import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.dto.AnalyticsResponse;
import com.smartcbwtf.repository.BagEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTenantScopeTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Mock
    private BagEventRepository bagEventRepository;
    @Mock
    private FeatureGuardService featureGuardService;

    @Test
    void hcfAnalyticsFiltersEventsByFacilityAndHcf() {
        AnalyticsService service = new AnalyticsService(bagEventRepository, featureGuardService, 24);
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        Instant from = start.atStartOfDay(IST).toInstant();
        Instant to = end.plusDays(1).atStartOfDay(IST).toInstant();
        when(bagEventRepository.findByFacilityIdAndHcfIdAndEventTsBetween(facilityId, hcfId, from, to))
                .thenReturn(List.of());

        service.hcfAnalytics(hcfId, facilityId, start, end);

        verify(featureGuardService).assertEnabled(facilityId, FeatureGuardService.ADVANCED_ANALYTICS);
        verify(bagEventRepository).findByFacilityIdAndHcfIdAndEventTsBetween(facilityId, hcfId, from, to);
    }

    @Test
    void facilityAnalyticsToleratesLegacyRowsAndCountsMissingBags() {
        AnalyticsService service = new AnalyticsService(bagEventRepository, featureGuardService, 24);
        UUID facilityId = UUID.randomUUID();
        LocalDate date = LocalDate.now(IST).minusDays(3);
        Instant from = date.atStartOfDay(IST).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(IST).toInstant();

        UUID collectedLabelId = UUID.randomUUID();
        BagEvent collection = event("HCF_COLLECTION", date.atTime(10, 0).atZone(IST).toInstant(), null);
        BagLabel blankCategoryLabel = new BagLabel();
        blankCategoryLabel.setId(collectedLabelId);
        blankCategoryLabel.setCategory(" ");
        collection.setBagLabel(blankCategoryLabel);

        BagEvent mismatchWithMissingLabel = event("CBWTF_VERIFICATION", date.atTime(11, 0).atZone(IST).toInstant(),
                new BigDecimal("2.500"));
        mismatchWithMissingLabel.setAnomalyState("MISMATCH");

        when(bagEventRepository.findByFacilityIdAndEventTsBetween(facilityId, from, to))
                .thenReturn(List.of(collection, mismatchWithMissingLabel));

        AnalyticsResponse response = service.facilityAnalytics(facilityId, date, date);

        assertEquals(new BigDecimal("2.500"), response.getTotalWeightKg());
        assertEquals(2, response.getBagCount());
        assertEquals(new BigDecimal("2.500"), response.getWeightByCategory().get("UNKNOWN"));
        assertEquals(2L, response.getBagCountByCategory().get("UNKNOWN"));
        assertEquals(1, response.getMismatchCount());
        assertEquals(1, response.getMissingCount());
        assertEquals(new BigDecimal("2.500"), response.getWeightTrendByDate().get(date.toString()));
        assertEquals(2L, response.getBagCountTrendByDate().get(date.toString()));
    }

    private static BagEvent event(String eventType, Instant eventTs, BigDecimal weightKg) {
        BagEvent event = new BagEvent();
        event.setEventType(eventType);
        event.setEventTs(eventTs);
        event.setWeightKg(weightKg);
        return event;
    }
}
