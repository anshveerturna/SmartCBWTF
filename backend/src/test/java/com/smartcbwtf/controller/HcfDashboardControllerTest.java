package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import com.smartcbwtf.service.HcfAccessGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcfDashboardControllerTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Mock
    private BagEventRepository bagEventRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private HcfAccessGuard accessGuard;

    private HcfDashboardController controller;
    private UUID hcfId;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        controller = new HcfDashboardController(bagEventRepository, invoiceRepository, accessGuard);
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
    void dashboardUsesIndiaDayWindowAndSurvivesIncompleteBagEvents() {
        LocalDate today = LocalDate.now(IST);
        ArgumentCaptor<Instant> todayStart = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> todayEnd = ArgumentCaptor.forClass(Instant.class);

        when(bagEventRepository.sumWeightByFacilityIdAndHcfIdAndEventTsBetween(
                eq(facilityId), eq(hcfId), todayStart.capture(), todayEnd.capture())).thenReturn(null);
        when(bagEventRepository.countByFacilityIdAndHcfIdAndEventTsBetween(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(2L, 4L);
        when(bagEventRepository.countOkByFacilityIdAndHcfIdAndEventTsBetween(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(3L);
        when(invoiceRepository.countUnpaidByFacilityIdAndHcfId(facilityId, hcfId)).thenReturn(2L);
        when(bagEventRepository.summarizePickupsByDayForFacilityAndHcf(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.<Object[]>of(
                        new Object[] { java.sql.Date.valueOf(today), 2L, new BigDecimal("1.250"), 1L }));
        when(bagEventRepository.sumWeightGroupedByCategoryForFacilityAndHcfBetween(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class))).thenReturn(List.<Object[]>of(
                        new Object[] { null, BigDecimal.ZERO },
                        new Object[] { "BLUE", new BigDecimal("1.250") }));
        when(bagEventRepository.sumWeightGroupedByDayAndCategoryForFacilityAndHcf(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class))).thenReturn(List.<Object[]>of(
                        new Object[] { java.sql.Date.valueOf(today), "BLUE", new BigDecimal("1.250") }));
        when(bagEventRepository.countByFacilityIdAndHcfIdAndCategoryAndEventTsBetween(
                eq(facilityId), eq(hcfId), eq("BLUE"), any(Instant.class), any(Instant.class))).thenReturn(2L);
        when(bagEventRepository.countOkByFacilityIdAndHcfIdAndCategoryAndEventTsBetween(
                eq(facilityId), eq(hcfId), eq("BLUE"), any(Instant.class), any(Instant.class))).thenReturn(1L);

        ResponseEntity<HcfDashboardController.DashboardStats> response = controller.getStats();

        assertEquals(today.atStartOfDay(IST).toInstant(), todayStart.getValue());
        assertEquals(today.plusDays(1).atStartOfDay(IST).toInstant(), todayEnd.getValue());
        HcfDashboardController.DashboardStats stats = response.getBody();
        assertEquals(0.0, stats.todayWaste());
        assertEquals(2L, stats.monthPickups());
        assertEquals("Dues Pending", stats.duesStatus());
        assertEquals("2 invoice(s) pending", stats.duesMessage());
        assertEquals(75, stats.complianceScore());
        assertEquals(50, stats.blueCompliance());
        assertEquals(0.0, stats.categorySplit().get("UNKNOWN"));
        assertEquals(1.25, stats.categorySplit().get("BLUE"));
        assertTrue(stats.recentPickups().get(0).weight().contains("1.3 kg"));
        assertEquals("Flagged", stats.recentPickups().get(0).status());
        verify(invoiceRepository).countUnpaidByFacilityIdAndHcfId(facilityId, hcfId);
        verify(invoiceRepository, never()).countUnpaidByHcfId(hcfId);
        verify(invoiceRepository, never()).findUnpaidByHcfIdOrderByDateAsc(hcfId);
        verify(bagEventRepository, never()).sumWeightByHcfIdAndEventTsBetween(
                eq(hcfId), any(Instant.class), any(Instant.class));
        verify(bagEventRepository, never()).countByHcfIdAndEventTsBetween(
                eq(hcfId), any(Instant.class), any(Instant.class));
        verify(bagEventRepository, never()).findByHcfIdAndEventTsAfter(eq(hcfId), any(Instant.class));
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
    }
}
