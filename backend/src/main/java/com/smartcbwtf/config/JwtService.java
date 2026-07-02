package com.smartcbwtf.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.smartcbwtf.service.SystemConfigService;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import java.util.Locale;
import java.util.Map;

@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final Set<String> BLOCKED_SECRET_FRAGMENTS = Set.of(
            "change-me",
            "change-this",
            "changethis",
            "local-dev",
            "not-for-prod",
            "production-signing-key",
            "smartcbwtf2026productionsigningkey");
    private final Key key;
    private final String issuer;
    private final long defaultAccessTokenTtlMinutes;
    private final SystemConfigService systemConfigService;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes,
            SystemConfigService systemConfigService) {
        assertStrongSigningSecret("security.jwt.secret", secret);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.defaultAccessTokenTtlMinutes = accessTokenTtlMinutes;
        this.systemConfigService = systemConfigService;
        log.info("JWT service initialized with issuer {}", issuer);
    }

    public static void assertStrongSigningSecret(String propertyName, String secret) {
        String normalizedSecret = secret == null ? "" : secret.toLowerCase(Locale.ROOT);
        if (secret == null || secret.isBlank() || secret.length() < 32
                || BLOCKED_SECRET_FRAGMENTS.stream().anyMatch(normalizedSecret::contains)) {
            throw new IllegalStateException(
                    propertyName + " must be set to a strong 32+ character value via environment");
        }
    }

    public String generateToken(String subject, Map<String, Object> claims) {
        // Get session timeout from system config, fall back to default
        long sessionTimeoutMinutes = systemConfigService.getInt(
                "security.session_timeout_minutes",
                (int) defaultAccessTokenTtlMinutes);
        return generateToken(subject, claims, sessionTimeoutMinutes);
    }

    public String generateToken(String subject, Map<String, Object> claims, long ttlMinutes) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttlMinutes, ChronoUnit.MINUTES)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
