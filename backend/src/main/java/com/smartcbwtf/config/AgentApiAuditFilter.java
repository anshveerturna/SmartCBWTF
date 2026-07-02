package com.smartcbwtf.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcbwtf.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AgentApiAuditFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AgentApiAuditFilter.class);

    private final ObjectProvider<AuditLogService> auditLogServiceProvider;
    private final ObjectMapper objectMapper;

    public AgentApiAuditFilter(ObjectProvider<AuditLogService> auditLogServiceProvider, ObjectMapper objectMapper) {
        this.auditLogServiceProvider = auditLogServiceProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            auditAgentApiCall(request, response);
        }
    }

    private void auditAgentApiCall(HttpServletRequest request, HttpServletResponse response) {
        try {
            AuditLogService auditLogService = auditLogServiceProvider.getIfAvailable();
            if (auditLogService != null && shouldAudit(request)) {
                auditLogService.log("API_REQUEST", null, "AGENTAI_API_CALL", TenantContext.getUserId(),
                        objectMapper.writeValueAsString(auditPayload(request, response)));
            }
        } catch (Exception e) {
            log.warn("Agent API audit logging failed for {} {} with status {}: {}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), e.toString());
        }
    }

    private boolean shouldAudit(HttpServletRequest request) {
        if (!isMutation(request.getMethod()) || !request.getRequestURI().startsWith("/api/")) {
            return false;
        }
        return headerPresent(request, "X-AgentAI-Run-Id")
                || headerPresent(request, "X-AgentAI-Workflow-Id")
                || headerPresent(request, "Idempotency-Key")
                || JwtAuthFilter.TOKEN_USE_OAUTH_ACCESS.equals(request.getAttribute(JwtAuthFilter.ATTR_TOKEN_USE));
    }

    private Map<String, Object> auditPayload(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("method", request.getMethod());
        payload.put("path", request.getRequestURI());
        payload.put("query", HttpLogSanitizer.queryForAudit(request.getQueryString()));
        payload.put("status", response.getStatus());
        payload.put("requestId", HttpLogSanitizer.headerForLog(response.getHeader("X-Request-Id")));
        payload.put("clientId", request.getAttribute(JwtAuthFilter.ATTR_CLIENT_ID));
        payload.put("scope", request.getAttribute(JwtAuthFilter.ATTR_SCOPES));
        payload.put("idempotencyKey", HttpLogSanitizer.headerForLog(request.getHeader("Idempotency-Key")));
        payload.put("agentRunId", HttpLogSanitizer.headerForLog(request.getHeader("X-AgentAI-Run-Id")));
        payload.put("agentWorkflowId", HttpLogSanitizer.headerForLog(request.getHeader("X-AgentAI-Workflow-Id")));
        payload.put("agentNodeId", HttpLogSanitizer.headerForLog(request.getHeader("X-AgentAI-Node-Id")));
        payload.put("agentReason", HttpLogSanitizer.headerForLog(request.getHeader("X-AgentAI-Reason")));
        payload.put("tenantId", TenantContext.getTenantId());
        payload.put("hcfId", TenantContext.getHcfId());
        return payload;
    }

    private boolean headerPresent(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value != null && !value.isBlank();
    }

    private boolean isMutation(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }
}
