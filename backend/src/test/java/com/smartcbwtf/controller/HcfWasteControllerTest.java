package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.service.HcfAccessGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcfWasteControllerTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Mock
    private BagEventRepository bagEventRepository;
    @Mock
    private HcfAccessGuard accessGuard;

    private HcfWasteController controller;
    private UUID hcfId;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        controller = new HcfWasteController(bagEventRepository, accessGuard);
        hcfId = UUID.randomUUID();
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId,
                "HCF_ADMIN", "hcf-admin"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void dailyWasteUsesIndiaDayBoundaryAndHandlesNullOperationalData() {
        LocalDate date = LocalDate.of(2026, 7, 1);
        BagEvent unknown = event(null, null, Instant.parse("2026-06-30T18:45:00Z"));
        BagEvent yellow = event("YELLOW", new BigDecimal("2.500"), Instant.parse("2026-07-01T08:15:00Z"));
        ArgumentCaptor<Instant> startCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> endCaptor = ArgumentCaptor.forClass(Instant.class);
        when(bagEventRepository.countByFacilityIdAndHcfIdAndEventTsBetween(eq(facilityId), eq(hcfId),
                startCaptor.capture(), endCaptor.capture()))
                .thenReturn(2L);
        when(bagEventRepository.sumWeightByFacilityIdAndHcfIdAndEventTsBetween(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("2.500"));
        when(bagEventRepository.countAndSumWeightGroupedByCategoryForFacilityAndHcfBetweenIncludingUnknown(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        new Object[] { "UNKNOWN", 1L, BigDecimal.ZERO },
                        new Object[] { "YELLOW", 1L, new BigDecimal("2.500") }));
        when(bagEventRepository.findByFacilityIdAndHcfIdAndEventTsBetweenOrderByEventTsDesc(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(unknown, yellow));

        ResponseEntity<?> response = controller.getDailyWaste(date, 200);

        assertEquals(date.atStartOfDay(IST).toInstant(), startCaptor.getValue());
        assertEquals(date.plusDays(1).atStartOfDay(IST).toInstant(), endCaptor.getValue());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(2L, body.get("totalEvents"));
        assertEquals(new BigDecimal("2.500"), body.get("totalWeightKg"));
        assertEquals(200, body.get("eventLimit"));
        Map<?, ?> byCategory = (Map<?, ?>) body.get("byCategory");
        assertTrue(byCategory.containsKey("UNKNOWN"));
        List<?> events = (List<?>) body.get("events");
        assertEquals(BigDecimal.ZERO, ((Map<?, ?>) events.get(0)).get("weightKg"));
        assertEquals("UNKNOWN", ((Map<?, ?>) events.get(0)).get("category"));
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
        verify(bagEventRepository, never()).countByHcfIdAndEventTsBetween(
                eq(hcfId), any(Instant.class), any(Instant.class));
    }

    @Test
    void dailyWasteCapsEventTimelineLimit() {
        LocalDate date = LocalDate.of(2026, 7, 1);
        when(bagEventRepository.countByFacilityIdAndHcfIdAndEventTsBetween(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(1200L);
        when(bagEventRepository.sumWeightByFacilityIdAndHcfIdAndEventTsBetween(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(BigDecimal.ZERO);
        when(bagEventRepository.countAndSumWeightGroupedByCategoryForFacilityAndHcfBetweenIncludingUnknown(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(bagEventRepository.findByFacilityIdAndHcfIdAndEventTsBetweenOrderByEventTsDesc(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        ResponseEntity<?> response = controller.getDailyWaste(date, 5000);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(500, body.get("eventLimit"));
        verify(bagEventRepository).findByFacilityIdAndHcfIdAndEventTsBetweenOrderByEventTsDesc(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class),
                org.mockito.ArgumentMatchers.argThat(pageable ->
                        pageable.getPageSize() == 500));
    }

    @Test
    void weekSummaryFetchesSevenDayWindowInOneQuery() {
        LocalDate today = LocalDate.now(IST);
        BagEvent todayEvent = event("RED", new BigDecimal("1.250"), today.atTime(10, 0).atZone(IST).toInstant());
        when(bagEventRepository.findByFacilityIdAndHcfIdAndEventTsBetween(
                org.mockito.ArgumentMatchers.eq(facilityId),
                org.mockito.ArgumentMatchers.eq(hcfId),
                org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(List.of(todayEvent));

        ResponseEntity<?> response = controller.getWeekSummary();

        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
        verify(bagEventRepository, times(1)).findByFacilityIdAndHcfIdAndEventTsBetween(
                org.mockito.ArgumentMatchers.eq(facilityId),
                org.mockito.ArgumentMatchers.eq(hcfId),
                org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.any(Instant.class));
        verify(bagEventRepository, never()).findByHcfIdAndEventTsBetween(
                org.mockito.ArgumentMatchers.eq(hcfId),
                org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.any(Instant.class));
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        List<?> days = (List<?>) body.get("days");
        assertEquals(7, days.size());
        Map<?, ?> lastDay = (Map<?, ?>) days.get(6);
        assertEquals(today.toString(), lastDay.get("date"));
        assertEquals(1, lastDay.get("eventCount"));
        assertEquals(new BigDecimal("1.250"), lastDay.get("totalWeightKg"));
    }

    private static BagEvent event(String category, BigDecimal weight, Instant eventTs) {
        BagEvent event = new BagEvent();
        event.setId(UUID.randomUUID());
        event.setEventType("HCF_COLLECTION");
        event.setEventTs(eventTs);
        event.setWeightKg(weight);
        if (category != null) {
            BagLabel label = new BagLabel();
            label.setCategory(category);
            event.setBagLabel(label);
        }
        return event;
    }
}
