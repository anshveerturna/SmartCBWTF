package com.smartcbwtf.service;

import com.smartcbwtf.domain.FacilityNotificationSettings;
import com.smartcbwtf.repository.FacilityNotificationSettingsRepository;
import com.smartcbwtf.repository.FacilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSettingsServiceTest {

    @Mock
    private FacilityNotificationSettingsRepository settingsRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private AuditLogService auditLogService;

    private NotificationSettingsService service;

    @BeforeEach
    void setUp() {
        service = new NotificationSettingsService(settingsRepository, facilityRepository, auditLogService);
    }

    @Test
    void updateSettingsRejectsOutOfRangeValuesBeforeLoadingFacility() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateSettings(UUID.randomUUID(),
                        new NotificationSettingsService.UpdateRequest(0, null, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateSettings(UUID.randomUUID(),
                        new NotificationSettingsService.UpdateRequest(null, 15, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateSettings(UUID.randomUUID(),
                        new NotificationSettingsService.UpdateRequest(null, null, 11, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateSettings(UUID.randomUUID(),
                        new NotificationSettingsService.UpdateRequest(null, null, null, 6)));

        verifyNoInteractions(settingsRepository, facilityRepository, auditLogService);
    }

    @Test
    void updateSettingsPersistsBoundedValuesAndAuditsChange() {
        UUID facilityId = UUID.randomUUID();
        FacilityNotificationSettings settings = new FacilityNotificationSettings();
        settings.setPaymentReminderStartDays(7);
        settings.setPaymentReminderFrequencyDays(3);
        settings.setMaxOverdueReminders(5);
        settings.setAgreementExpiryWarningDays(30);
        when(settingsRepository.findById(facilityId)).thenReturn(Optional.of(settings));
        when(settingsRepository.save(settings)).thenReturn(settings);

        FacilityNotificationSettings updated = service.updateSettings(facilityId,
                new NotificationSettingsService.UpdateRequest(10, 5, 8, 45));

        assertEquals(10, updated.getPaymentReminderStartDays());
        assertEquals(5, updated.getPaymentReminderFrequencyDays());
        assertEquals(8, updated.getMaxOverdueReminders());
        assertEquals(45, updated.getAgreementExpiryWarningDays());
        verify(auditLogService).log(eq("FacilityNotificationSettings"), eq(facilityId), eq("SETTINGS_UPDATED"),
                eq(null), anyString());
        verify(settingsRepository).save(settings);
    }
}
