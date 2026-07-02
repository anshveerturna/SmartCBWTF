package com.smartcbwtf.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcbwtf.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentApiAuditFilterTest {

    @Mock
    private ObjectProvider<AuditLogService> auditLogServiceProvider;

    @Mock
    private AuditLogService auditLogService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void auditFailureDoesNotBreakSuccessfulApiMutation() throws Exception {
        UUID userId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, UUID.randomUUID(), null, "CBWTF_ADMIN", "admin"));
        when(auditLogServiceProvider.getIfAvailable()).thenReturn(auditLogService);
        doThrow(new IllegalStateException("audit store unavailable"))
                .when(auditLogService)
                .log(eq("API_REQUEST"), any(), eq("AGENTAI_API_CALL"), eq(userId), any());

        AgentApiAuditFilter filter = new AgentApiAuditFilter(auditLogServiceProvider, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/cbwtf/payments");
        request.addHeader("Idempotency-Key", "payment-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(request, response, okJsonChain(chainCalls));

        assertEquals(1, chainCalls.get());
        assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
        assertEquals("{\"ok\":true}", response.getContentAsString());
        verify(auditLogService).log(eq("API_REQUEST"), any(), eq("AGENTAI_API_CALL"), eq(userId), any());
    }

    private FilterChain okJsonChain(AtomicInteger chainCalls) {
        return (request, response) -> {
            chainCalls.incrementAndGet();
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_CREATED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"ok\":true}");
        };
    }
}
