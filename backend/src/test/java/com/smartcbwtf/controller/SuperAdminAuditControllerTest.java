package com.smartcbwtf.controller;

import com.smartcbwtf.repository.SubscriptionAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminAuditControllerTest {

    @Mock
    private SubscriptionAuditRepository auditRepository;

    private SuperAdminAuditController controller;

    @BeforeEach
    void setUp() {
        controller = new SuperAdminAuditController(auditRepository);
    }

    @Test
    void getAuditLogsCombinesNormalizedFiltersAndDateRange() {
        UUID actorId = UUID.randomUUID();
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-02T00:00:00Z");
        when(auditRepository.searchAuditLogs(eq("USER"), eq("LOGIN_FAILURE"), eq(actorId), eq(from), eq(to),
                any(Pageable.class))).thenReturn(Page.empty());

        ResponseEntity<Page<SuperAdminAuditController.AuditLogDTO>> response = controller.getAuditLogs(
                " user ", " login_failure ", actorId, from, to, 0, 50);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
        verify(auditRepository).searchAuditLogs(eq("USER"), eq("LOGIN_FAILURE"), eq(actorId), eq(from), eq(to),
                any(Pageable.class));
    }

    @Test
    void recentAuditLogsAreNotCacheable() {
        when(auditRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        ResponseEntity<?> response = controller.getRecentAuditLogs();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    @Test
    void superAdminActionAuditLogsAreNotCacheable() {
        when(auditRepository.findByPerformedByRoleOrderByCreatedAtDesc(eq("SUPER_ADMIN"), any(Pageable.class)))
                .thenReturn(Page.empty());

        ResponseEntity<Page<SuperAdminAuditController.AuditLogDTO>> response = controller.getSuperAdminActions(0, 50);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    @Test
    void getAuditLogsRejectsReversedDateRangeBeforeRepositoryCall() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.getAuditLogs(null, null, null,
                        Instant.parse("2026-07-02T00:00:00Z"),
                        Instant.parse("2026-07-01T00:00:00Z"),
                        0, 50));

        verifyNoInteractions(auditRepository);
    }

    @Test
    void getAuditLogsRejectsOversizedFilterBeforeRepositoryCall() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.getAuditLogs("x".repeat(81), null, null, null, null, 0, 50));

        verifyNoInteractions(auditRepository);
    }
}
