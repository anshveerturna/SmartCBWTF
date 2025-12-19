package com.smartcbwtf.controller;

import com.smartcbwtf.config.JwtService;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.dto.AuthLoginRequest;
import com.smartcbwtf.dto.AuthLoginResponse;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
            AppUserRepository appUserRepository, AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        AppUser user = appUserRepository.findByUsername(authentication.getName()).orElseThrow();

        // Build claims with user_id, tenant_id (facility_id) and hcf_id for
        // multi-tenant support
        java.util.HashMap<String, Object> claims = new java.util.HashMap<>();
        claims.put("user_id", user.getId().toString());
        claims.put("role", user.getRole());
        claims.put("tenant_id", user.getFacility() != null ? user.getFacility().getId().toString() : null);
        claims.put("hcf_id", user.getHcf() != null ? user.getHcf().getId().toString() : null);

        String token = jwtService.generateToken(user.getUsername(), claims);
        auditLogService.log("APP_USER", user.getId(), "LOGIN", user.getId(), null);
        return ResponseEntity.ok(new AuthLoginResponse(token));
    }
}
