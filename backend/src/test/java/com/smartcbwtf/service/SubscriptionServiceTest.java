package com.smartcbwtf.service;

import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.SubscriptionAuditRepository;
import com.smartcbwtf.repository.TenantFeatureFlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private SubscriptionAuditRepository auditRepository;
    @Mock
    private TenantFeatureFlagRepository featureFlagRepository;
    @Mock
    private SystemConfigService systemConfigService;

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(
                facilityRepository,
                auditRepository,
                featureFlagRepository,
                systemConfigService);
    }

    @Test
    void grantTemporaryAccessRejectsDaysOutsideConfiguredRangeBeforeLoadingFacility() {
        when(systemConfigService.getInt("subscription.temp_access_max_days", 30)).thenReturn(14);

        assertThrows(IllegalArgumentException.class,
                () -> service.grantTemporaryAccess(UUID.randomUUID(), 0, UUID.randomUUID(), "admin", "bad"));
        assertThrows(IllegalArgumentException.class,
                () -> service.grantTemporaryAccess(UUID.randomUUID(), 15, UUID.randomUUID(), "admin", "bad"));

        verifyNoInteractions(facilityRepository, auditRepository, featureFlagRepository);
    }
}
