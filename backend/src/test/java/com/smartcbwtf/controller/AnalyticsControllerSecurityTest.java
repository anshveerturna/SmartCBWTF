package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.dto.AnalyticsResponse;
import com.smartcbwtf.repository.DailyWasteSnapshotRepository;
import com.smartcbwtf.repository.MonthlyWasteSnapshotRepository;
import com.smartcbwtf.service.AnalyticsPageService;
import com.smartcbwtf.service.AnalyticsService;
import com.smartcbwtf.service.HcfAccessGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerSecurityTest {

    @Mock
    private AnalyticsService analyticsService;
    @Mock
    private DailyWasteSnapshotRepository dailySnapshotRepository;
    @Mock
    private MonthlyWasteSnapshotRepository monthlySnapshotRepository;
    @Mock
    private AnalyticsPageService analyticsPageService;
    @Mock
    private HcfAccessGuard hcfAccessGuard;

    private AnalyticsController controller;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        controller = new AnalyticsController(
                analyticsService,
                dailySnapshotRepository,
                monthlySnapshotRepository,
                analyticsPageService,
                hcfAccessGuard);
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void facilityAnalyticsRejectsPathFacilityMismatch() {
        UUID requestedFacilityId = UUID.randomUUID();

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.facility(requestedFacilityId, LocalDate.now().minusDays(1), LocalDate.now()));

        assertEquals(404, thrown.getStatusCode().value());
        verify(analyticsService, never()).facilityAnalytics(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void facilityAnalyticsUsesAuthenticatedTenantFacility() {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        AnalyticsResponse expected = emptyAnalyticsResponse();
        when(analyticsService.facilityAnalytics(facilityId, start, end)).thenReturn(expected);

        AnalyticsResponse response = controller.facility(facilityId, start, end);

        assertEquals(expected, response);
        verify(analyticsService).facilityAnalytics(facilityId, start, end);
    }

    @Test
    void hcfAnalyticsPassesAuthenticatedTenantFacilityToService() {
        UUID hcfId = UUID.randomUUID();
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        AnalyticsResponse expected = emptyAnalyticsResponse();
        when(analyticsService.hcfAnalytics(hcfId, facilityId, start, end)).thenReturn(expected);

        AnalyticsResponse response = controller.hcf(hcfId, start, end);

        assertEquals(expected, response);
        verify(analyticsService).hcfAnalytics(hcfId, facilityId, start, end);
    }

    @Test
    void hcfAnalyticsRejectsOversizedDateRangeBeforeServiceCall() {
        UUID hcfId = UUID.randomUUID();
        LocalDate start = LocalDate.now().minusDays(400);
        LocalDate end = LocalDate.now();

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.hcf(hcfId, start, end));

        assertEquals(400, thrown.getStatusCode().value());
        verify(analyticsService, never()).hcfAnalytics(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void dashboardMetricsRejectsOversizedDaysWindowBeforeRepositoryCall() {
        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.getDashboardMetrics(400));

        assertEquals(400, thrown.getStatusCode().value());
        verify(dailySnapshotRepository, never()).findByFacilityIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void analyticsPageRejectsInvertedDateRangeBeforeServiceCall() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.minusDays(1);

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.getPageProcessedBags(from, to, null, 0, 20));

        assertEquals(400, thrown.getStatusCode().value());
        verify(analyticsPageService, never()).getProcessedBags(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void myHcfMetricsRequiresPortalAccess() {
        UUID hcfId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
        when(dailySnapshotRepository.findByFacilityIdAndHcfIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                org.mockito.ArgumentMatchers.eq(facilityId),
                org.mockito.ArgumentMatchers.eq(hcfId),
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class))).thenReturn(List.of());

        controller.getMyHcfMetrics(30);

        verify(hcfAccessGuard).assertPortalAccess(hcfId, facilityId);
        verify(dailySnapshotRepository).findByFacilityIdAndHcfIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                org.mockito.ArgumentMatchers.eq(facilityId),
                org.mockito.ArgumentMatchers.eq(hcfId),
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class));
        verify(dailySnapshotRepository, never()).findByHcfIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                org.mockito.ArgumentMatchers.eq(hcfId),
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class));
    }

    @Test
    void hcfTrendsRequirePortalAccess() {
        UUID hcfId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
        when(dailySnapshotRepository.findByFacilityIdAndHcfIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                org.mockito.ArgumentMatchers.eq(facilityId),
                org.mockito.ArgumentMatchers.eq(hcfId),
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class))).thenReturn(List.of());

        controller.getTrends(30, "weight");

        verify(hcfAccessGuard).assertPortalAccess(hcfId, facilityId);
        verify(dailySnapshotRepository).findByFacilityIdAndHcfIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                org.mockito.ArgumentMatchers.eq(facilityId),
                org.mockito.ArgumentMatchers.eq(hcfId),
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class));
        verify(dailySnapshotRepository, never()).findByHcfIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                org.mockito.ArgumentMatchers.eq(hcfId),
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class));
    }

    private AnalyticsResponse emptyAnalyticsResponse() {
        return new AnalyticsResponse(
                BigDecimal.ZERO,
                0,
                Map.of(),
                Map.of(),
                0,
                0,
                Map.of(),
                Map.of());
    }
}
