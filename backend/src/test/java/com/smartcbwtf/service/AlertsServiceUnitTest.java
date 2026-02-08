package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.repository.BagEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        when(bagEventRepository.findMissingBags(eq(tenantId), any())).thenReturn(List.of());

        assertTrue(alertsService.listMissingBags().isEmpty());
        verify(bagEventRepository).findMissingBags(eq(tenantId), any());
    }
}
