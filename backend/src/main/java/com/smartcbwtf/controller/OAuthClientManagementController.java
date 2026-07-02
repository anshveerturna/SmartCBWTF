package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.repository.OAuthClientRepository;
import com.smartcbwtf.service.OAuthService;
import com.smartcbwtf.util.PaginationUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/oauth/clients")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class OAuthClientManagementController {
    private static final int DEFAULT_CLIENT_LIST_LIMIT = 100;
    private static final int MAX_CLIENT_LIST_LIMIT = 250;

    private final OAuthService oauthService;
    private final OAuthClientRepository clientRepository;

    public OAuthClientManagementController(OAuthService oauthService, OAuthClientRepository clientRepository) {
        this.oauthService = oauthService;
        this.clientRepository = clientRepository;
    }

    @GetMapping
    public List<OAuthService.ClientView> list(
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        int safeLimit = PaginationUtils.normalizeSize(limit, DEFAULT_CLIENT_LIST_LIMIT, MAX_CLIENT_LIST_LIMIT);
        return clientRepository.findAll(PageRequest.of(0, safeLimit))
                .stream()
                .map(oauthService::toClientView)
                .toList();
    }

    @PostMapping
    public OAuthService.CreatedClient create(@Valid @RequestBody CreateOAuthClientRequest request) {
        return oauthService.createClient(
                new OAuthService.CreateClientCommand(
                        request.clientId(),
                        request.name(),
                        request.serviceAccountUserId(),
                        request.redirectUris(),
                        request.scopes(),
                        request.grantTypes()),
                TenantContext.getUserId());
    }

    @PatchMapping("/{clientId}/disable")
    public OAuthService.ClientDisableResult disable(@PathVariable String clientId) {
        return oauthService.disableClient(clientId);
    }

    public record CreateOAuthClientRequest(
            @Size(max = 80, message = "clientId must be 80 characters or less")
            @Pattern(regexp = "^$|[A-Za-z0-9._-]+", message = "clientId contains invalid characters")
            String clientId,
            @NotBlank
            @Size(max = 120, message = "name must be 120 characters or less")
            String name,
            @NotNull UUID serviceAccountUserId,
            @Size(max = 4000, message = "redirectUris must be 4000 characters or less")
            String redirectUris,
            @Size(max = 4000, message = "scopes must be 4000 characters or less")
            String scopes,
            @Size(max = 200, message = "grantTypes must be 200 characters or less")
            String grantTypes) {
    }
}
