package com.smartcbwtf.controller;

import com.smartcbwtf.domain.OAuthClient;
import com.smartcbwtf.repository.OAuthClientRepository;
import com.smartcbwtf.service.OAuthService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthClientManagementControllerTest {

    @Mock
    private OAuthService oauthService;
    @Mock
    private OAuthClientRepository clientRepository;

    @Test
    void listUsesBoundedPageableQuery() {
        OAuthClient client = new OAuthClient();
        client.setClientId("client-1");
        OAuthService.ClientView view = new OAuthService.ClientView(
                "client-1", "Client", "", "reports:read", "client_credentials",
                true, true, null, null, null, Instant.now(), Instant.now());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(clientRepository.findAll(pageable.capture())).thenReturn(new PageImpl<>(List.of(client)));
        when(oauthService.toClientView(client)).thenReturn(view);

        List<OAuthService.ClientView> response = new OAuthClientManagementController(oauthService, clientRepository)
                .list(5000);

        assertEquals(1, response.size());
        assertEquals(250, pageable.getValue().getPageSize());
        verify(oauthService).toClientView(client);
    }

    @Test
    void disableDelegatesToServiceSoRefreshTokensAreRevoked() {
        OAuthService.ClientDisableResult result = new OAuthService.ClientDisableResult("client-1", false, 3);
        when(oauthService.disableClient("client-1")).thenReturn(result);

        OAuthService.ClientDisableResult response = new OAuthClientManagementController(oauthService, clientRepository)
                .disable("client-1");

        assertEquals(result, response);
        verify(oauthService).disableClient("client-1");
    }

    @Test
    void createRequestValidationBoundsClientMetadataButAllowsClientCredentialsWithoutRedirect() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        var clientCredentialsOnly = new OAuthClientManagementController.CreateOAuthClientRequest(
                null,
                "Operations Client",
                UUID.randomUUID(),
                null,
                "smartcbwtf.facility.read",
                "client_credentials");

        assertTrue(validator.validate(clientCredentialsOnly).isEmpty());

        var unsafe = new OAuthClientManagementController.CreateOAuthClientRequest(
                "bad client id",
                "",
                UUID.randomUUID(),
                "x".repeat(4001),
                "x".repeat(4001),
                "x".repeat(201));

        var violations = validator.validate(unsafe);

        assertTrue(violations.stream().anyMatch(v -> "clientId".contentEquals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "name".contentEquals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "redirectUris".contentEquals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "scopes".contentEquals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "grantTypes".contentEquals(v.getPropertyPath().toString())));
    }
}
