package com.smartcbwtf.config;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProductionReadinessChecks implements ApplicationRunner {

    private final Environment environment;
    private final boolean emailEnabled;
    private final String brevoApiKey;

    public ProductionReadinessChecks(
            Environment environment,
            @Value("${app.email.enabled:false}") boolean emailEnabled,
            @Value("${app.email.brevo.api-key:}") String brevoApiKey) {
        this.environment = environment;
        this.emailEnabled = emailEnabled;
        this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfile()) {
            return;
        }

        if (emailEnabled && !StringUtils.hasText(brevoApiKey)) {
            throw new IllegalStateException(
                    "Production email is enabled but BREVO_API_KEY is missing. Set BREVO_API_KEY or APP_EMAIL_ENABLED=false.");
        }
        requireApiDocsHidden();
        requireSafeActuatorExposure();
        requireProductionCorsOrigins();
        requireSafePersistenceSettings();
    }

    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }

    private void requireProductionCorsOrigins() {
        List<String> origins = Binder.get(environment)
                .bind("security.cors.allowed-origins", Bindable.listOf(String.class))
                .orElse(List.of());
        List<String> normalizedOrigins = SecurityConfig.normalizeAllowedOrigins(origins);
        if (normalizedOrigins.isEmpty()) {
            throw new IllegalStateException("Production CORS origins must be explicitly configured.");
        }
        normalizedOrigins.forEach(this::requireProductionCorsOrigin);
    }

    private void requireProductionCorsOrigin(String origin) {
        URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid production CORS origin: " + origin, e);
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(scheme)) {
            throw new IllegalStateException("Production CORS origins must use HTTPS: " + origin);
        }
        if (!StringUtils.hasText(host)) {
            throw new IllegalStateException("Production CORS origin must include a host: " + origin);
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalizedHost)
                || normalizedHost.startsWith("127.")
                || "0.0.0.0".equals(normalizedHost)
                || "::1".equals(normalizedHost)
                || "[::1]".equals(normalizedHost)) {
            throw new IllegalStateException("Production CORS origins must not use localhost: " + origin);
        }
    }

    private void requireApiDocsHidden() {
        boolean exposeApiDocs = Binder.get(environment)
                .bind("app.security.expose-api-docs", Boolean.class)
                .orElse(false);
        if (exposeApiDocs) {
            throw new IllegalStateException("Production API documentation endpoints must not be publicly exposed.");
        }
    }

    private void requireSafeActuatorExposure() {
        List<String> exposedEndpoints = bindCommaSeparatedList("management.endpoints.web.exposure.include");
        if (!Set.of("health").equals(Set.copyOf(exposedEndpoints))) {
            throw new IllegalStateException(
                    "Production actuator exposure must include only health. Found: "
                            + displayConfiguredValue(String.join(",", exposedEndpoints)));
        }

        String healthDetails = environment.getProperty("management.endpoint.health.show-details", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!"never".equals(healthDetails)) {
            throw new IllegalStateException(
                    "Production actuator health details must be hidden with management.endpoint.health.show-details=never.");
        }
    }

    private List<String> bindCommaSeparatedList(String propertyName) {
        return Binder.get(environment)
                .bind(propertyName, Bindable.listOf(String.class))
                .orElse(List.of()).stream()
                .filter(StringUtils::hasText)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private void requireSafePersistenceSettings() {
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto", "").trim().toLowerCase(Locale.ROOT);
        if (!ddlAuto.equals("validate") && !ddlAuto.equals("none")) {
            throw new IllegalStateException(
                    "Production spring.jpa.hibernate.ddl-auto must be validate or none. Found: "
                            + displayConfiguredValue(ddlAuto));
        }

        Boolean flywayValidateOnMigrate = environment.getProperty("spring.flyway.validate-on-migrate", Boolean.class);
        if (!Boolean.TRUE.equals(flywayValidateOnMigrate)) {
            throw new IllegalStateException("Production Flyway validate-on-migrate must be true.");
        }

        Boolean flywayIgnoreMissingMigrations =
                environment.getProperty("spring.flyway.ignore-missing-migrations", Boolean.class);
        if (Boolean.TRUE.equals(flywayIgnoreMissingMigrations)) {
            throw new IllegalStateException("Production Flyway ignore-missing-migrations must be false.");
        }

        Boolean openInView = environment.getProperty("spring.jpa.open-in-view", Boolean.class);
        if (!Boolean.FALSE.equals(openInView)) {
            throw new IllegalStateException("Production spring.jpa.open-in-view must be false.");
        }
    }

    private String displayConfiguredValue(String value) {
        return StringUtils.hasText(value) ? value : "<missing>";
    }
}
