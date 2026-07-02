package com.smartcbwtf.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class SecurityConfigCorsTest {

    @Test
    void normalizeAllowedOriginsTrimsDeduplicatesAndNormalizesOriginOnlyUrls() {
        List<String> origins = SecurityConfig.normalizeAllowedOrigins(List.of(
                " https://Portal.SmartCBWTF.com ",
                "https://portal.smartcbwtf.com/",
                "http://localhost:5173"));

        assertEquals(List.of(
                "https://portal.smartcbwtf.com",
                "http://localhost:5173"), origins);
    }

    @Test
    void normalizeAllowedOriginsRejectsWildcardOrigins() {
        assertThrows(IllegalStateException.class,
                () -> SecurityConfig.normalizeAllowedOrigins(List.of("*")));
        assertThrows(IllegalStateException.class,
                () -> SecurityConfig.normalizeAllowedOrigins(List.of("https://*.smartcbwtf.com")));
    }

    @Test
    void normalizeAllowedOriginsRejectsValuesThatAreNotOrigins() {
        assertThrows(IllegalStateException.class,
                () -> SecurityConfig.normalizeAllowedOrigins(List.of("portal.smartcbwtf.com")));
        assertThrows(IllegalStateException.class,
                () -> SecurityConfig.normalizeAllowedOrigins(List.of("https://portal.smartcbwtf.com/api")));
        assertThrows(IllegalStateException.class,
                () -> SecurityConfig.normalizeAllowedOrigins(List.of("https://portal.smartcbwtf.com?debug=true")));
    }
}
