package com.smartcbwtf.service;

import com.smartcbwtf.domain.SystemConfigAudit;
import com.smartcbwtf.domain.SystemConfig;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.SystemConfigAuditRepository;
import com.smartcbwtf.repository.SystemConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SystemConfigRepository configRepository;
    @Mock
    private SystemConfigAuditRepository auditRepository;
    @Mock
    private AppUserRepository userRepository;

    @Test
    void getAuditHistoryUsesBoundedPageable() {
        SystemConfigService service = new SystemConfigService(configRepository, auditRepository, userRepository);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(auditRepository.findByConfigKeyOrderByChangedAtDesc(eq("security.session_timeout_minutes"),
                pageable.capture())).thenReturn(new PageImpl<>(List.of(new SystemConfigAudit())));

        List<SystemConfigAudit> audits = service.getAuditHistory("security.session_timeout_minutes", -10, 5000);

        assertEquals(1, audits.size());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
    }

    @Test
    void updateConfigMasksSensitiveValuesInAuditRecord() {
        SystemConfigService service = new SystemConfigService(configRepository, auditRepository, userRepository);
        SystemConfig config = new SystemConfig();
        config.setConfigKey("security.jwt.secret");
        config.setConfigValue("old-secret-value");
        config.setValueTypeEnum(SystemConfig.ValueType.STRING);
        config.setSensitive(true);

        when(configRepository.findByConfigKey("security.jwt.secret")).thenReturn(java.util.Optional.of(config));
        when(configRepository.save(config)).thenReturn(config);
        ArgumentCaptor<SystemConfigAudit> auditCaptor = ArgumentCaptor.forClass(SystemConfigAudit.class);

        service.updateConfig("security.jwt.secret", "new-secret-value", null, "rotate", "203.0.113.10");

        verify(auditRepository).save(auditCaptor.capture());
        assertEquals(SystemConfigService.MASKED_VALUE, auditCaptor.getValue().getOldValue());
        assertEquals(SystemConfigService.MASKED_VALUE, auditCaptor.getValue().getNewValue());
    }

    @Test
    void isSensitiveConfigKeyFallsBackToKeyNameWhenConfigMetadataIsMissing() {
        SystemConfigService service = new SystemConfigService(configRepository, auditRepository, userRepository);
        when(configRepository.findByConfigKey("security.jwt.secret")).thenReturn(Optional.empty());

        assertTrue(service.isSensitiveConfigKey("security.jwt.secret"));
    }
}
