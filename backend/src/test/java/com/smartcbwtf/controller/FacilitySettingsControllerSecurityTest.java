package com.smartcbwtf.controller;

import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.FacilitySettingsRepository;
import com.smartcbwtf.service.AgreementNumberGeneratorService;
import com.smartcbwtf.service.FacilitySettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilitySettingsControllerSecurityTest {

    @Mock
    private FacilitySettingsService settingsService;
    @Mock
    private AgreementNumberGeneratorService agreementNumberGenerator;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private FacilitySettingsRepository facilitySettingsRepository;

    private FacilitySettingsController controller;

    @BeforeEach
    void setUp() {
        controller = new FacilitySettingsController(
                settingsService,
                agreementNumberGenerator,
                facilityRepository,
                facilitySettingsRepository);
    }

    @Test
    void settingsAuditHistoryIsNotCacheable() {
        when(settingsService.getAuditHistory("legal", 0, 20)).thenReturn(Page.empty());

        var response = controller.getAuditHistory("legal", 0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }
}
