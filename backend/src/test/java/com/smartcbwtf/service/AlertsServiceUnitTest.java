package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.AlertMissingBagDto;
import com.smartcbwtf.dto.AlertMismatchedBagDto;
import com.smartcbwtf.repository.BagEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertsServiceUnitTest {

    @Mock
    private BagEventRepository bagEventRepository;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listMissingBagsReturnsEmptyWhenTenantMissing() {
        AlertsService alertsService = new AlertsService(bagEventRepository, 24);

        TenantContext.clear();
        assertTrue(alertsService.listMissingBags().isEmpty());
        verifyNoInteractions(bagEventRepository);
    }

    @Test
    void listMissingBagsScopesRepositoryCallToTenant() {
        AlertsService alertsService = new AlertsService(bagEventRepository, 24);
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), tenantId, null, "CBWTF_ADMIN", "test"));

        when(bagEventRepository.findMissingBags(eq(tenantId), any(), any(Pageable.class))).thenReturn(List.of());

        assertTrue(alertsService.listMissingBags().isEmpty());
        verify(bagEventRepository).findMissingBags(eq(tenantId), any(), any(Pageable.class));
    }

    @Test
    void listMissingBagsToleratesLegacyRowsWithMissingLabelAndHcf() {
        AlertsService alertsService = new AlertsService(bagEventRepository, 24);
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), tenantId, null, "CBWTF_ADMIN", "test"));

        BagEvent event = new BagEvent();
        event.setEventTs(Instant.parse("2026-01-02T03:04:05Z"));
        event.setWeightKg(null);
        when(bagEventRepository.findMissingBags(eq(tenantId), any(), any(Pageable.class))).thenReturn(List.of(event));

        List<AlertMissingBagDto> result = alertsService.listMissingBags();

        assertEquals(1, result.size());
        AlertMissingBagDto dto = result.get(0);
        assertEquals(null, dto.getBagLabelId());
        assertEquals("UNKNOWN_QR", dto.getQrCode());
        assertEquals("UNKNOWN", dto.getCategory());
        assertEquals("Unknown HCF", dto.getHcfName());
        assertEquals(BigDecimal.ZERO, dto.getWeightKg());
    }

    @Test
    void listMismatchedBagsToleratesMissingLabelWithoutCollectionLookup() {
        AlertsService alertsService = new AlertsService(bagEventRepository, 24);
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), tenantId, null, "CBWTF_ADMIN", "test"));

        BagEvent verification = new BagEvent();
        verification.setEventTs(Instant.parse("2026-01-02T03:04:05Z"));
        when(bagEventRepository.findByFacilityIdAndEventTypeAndAnomalyStateOrderByEventTsDesc(
                eq(tenantId), eq("CBWTF_VERIFICATION"), eq("MISMATCH"), any(Pageable.class)))
                .thenReturn(List.of(verification));

        List<AlertMismatchedBagDto> result = alertsService.listMismatchedBags();

        assertEquals(1, result.size());
        AlertMismatchedBagDto dto = result.get(0);
        assertEquals(null, dto.getBagLabelId());
        assertEquals("UNKNOWN_QR", dto.getQrCode());
        assertEquals("UNKNOWN", dto.getCategory());
        assertEquals("Unknown HCF", dto.getHcfName());
        assertEquals(BigDecimal.ZERO, dto.getHcfWeightKg());
        assertEquals(BigDecimal.ZERO, dto.getCbtwfWeightKg());
        assertEquals(BigDecimal.ZERO, dto.getDeltaKg());
        verify(bagEventRepository, never()).findFirstByBagLabelIdAndEventTypeOrderByEventTsDesc(any(), any());
    }

    @Test
    void listMismatchedBagsDefaultsNullCollectionWeightToZero() {
        AlertsService alertsService = new AlertsService(bagEventRepository, 24);
        UUID tenantId = UUID.randomUUID();
        UUID labelId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), tenantId, null, "CBWTF_ADMIN", "test"));

        BagLabel label = new BagLabel();
        label.setId(labelId);
        label.setQrCode("QR-1");
        label.setCategory("YELLOW");
        Hcf hcf = new Hcf();
        hcf.setName("City Hospital");
        BagEvent verification = new BagEvent();
        verification.setBagLabel(label);
        verification.setHcf(hcf);
        verification.setWeightKg(new BigDecimal("3.250"));
        verification.setEventTs(Instant.parse("2026-01-02T03:04:05Z"));
        BagEvent collection = new BagEvent();

        when(bagEventRepository.findByFacilityIdAndEventTypeAndAnomalyStateOrderByEventTsDesc(
                eq(tenantId), eq("CBWTF_VERIFICATION"), eq("MISMATCH"), any(Pageable.class)))
                .thenReturn(List.of(verification));
        when(bagEventRepository.findFirstByBagLabelIdAndEventTypeOrderByEventTsDesc(labelId, "HCF_COLLECTION"))
                .thenReturn(Optional.of(collection));

        List<AlertMismatchedBagDto> result = alertsService.listMismatchedBags();

        assertEquals(1, result.size());
        AlertMismatchedBagDto dto = result.get(0);
        assertEquals(labelId, dto.getBagLabelId());
        assertEquals("QR-1", dto.getQrCode());
        assertEquals("YELLOW", dto.getCategory());
        assertEquals("City Hospital", dto.getHcfName());
        assertEquals(BigDecimal.ZERO, dto.getHcfWeightKg());
        assertEquals(new BigDecimal("3.250"), dto.getCbtwfWeightKg());
        assertEquals(new BigDecimal("3.250"), dto.getDeltaKg());
    }
}
