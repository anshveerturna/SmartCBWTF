package com.smartcbwtf.config;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RequestLoggingFilter requestLoggingFilter;
    private final OAuthScopeFilter oAuthScopeFilter;
    private final IdempotencyFilter idempotencyFilter;
    private final AgentApiAuditFilter agentApiAuditFilter;
    private final UserDetailsService userDetailsService;
    private final List<String> allowedOrigins;
    private final boolean exposeApiDocs;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RequestLoggingFilter requestLoggingFilter,
            OAuthScopeFilter oAuthScopeFilter, IdempotencyFilter idempotencyFilter,
            AgentApiAuditFilter agentApiAuditFilter, Environment environment,
            UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.requestLoggingFilter = requestLoggingFilter;
        this.oAuthScopeFilter = oAuthScopeFilter;
        this.idempotencyFilter = idempotencyFilter;
        this.agentApiAuditFilter = agentApiAuditFilter;
        this.userDetailsService = userDetailsService;
        this.allowedOrigins = normalizeAllowedOrigins(Binder.get(environment)
                .bind("security.cors.allowed-origins", Bindable.listOf(String.class))
                .orElse(List.of()));
        this.exposeApiDocs = Binder.get(environment)
                .bind("app.security.expose-api-docs", Boolean.class)
                .orElse(false);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
                        .referrerPolicy(referrer -> referrer
                                .policy(
                                        org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(PublicEndpoints.permitAllPatterns(exposeApiDocs)).permitAll();
                    auth.anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(oAuthScopeFilter, JwtAuthFilter.class)
                .addFilterAfter(idempotencyFilter, OAuthScopeFilter.class)
                .addFilterAfter(agentApiAuditFilter, IdempotencyFilter.class)
                .addFilterBefore(requestLoggingFilter, JwtAuthFilter.class);
        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    static List<String> normalizeAllowedOrigins(List<String> configuredOrigins) {
        if (configuredOrigins == null) {
            return List.of();
        }
        return configuredOrigins.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .map(SecurityConfig::normalizeAllowedOrigin)
                .distinct()
                .toList();
    }

    private static String normalizeAllowedOrigin(String origin) {
        if (origin.contains("*")) {
            throw new IllegalStateException(
                    "CORS allowed origins must be explicit; wildcard origins are not allowed with credentials");
        }

        URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid CORS allowed origin: " + origin, e);
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalStateException("CORS allowed origin must use http or https: " + origin);
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalStateException("CORS allowed origin must include a host: " + origin);
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalStateException("CORS allowed origin must not include user info, query, or fragment: "
                    + origin);
        }

        String path = uri.getRawPath();
        if (path != null && !path.isBlank() && !"/".equals(path)) {
            throw new IllegalStateException("CORS allowed origin must not include a path: " + origin);
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.contains(":") && !host.startsWith("[")) {
            host = "[" + host + "]";
        }

        StringBuilder normalized = new StringBuilder()
                .append(scheme.toLowerCase(Locale.ROOT))
                .append("://")
                .append(host);
        if (uri.getPort() >= 0) {
            normalized.append(":").append(uri.getPort());
        }
        return normalized.toString();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
