package com.smartcbwtf.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = headerOrGenerated(request.getHeader(REQUEST_ID_HEADER));
        String traceId = headerOrGenerated(request.getHeader(TRACE_HEADER));
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(TRACE_HEADER, traceId);
        MDC.put("requestId", requestId);
        MDC.put("traceId", traceId);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
            int status = response.getStatus();
            log.info("requestId={} traceId={} method={} path={} status={} user={} durationMs={} agentRunId={} agentWorkflowId={}",
                    requestId,
                    traceId,
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    userId,
                    durationMs,
                    HttpLogSanitizer.headerForLog(request.getHeader("X-AgentAI-Run-Id")),
                    HttpLogSanitizer.headerForLog(request.getHeader("X-AgentAI-Workflow-Id")));
            MDC.remove("requestId");
            MDC.remove("traceId");
        }
    }

    private String headerOrGenerated(String value) {
        String safeValue = HttpLogSanitizer.correlationIdOrNull(value);
        return safeValue != null ? safeValue : UUID.randomUUID().toString();
    }
}
