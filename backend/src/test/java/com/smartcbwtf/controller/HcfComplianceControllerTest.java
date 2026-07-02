package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.DuesClearanceRequestRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.service.PdfService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
class HcfComplianceControllerTest {

    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private BagEventRepository bagEventRepository;
    @Mock
    private BagLabelRepository bagLabelRepository;
    @Mock
    private DuesClearanceRequestRepository duesRequestRepository;
    @Mock
    private HcfAccessGuard accessGuard;
    @Mock
    private PdfService pdfService;

    private HcfComplianceController controller;
    private UUID hcfId;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        controller = new HcfComplianceController(hcfRepository, agreementRepository, bagEventRepository,
                bagLabelRepository, duesRequestRepository, accessGuard, pdfService);
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
    void dailyDataHandlesIncompleteBagEvents() {
        LocalDate date = LocalDate.of(2026, 7, 1);
        BagEvent unknown = event(null, null, null);
        unknown.setEventTs(null);
        BagEvent yellow = event("YELLOW", "SER-1", new BigDecimal("3.250"));
        when(bagEventRepository.sumWeightGroupedByCategoryForFacilityAndHcfBetweenIncludingUnknown(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        new Object[] { "UNKNOWN", BigDecimal.ZERO },
                        new Object[] { "YELLOW", new BigDecimal("3.250") }));
        when(bagEventRepository.sumWeightByFacilityIdAndHcfIdAndEventTsBetween(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("3.250"));
        when(bagEventRepository.countByFacilityIdAndHcfIdAndEventTsBetween(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(2L);
        when(bagEventRepository.findByFacilityIdAndHcfIdAndEventTsBetweenOrderByEventTsDesc(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(yellow, unknown));
        when(bagLabelRepository.countByFacilityIdAndHcfIdAndIssuedAtBetween(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(2L);

        ResponseEntity<Map<String, Object>> response = controller.getDailyData(date, 200);

        Map<String, Object> body = response.getBody();
        assertEquals(new BigDecimal("3.250"), body.get("totalWeight"));
        assertEquals(2L, body.get("pickupCount"));
        assertEquals(200, body.get("pickupLimit"));
        Map<?, ?> categoryWeights = (Map<?, ?>) body.get("categoryWeights");
        assertEquals(BigDecimal.ZERO, categoryWeights.get("UNKNOWN"));
        assertEquals(new BigDecimal("3.250"), categoryWeights.get("YELLOW"));
        List<?> pickups = (List<?>) body.get("pickups");
        assertEquals("SER-1", ((Map<?, ?>) pickups.get(0)).get("bagSerial"));
        assertTrue(((Map<?, ?>) pickups.get(0)).containsKey("bagSerial"));
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
        verify(bagEventRepository, never()).sumWeightGroupedByCategoryForHcfBetweenIncludingUnknown(
                eq(hcfId), any(Instant.class), any(Instant.class));
        verify(bagEventRepository, never()).sumWeightByHcfIdAndEventTsBetween(
                eq(hcfId), any(Instant.class), any(Instant.class));
    }

    @Test
    void dailyDataCapsPickupTimelineLimit() {
        LocalDate date = LocalDate.of(2026, 7, 1);
        when(bagEventRepository.sumWeightGroupedByCategoryForFacilityAndHcfBetweenIncludingUnknown(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(bagEventRepository.sumWeightByFacilityIdAndHcfIdAndEventTsBetween(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(BigDecimal.ZERO);
        when(bagEventRepository.countByFacilityIdAndHcfIdAndEventTsBetween(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(1200L);
        when(bagEventRepository.findByFacilityIdAndHcfIdAndEventTsBetweenOrderByEventTsDesc(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.getDailyData(date, 5000);

        assertEquals(500, response.getBody().get("pickupLimit"));
        verify(bagEventRepository, times(1)).findByFacilityIdAndHcfIdAndEventTsBetweenOrderByEventTsDesc(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class),
                org.mockito.ArgumentMatchers.argThat(pageable ->
                        pageable.getPageSize() == 500));
        verify(bagEventRepository, never()).findByHcfIdAndEventTsBetweenOrderByEventTsDesc(
                eq(hcfId), any(Instant.class), any(Instant.class), any(Pageable.class));
    }

    @Test
    void requestDuesClearanceRejectsMissingPeriodBeforeWriting() {
        ResponseEntity<?> response = controller.requestDuesClearance(null);

        assertEquals(400, response.getStatusCode().value());
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
        verify(duesRequestRepository, never()).save(any());
    }

    private static BagEvent event(String category, String serialNo, BigDecimal weight) {
        BagEvent event = new BagEvent();
        event.setId(UUID.randomUUID());
        event.setEventTs(Instant.parse("2026-07-01T08:15:00Z"));
        event.setWeightKg(weight);
        if (category != null) {
            BagLabel label = new BagLabel();
            label.setCategory(category);
            label.setSerialNo(serialNo);
            event.setBagLabel(label);
        }
        return event;
    }
}
