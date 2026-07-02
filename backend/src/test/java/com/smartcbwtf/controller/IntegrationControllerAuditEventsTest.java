package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.AuditLogRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.RouteRepository;
import com.smartcbwtf.repository.VehicleRepository;
import com.smartcbwtf.service.OAuthScopeRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationControllerAuditEventsTest {

    @Mock
    private RequestMappingHandlerMapping handlerMapping;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private BagLabelRepository bagLabelRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private IntegrationController controller;

    @BeforeEach
    void setUp() {
        controller = new IntegrationController(handlerMapping, new OAuthScopeRegistry(), appUserRepository,
                facilityRepository, hcfRepository, agreementRepository, vehicleRepository, routeRepository,
                bagLabelRepository, auditLogRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void auditEventsNormalizesEntityTypeAndKeepsSuperAdminActorFilter() {
        UUID actorId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-02T00:00:00Z");
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), null, null, "SUPER_ADMIN", "root"));
        when(auditLogRepository.search(eq("FACILITY"), eq(entityId), eq(actorId), eq(from), eq(to),
                any(Pageable.class))).thenReturn(Page.empty());

        var response = controller.auditEvents(" facility ", entityId, actorId, from, to, 0, 50);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
        verify(auditLogRepository).search(eq("FACILITY"), eq(entityId), eq(actorId), eq(from), eq(to),
                any(Pageable.class));
    }

    @Test
    void auditEventsForTenantUsersOverridesActorFilterWithCurrentUser() {
        UUID currentUserId = UUID.randomUUID();
        UUID requestedActorId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(currentUserId, UUID.randomUUID(), null, "CBWTF_ADMIN", "admin"));
        when(auditLogRepository.search(eq("HCF"), isNull(), eq(currentUserId), isNull(), isNull(),
                any(Pageable.class))).thenReturn(Page.empty());

        controller.auditEvents("hcf", null, requestedActorId, null, null, 0, 50);

        verify(auditLogRepository).search(eq("HCF"), isNull(), eq(currentUserId), isNull(), isNull(),
                any(Pageable.class));
    }

    @Test
    void auditEventsRejectsReversedDateRangeBeforeRepositoryCall() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), null, null, "SUPER_ADMIN", "root"));

        assertThrows(IllegalArgumentException.class,
                () -> controller.auditEvents(null, null, null,
                        Instant.parse("2026-07-02T00:00:00Z"),
                        Instant.parse("2026-07-01T00:00:00Z"),
                        0, 50));

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void auditEventsRejectsOversizedEntityTypeBeforeRepositoryCall() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), null, null, "SUPER_ADMIN", "root"));

        assertThrows(IllegalArgumentException.class,
                () -> controller.auditEvents("x".repeat(81), null, null, null, null, 0, 50));

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void probeUsesCountQueryForHcfAgreementCount() {
        UUID userId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, null, hcfId, "HCF_ADMIN", "hcf-user"));
        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("hcf-user");
        user.setRole("HCF_ADMIN");
        when(appUserRepository.findByUsername("hcf-user")).thenReturn(Optional.of(user));
        when(agreementRepository.countByHcfId(hcfId)).thenReturn(7L);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("app.smartcbwtf.test");
        when(request.getServerPort()).thenReturn(443);

        Map<String, Object> response = controller.probe(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> counts = (Map<String, Object>) response.get("counts");
        @SuppressWarnings("unchecked")
        Map<String, Object> connection = (Map<String, Object>) response.get("connection");

        assertEquals(7L, counts.get("hcfAgreements"));
        assertEquals("local", connection.get("environment"));
        assertEquals("https://app.smartcbwtf.test", connection.get("apiBaseUrl"));
        verify(agreementRepository).countByHcfId(hcfId);
        verify(agreementRepository, never()).findAllByHcfId(hcfId);
    }
}
