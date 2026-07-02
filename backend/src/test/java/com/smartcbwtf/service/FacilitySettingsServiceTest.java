package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.FacilitySettings;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.BankAccountRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.FacilitySettingsRepository;
import com.smartcbwtf.repository.SettingsAuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilitySettingsServiceTest {

    @Mock
    private FacilitySettingsRepository settingsRepository;
    @Mock
    private SettingsAuditLogRepository auditLogRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private FacilityRepository facilityRepository;

    private FacilitySettingsService service;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        service = new FacilitySettingsService(
                settingsRepository,
                auditLogRepository,
                userRepository,
                bankAccountRepository,
                facilityRepository);
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void updateAgreementTermsNormalizesLineEndingsAndStoresNullForBlank() {
        FacilitySettings settings = new FacilitySettings();
        settings.setAgreementTermsTemplate("Old clause");
        when(settingsRepository.findById(facilityId)).thenReturn(Optional.of(settings));
        when(settingsRepository.save(settings)).thenReturn(settings);

        service.updateAgreementTermsTemplate("  Clause one\r\nClause\ttwo  ", "127.0.0.1");

        ArgumentCaptor<FacilitySettings> saved = ArgumentCaptor.forClass(FacilitySettings.class);
        verify(settingsRepository).save(saved.capture());
        assertEquals("Clause one\nClause two", saved.getValue().getAgreementTermsTemplate());
        verify(auditLogRepository).save(org.mockito.ArgumentMatchers.any());

        service.updateAgreementTermsTemplate("   \n\t  ", "127.0.0.1");
        assertNull(settings.getAgreementTermsTemplate());
    }

    @Test
    void updateAgreementTermsRejectsOversizedTemplate() {
        FacilitySettings settings = new FacilitySettings();
        when(settingsRepository.findById(facilityId)).thenReturn(Optional.of(settings));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateAgreementTermsTemplate("x".repeat(20_001), "127.0.0.1"));
    }
}
