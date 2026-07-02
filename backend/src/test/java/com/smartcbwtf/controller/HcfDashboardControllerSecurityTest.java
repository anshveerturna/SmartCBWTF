package com.smartcbwtf.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class HcfDashboardControllerSecurityTest {

    @Test
    void dashboardMustNotExposeSeedEndpoint() {
        boolean exposesSeedEndpoint = Arrays.stream(HcfDashboardController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(PostMapping.class))
                .filter(java.util.Objects::nonNull)
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .anyMatch("/seed"::equals);

        assertFalse(exposesSeedEndpoint, "HCF dashboard must not expose production data seeding endpoints");
    }
}
