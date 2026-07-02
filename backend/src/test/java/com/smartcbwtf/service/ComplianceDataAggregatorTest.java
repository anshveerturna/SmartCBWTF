package com.smartcbwtf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.RouteCycleHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceDataAggregatorTest {

    @Mock
    private BagEventRepository bagEventRepository;
    @Mock
    private RouteCycleHistoryRepository routeCycleHistoryRepository;

    @Test
    void aggregateDailyToleratesIncompleteLegacyEvents() {
        ComplianceDataAggregator aggregator = aggregator();
        UUID facilityId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 1, 2);
        Instant from = Instant.parse("2026-01-01T18:30:00Z");
        Instant to = Instant.parse("2026-01-02T18:30:00Z");

        BagEvent incompleteCollection = new BagEvent();
        incompleteCollection.setAnomalyState("OUT_OF_GEOFENCE");

        UUID labelId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        UUID collectorId = UUID.randomUUID();
        BagEvent completeCollection = collection(labelId, hcfId, collectorId, "RED", new BigDecimal("1.250"));
        BagEvent verification = verification(labelId);

        when(bagEventRepository.findByFacilityIdAndEventTypeAndEventTsBetween(
                facilityId, "HCF_COLLECTION", from, to)).thenReturn(List.of(incompleteCollection, completeCollection));
        when(bagEventRepository.findByFacilityIdAndEventTypeAndEventTsBetween(
                facilityId, "CBWTF_VERIFICATION", from, to)).thenReturn(List.of(verification));
        when(routeCycleHistoryRepository.sumMissedWaypointsByFacilityAndDate(facilityId, date)).thenReturn(3L);

        ComplianceDataAggregator.DailyAggregation result = aggregator.aggregateDaily(facilityId, date, from, to);

        assertEquals(new BigDecimal("1.250"), result.totalWasteKg());
        assertEquals(new BigDecimal("1.250"), result.categoryWiseKg().get("RED"));
        assertEquals(BigDecimal.ZERO, result.categoryWiseKg().get("UNKNOWN"));
        assertEquals(1, result.hcfsServiced());
        assertEquals(1, result.vehiclesDeployed());
        assertEquals(3, result.missedPickups());
        assertEquals(0, result.unverifiedBags());
        assertEquals(List.of("GPS_ANOMALY:UNKNOWN_QR"), result.violations());
        assertTrue(result.hasViolations());
    }

    @Test
    void aggregateMonthlyKeepsTotalsWhenLabelOrHcfIsMissing() {
        ComplianceDataAggregator aggregator = aggregator();
        UUID facilityId = UUID.randomUUID();
        LocalDate monthStart = LocalDate.of(2026, 1, 1);
        Instant from = Instant.parse("2025-12-31T18:30:00Z");
        Instant to = Instant.parse("2026-01-31T18:30:00Z");

        BagEvent legacyEvent = new BagEvent();
        legacyEvent.setWeightKg(new BigDecimal("2.500"));

        when(bagEventRepository.findByFacilityIdAndEventTypeAndEventTsBetween(
                facilityId, "HCF_COLLECTION", from, to)).thenReturn(List.of(legacyEvent));

        ComplianceDataAggregator.MonthlyAggregation result = aggregator.aggregateMonthly(facilityId, monthStart, from, to);

        assertEquals(new BigDecimal("2.500"), result.totalWasteKg());
        assertEquals(new BigDecimal("2.500"), result.categoryWiseKg().get("UNKNOWN"));
        assertTrue(result.hcfWiseWaste().isEmpty());
        assertEquals(0, result.hcfCount());
    }

    private ComplianceDataAggregator aggregator() {
        return new ComplianceDataAggregator(bagEventRepository, routeCycleHistoryRepository, new ObjectMapper());
    }

    private static BagEvent collection(UUID labelId, UUID hcfId, UUID collectorId, String category, BigDecimal weight) {
        BagLabel label = new BagLabel();
        label.setId(labelId);
        label.setCategory(category);
        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        BagEvent event = new BagEvent();
        event.setBagLabel(label);
        event.setHcf(hcf);
        event.setCollectedByUserId(collectorId);
        event.setWeightKg(weight);
        return event;
    }

    private static BagEvent verification(UUID labelId) {
        BagLabel label = new BagLabel();
        label.setId(labelId);
        BagEvent event = new BagEvent();
        event.setBagLabel(label);
        return event;
    }
}
