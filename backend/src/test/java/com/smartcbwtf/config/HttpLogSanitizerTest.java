package com.smartcbwtf.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpLogSanitizerTest {

    @Test
    void rejectsUnsafeCorrelationIds() {
        assertEquals("req-123", HttpLogSanitizer.correlationIdOrNull("req-123"));
        assertNull(HttpLogSanitizer.correlationIdOrNull("req-123\r\nInjected: true"));
        assertNull(HttpLogSanitizer.correlationIdOrNull("x".repeat(129)));
    }

    @Test
    void redactsSensitiveQueryValues() {
        String query = HttpLogSanitizer.queryForAudit(
                "client_secret=s3cr3t&search=abc&refresh_token=tok&code%5Fverifier=pkce&state=csrf");

        assertEquals(
                "client_secret=[REDACTED]&search=abc&refresh_token=[REDACTED]&code%5Fverifier=[REDACTED]&state=[REDACTED]",
                query);
    }
}
