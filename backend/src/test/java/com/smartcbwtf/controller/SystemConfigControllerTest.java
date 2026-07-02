package com.smartcbwtf.controller;

import com.smartcbwtf.domain.SystemConfigAudit;
import com.smartcbwtf.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemConfigControllerTest {

    @Test
    void getAuditHistoryMasksSensitiveLegacyValues() {
        SystemConfigService configService = mock(SystemConfigService.class);
        SystemConfigAudit audit = new SystemConfigAudit();
        audit.setConfigKey("security.jwt.secret");
        audit.setOldValue("old-secret-value");
        audit.setNewValue("new-secret-value");
        audit.setChangedAt(Instant.parse("2026-07-01T00:00:00Z"));

        when(configService.getAuditHistory("security.jwt.secret", 0, 50)).thenReturn(List.of(audit));
        when(configService.isSensitiveConfigKey("security.jwt.secret")).thenReturn(true);

        SystemConfigController controller = new SystemConfigController(configService);

        var response = controller.getAuditHistory("security.jwt.secret", 0, 50);

        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertEquals(SystemConfigService.MASKED_VALUE, response.getBody().get(0).oldValue());
        assertEquals(SystemConfigService.MASKED_VALUE, response.getBody().get(0).newValue());
    }

    @Test
    void recentChangesAreNotCacheable() {
        SystemConfigService configService = mock(SystemConfigService.class);
        when(configService.getRecentChanges()).thenReturn(List.of());

        SystemConfigController controller = new SystemConfigController(configService);

        var response = controller.getRecentChanges();

        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }
}
