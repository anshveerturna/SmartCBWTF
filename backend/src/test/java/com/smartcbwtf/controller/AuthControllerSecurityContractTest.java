package com.smartcbwtf.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerSecurityContractTest {

    @Test
    void unlockEndpointMustRequireSuperAdminRole() {
        Method unlockMethod = Arrays.stream(AuthController.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("unlockAccount"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("unlockAccount method not found"));

        PreAuthorize preAuthorize = unlockMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "unlockAccount must have @PreAuthorize");
        assertEquals("hasRole('SUPER_ADMIN')", preAuthorize.value());

        PostMapping postMapping = unlockMethod.getAnnotation(PostMapping.class);
        assertNotNull(postMapping, "unlockAccount must remain an explicit POST mapping");
        assertTrue(Arrays.asList(postMapping.value()).contains("/unlock/{username}"));
    }
}
