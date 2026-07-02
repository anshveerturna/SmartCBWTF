package com.smartcbwtf.config;

import com.smartcbwtf.service.SubscriptionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionGuardFilterTest {

    @Mock
    private SubscriptionService subscriptionService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void publicEndpointsBypassSubscriptionCheck() throws Exception {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), UUID.randomUUID(), null,
                "CBWTF_ADMIN", "admin"));
        SubscriptionGuardFilter filter = new SubscriptionGuardFilter(subscriptionService, false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/public/contact");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(request, response, countingChain(chainCalls));

        assertEquals(1, chainCalls.get());
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void stalePaymentPrefixDoesNotBypassSubscriptionCheck() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), tenantId, null, "CBWTF_ADMIN", "admin"));
        SubscriptionGuardFilter filter = new SubscriptionGuardFilter(subscriptionService, false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payment/reactivate");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainCalls = new AtomicInteger();
        when(subscriptionService.isActive(tenantId)).thenReturn(false);

        filter.doFilter(request, response, countingChain(chainCalls));

        assertEquals(0, chainCalls.get());
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains("SUBSCRIPTION_INACTIVE"));
    }

    @Test
    void apiDocsRespectSharedExposureFlag() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), tenantId, null, "CBWTF_ADMIN", "admin"));
        SubscriptionGuardFilter filter = new SubscriptionGuardFilter(subscriptionService, true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(request, response, countingChain(chainCalls));

        assertEquals(1, chainCalls.get());
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        verifyNoInteractions(subscriptionService);
    }

    private FilterChain countingChain(AtomicInteger chainCalls) {
        return (request, response) -> {
            chainCalls.incrementAndGet();
            ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_OK);
        };
    }
}
