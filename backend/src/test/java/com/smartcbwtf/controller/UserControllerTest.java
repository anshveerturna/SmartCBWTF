package com.smartcbwtf.controller;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.dto.UserProfileResponse;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.service.PasswordPolicyValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUserProfileUsesNoStoreNoCacheHeaders() {
        AppUserRepository userRepository = mock(AppUserRepository.class);
        UserController controller = new UserController(
                userRepository,
                mock(PasswordEncoder.class),
                mock(PasswordPolicyValidator.class));
        UUID userId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setName("Facility One");
        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("driver1");
        user.setFullName("Driver One");
        user.setEmail("driver@example.com");
        user.setRole("DRIVER");
        user.setPasswordHash("hash");
        user.setFacility(facility);
        when(userRepository.findByUsername("driver1")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("driver1", null));

        var response = controller.getCurrentUser();

        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst("Pragma"));
        UserProfileResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(userId, body.getId());
        assertEquals("driver1", body.getUsername());
        assertEquals(facilityId, body.getFacilityId());
    }
}
