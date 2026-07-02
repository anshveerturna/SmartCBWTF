package com.smartcbwtf.config;

import com.smartcbwtf.service.SystemConfigService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceSecurityTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void constructorRejectsWeakSecret() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService("short-secret", "smart-cbwtf", 30, new StubSystemConfigService()));
    }

    @Test
    void constructorRejectsLocalDevDefaultSecret() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService(
                        "smartcbwtf-local-dev-signing-key-2025-not-for-prod",
                        "smart-cbwtf",
                        30,
                        new StubSystemConfigService()));
    }

    @Test
    void parseClaimsRejectsWrongIssuer() {
        JwtService jwtService = new JwtService(SECRET, "expected-issuer", 30, new StubSystemConfigService());
        String token = Jwts.builder()
                .setClaims(Map.of("role", "DRIVER", "user_id", "5ef6cf5f-2b76-4aa6-9f6f-108f5ba4f2ec"))
                .setSubject("driver01")
                .setIssuer("wrong-issuer")
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertThrows(Exception.class, () -> jwtService.parseClaims(token));
    }

    @Test
    void parseClaimsAcceptsExpectedIssuer() {
        JwtService jwtService = new JwtService(SECRET, "expected-issuer", 30, new StubSystemConfigService());
        String token = jwtService.generateToken("driver01",
                Map.of("role", "DRIVER", "user_id", "5ef6cf5f-2b76-4aa6-9f6f-108f5ba4f2ec"));

        Claims claims = jwtService.parseClaims(token);
        assertEquals("driver01", claims.getSubject());
        assertEquals("expected-issuer", claims.getIssuer());
    }

    @Test
    void explicitTokenTtlBypassesSessionTimeout() {
        JwtService jwtService = new JwtService(SECRET, "expected-issuer", 30, new StubSystemConfigService(120));
        Instant before = Instant.now();

        String token = jwtService.generateToken("oauth-client",
                Map.of("role", "CBWTF_ADMIN", "user_id", "5ef6cf5f-2b76-4aa6-9f6f-108f5ba4f2ec"),
                5);

        Instant expiration = jwtService.parseClaims(token).getExpiration().toInstant();
        assertTrue(expiration.isAfter(before.plusSeconds(240)));
        assertTrue(expiration.isBefore(before.plusSeconds(360)));
    }

    private static class StubSystemConfigService extends SystemConfigService {
        private final int value;

        StubSystemConfigService() {
            this(0);
        }

        StubSystemConfigService(int value) {
            super(null, null, null);
            this.value = value;
        }

        @Override
        public int getInt(String key, int defaultValue) {
            return value == 0 ? defaultValue : value;
        }
    }
}
