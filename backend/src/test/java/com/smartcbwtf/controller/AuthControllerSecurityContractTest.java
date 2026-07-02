package com.smartcbwtf.controller;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.dto.AuthLoginRequest;
import com.smartcbwtf.dto.AuthLoginResponse;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Test
    void failedLoginForKnownUserDoesNotExposeAttemptsRemaining() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        SystemConfigService systemConfigService = mock(SystemConfigService.class);
        AuthController controller = new AuthController(
                authenticationManager,
                mock(com.smartcbwtf.config.JwtService.class),
                userRepository,
                mock(com.smartcbwtf.service.AuditLogService.class),
                systemConfigService,
                mock(com.smartcbwtf.service.HcfAccessGuard.class),
                mock(com.smartcbwtf.service.EmailService.class));

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setUsername("known-user");
        user.setRole("CBWTF_ADMIN");
        when(systemConfigService.getInt("security.max_login_attempts", 5)).thenReturn(5);
        when(systemConfigService.getBoolean("security.force_password_reset_first_login", false)).thenReturn(false);
        when(userRepository.findByUsername("known-user")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("known-user");
        request.setPassword("wrong-password");

        ResponseEntity<?> response = controller.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map<?, ?>);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("INVALID_CREDENTIALS", body.get("error"));
        assertEquals("Invalid username or password.", body.get("message"));
        assertFalse(body.toString().contains("attempts remaining"));
        assertEquals(1, user.getFailedLoginAttempts());
        verify(userRepository).save(user);
    }

    @Test
    void successfulLoginTokenResponseUsesNoStoreNoCacheHeaders() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        SystemConfigService systemConfigService = mock(SystemConfigService.class);
        com.smartcbwtf.config.JwtService jwtService = mock(com.smartcbwtf.config.JwtService.class);
        AuthController controller = new AuthController(
                authenticationManager,
                jwtService,
                userRepository,
                mock(com.smartcbwtf.service.AuditLogService.class),
                systemConfigService,
                mock(com.smartcbwtf.service.HcfAccessGuard.class),
                mock(com.smartcbwtf.service.EmailService.class));

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setUsername("admin");
        user.setRole("CBWTF_ADMIN");
        user.setActive(true);
        when(systemConfigService.getInt("security.max_login_attempts", 5)).thenReturn(5);
        when(systemConfigService.getBoolean("security.force_password_reset_first_login", false)).thenReturn(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("admin", null));
        when(jwtService.generateToken(eq("admin"), any())).thenReturn("session-token");

        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("admin");
        request.setPassword("correct-password");

        ResponseEntity<?> response = controller.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst("Pragma"));
        assertTrue(response.getBody() instanceof AuthLoginResponse);
        assertEquals("session-token", ((AuthLoginResponse) response.getBody()).getAccessToken());
    }
}
