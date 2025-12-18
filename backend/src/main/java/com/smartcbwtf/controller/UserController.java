package com.smartcbwtf.controller;

import com.smartcbwtf.dto.UserProfileResponse;
import com.smartcbwtf.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User profile controller.
 * 
 * IMPORTANT: This controller provides READ-ONLY access to user profile data.
 * Profile data is centrally managed at the backend database level.
 * There are intentionally NO PUT/POST/PATCH endpoints for profile updates.
 * 
 * Any profile modifications must be done directly in the database or
 * through a future admin portal (not the mobile app).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository userRepository;

    public UserController(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get current authenticated user's profile.
     * 
     * This is the ONLY profile endpoint and returns read-only data.
     * The mobile app uses this for identity confirmation only.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        // The JwtAuthFilter sets username as the principal (String)
        String username = authentication.getPrincipal().toString();

        return userRepository.findByUsername(username)
                .map(user -> ResponseEntity.ok(UserProfileResponse.fromUser(user)))
                .orElse(ResponseEntity.notFound().build());
    }
}
