package com.smartcbwtf.config;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.OAuthClient;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.OAuthClientRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterSecurityTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private OAuthClientRepository oAuthClientRepository;
    @Mock
    private ObjectProvider<OAuthClientRepository> oAuthClientRepositoryProvider;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void tenantContextUsesCurrentUserBindingsInsteadOfTokenTenantClaims() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID currentFacilityId = UUID.randomUUID();
        UUID currentHcfId = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("hcf-admin");
        when(claims.get("user_id", String.class)).thenReturn(userId.toString());
        lenient().when(claims.get("tenant_id", String.class)).thenReturn(UUID.randomUUID().toString());
        lenient().when(claims.get("hcf_id", String.class)).thenReturn(UUID.randomUUID().toString());
        when(jwtService.parseClaims("token")).thenReturn(claims);

        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("hcf-admin");
        user.setRole("HCF_ADMIN");
        user.setActive(true);
        Facility facility = new Facility();
        facility.setId(currentFacilityId);
        Hcf hcf = new Hcf();
        hcf.setId(currentHcfId);
        user.setFacility(facility);
        user.setHcf(hcf);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));

        JwtAuthFilter filter = new JwtAuthFilter(jwtService, appUserRepository, agreementRepository,
                oAuthClientRepositoryProvider, new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hcf/profile/me");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<TenantContext.TenantInfo> capturedTenant = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedTenant.set(TenantContext.get());

        filter.doFilterInternal(request, response, chain);

        assertEquals(currentFacilityId, capturedTenant.get().tenantId());
        assertEquals(currentHcfId, capturedTenant.get().hcfId());
        verifyNoInteractions(oAuthClientRepository);
    }

    @Test
    void hcfAdminTenantContextUsesActiveAgreementWhenNoDirectFacilityIsSet() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("hcf-admin");
        when(claims.get("user_id", String.class)).thenReturn(userId.toString());
        lenient().when(claims.get("tenant_id", String.class)).thenReturn(UUID.randomUUID().toString());
        when(jwtService.parseClaims("token")).thenReturn(claims);

        Facility facility = new Facility();
        facility.setId(facilityId);
        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        Agreement agreement = new Agreement();
        agreement.setFacility(facility);
        agreement.setHcf(hcf);
        agreement.setStatus(Agreement.Status.ACTIVE.name());
        agreement.setStartDate(LocalDate.now().minusDays(1));
        agreement.setEndDate(LocalDate.now().plusDays(30));

        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("hcf-admin");
        user.setRole("HCF_ADMIN");
        user.setActive(true);
        user.setHcf(hcf);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(agreementRepository.findFirstByHcfIdAndStatusOrderByStartDateDesc(hcfId, Agreement.Status.ACTIVE.name()))
                .thenReturn(Optional.of(agreement));

        JwtAuthFilter filter = new JwtAuthFilter(jwtService, appUserRepository, agreementRepository,
                oAuthClientRepositoryProvider, new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/hcf/dashboard");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<TenantContext.TenantInfo> capturedTenant = new AtomicReference<>();
        FilterChain chain = (req, res) -> capturedTenant.set(TenantContext.get());

        filter.doFilterInternal(request, response, chain);

        assertEquals(facilityId, capturedTenant.get().tenantId());
        assertEquals(hcfId, capturedTenant.get().hcfId());
    }

    @Test
    void oauthAccessTokenForDisabledClientIsRejectedBeforeController() throws Exception {
        UUID userId = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("svc");
        when(claims.get("user_id", String.class)).thenReturn(userId.toString());
        when(claims.get("token_use", String.class)).thenReturn(JwtAuthFilter.TOKEN_USE_OAUTH_ACCESS);
        when(claims.get("client_id", String.class)).thenReturn("client_a");
        when(jwtService.parseClaims("token")).thenReturn(claims);

        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("svc");
        user.setRole("CBWTF_ADMIN");
        user.setActive(true);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));

        OAuthClient disabledClient = new OAuthClient();
        disabledClient.setClientId("client_a");
        disabledClient.setActive(false);
        when(oAuthClientRepositoryProvider.getIfAvailable()).thenReturn(oAuthClientRepository);
        when(oAuthClientRepository.findById("client_a")).thenReturn(Optional.of(disabledClient));

        JwtAuthFilter filter = new JwtAuthFilter(jwtService, appUserRepository, agreementRepository,
                oAuthClientRepositoryProvider, new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cbwtf/dashboard");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Boolean> chainReached = new AtomicReference<>(false);

        filter.doFilterInternal(request, response, (req, res) -> chainReached.set(true));

        assertEquals(401, response.getStatus());
        assertEquals(false, chainReached.get());
    }

    @Test
    void activeOauthAccessTokenContinuesWithClientMetadata() throws Exception {
        UUID userId = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("svc");
        when(claims.get("user_id", String.class)).thenReturn(userId.toString());
        when(claims.get("token_use", String.class)).thenReturn(JwtAuthFilter.TOKEN_USE_OAUTH_ACCESS);
        when(claims.get("client_id", String.class)).thenReturn("client_a");
        when(claims.get("scope", String.class)).thenReturn("smartcbwtf.facility.read");
        when(jwtService.parseClaims("token")).thenReturn(claims);

        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("svc");
        user.setRole("CBWTF_ADMIN");
        user.setActive(true);
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));

        OAuthClient activeClient = new OAuthClient();
        activeClient.setClientId("client_a");
        activeClient.setActive(true);
        when(oAuthClientRepositoryProvider.getIfAvailable()).thenReturn(oAuthClientRepository);
        when(oAuthClientRepository.findById("client_a")).thenReturn(Optional.of(activeClient));

        JwtAuthFilter filter = new JwtAuthFilter(jwtService, appUserRepository, agreementRepository,
                oAuthClientRepositoryProvider, new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cbwtf/dashboard");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> capturedClientId = new AtomicReference<>();

        filter.doFilterInternal(request, response,
                (req, res) -> capturedClientId.set((String) req.getAttribute(JwtAuthFilter.ATTR_CLIENT_ID)));

        assertEquals(200, response.getStatus());
        assertEquals("client_a", capturedClientId.get());
    }
}
