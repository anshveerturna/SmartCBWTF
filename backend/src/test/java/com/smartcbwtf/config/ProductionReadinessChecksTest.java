package com.smartcbwtf.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionReadinessChecksTest {

    @Test
    void prodEmailEnabledRequiresBrevoApiKey() {
        MockEnvironment environment = prodEnvironment();

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, true, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodEmailDisabledAllowsMissingBrevoApiKey() {
        MockEnvironment environment = prodEnvironment();

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertDoesNotThrow(() -> checks.run(null));
    }

    @Test
    void prodEmailEnabledAllowsConfiguredBrevoApiKey() {
        MockEnvironment environment = prodEnvironment();

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, true, "brevo-key");

        assertDoesNotThrow(() -> checks.run(null));
    }

    @Test
    void prodRequiresCorsOrigins() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        setSafePersistenceProperties(environment);

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodRejectsBlankOnlyCorsOrigins() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("security.cors.allowed-origins[0]", " ");
        setSafePersistenceProperties(environment);

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodRejectsHttpCorsOrigin() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("security.cors.allowed-origins[0]", "http://portal.smartcbwtf.com");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodRejectsLocalhostCorsOrigin() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("security.cors.allowed-origins[0]", "https://localhost:5173");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodRejectsLoopbackCorsOrigin() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("security.cors.allowed-origins[0]", "https://127.0.0.2:5173");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodRejectsWildcardCorsOrigin() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("security.cors.allowed-origins[0]", "https://*.smartcbwtf.com");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodRejectsCorsOriginWithPathQueryOrUserInfo() {
        assertProdRejectsCorsOrigin("https://portal.smartcbwtf.com/api");
        assertProdRejectsCorsOrigin("https://portal.smartcbwtf.com?debug=true");
        assertProdRejectsCorsOrigin("https://user@portal.smartcbwtf.com");
    }

    @Test
    void prodRejectsPublicApiDocsExposure() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("app.security.expose-api-docs", "true");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodRejectsMissingActuatorExposureSetting() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("management.endpoints.web.exposure.include", "");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodRejectsExtraActuatorExposure() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("management.endpoints.web.exposure.include", "health,info");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodRejectsVisibleActuatorHealthDetails() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("management.endpoint.health.show-details", "always");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void nonProdAllowsMissingBrevoApiKey() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, true, "");

        assertDoesNotThrow(() -> checks.run(null));
    }

    @Test
    void nonProdAllowsApiDocsExposure() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        environment.setProperty("app.security.expose-api-docs", "true");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertDoesNotThrow(() -> checks.run(null));
    }

    @Test
    void prodRejectsUnsafeHibernateDdlAuto() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("spring.jpa.hibernate.ddl-auto", "update");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodRejectsMissingHibernateDdlAuto() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("spring.jpa.hibernate.ddl-auto", "");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodAllowsHibernateDdlAutoNone() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("spring.jpa.hibernate.ddl-auto", "none");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertDoesNotThrow(() -> checks.run(null));
    }

    @Test
    void prodRejectsDisabledFlywayValidation() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("spring.flyway.validate-on-migrate", "false");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodRejectsIgnoredMissingFlywayMigrations() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("spring.flyway.ignore-missing-migrations", "true");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodRejectsOpenInView() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("spring.jpa.open-in-view", "true");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    @Test
    void prodRejectsMissingOpenInViewSetting() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("security.cors.allowed-origins[0]", "https://portal.smartcbwtf.com");
        environment.setProperty("spring.jpa.hibernate.ddl-auto", "validate");
        environment.setProperty("spring.flyway.validate-on-migrate", "true");
        environment.setProperty("spring.flyway.ignore-missing-migrations", "false");

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    private MockEnvironment prodEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("security.cors.allowed-origins[0]", "https://portal.smartcbwtf.com");
        setSafePersistenceProperties(environment);
        return environment;
    }

    private void assertProdRejectsCorsOrigin(String origin) {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("security.cors.allowed-origins[0]", origin);

        ProductionReadinessChecks checks = new ProductionReadinessChecks(environment, false, "");

        assertThrows(IllegalStateException.class, () -> checks.run(null));
    }

    private void setSafePersistenceProperties(MockEnvironment environment) {
        environment.setProperty("spring.jpa.hibernate.ddl-auto", "validate");
        environment.setProperty("spring.flyway.validate-on-migrate", "true");
        environment.setProperty("spring.flyway.ignore-missing-migrations", "false");
        environment.setProperty("spring.jpa.open-in-view", "false");
        environment.setProperty("management.endpoints.web.exposure.include", "health");
        environment.setProperty("management.endpoint.health.show-details", "never");
    }
}
