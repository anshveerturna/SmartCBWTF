package com.smartcbwtf.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.smartcbwtf.service.SystemConfigService;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Component
public class JwtService {

    private final Key key;
    private final String issuer;
    private final long defaultAccessTokenTtlMinutes;
    private final SystemConfigService systemConfigService;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes,
            SystemConfigService systemConfigService) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.issuer = issuer;
        this.defaultAccessTokenTtlMinutes = accessTokenTtlMinutes;
        this.systemConfigService = systemConfigService;
    }

    public String generateToken(String subject, Map<String, Object> claims) {
        // Get session timeout from system config, fall back to default
        long sessionTimeoutMinutes = systemConfigService.getInt(
                "security.session_timeout_minutes",
                (int) defaultAccessTokenTtlMinutes);

        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(sessionTimeoutMinutes, ChronoUnit.MINUTES)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
