package com.smartcbwtf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcbwtf.controller.HcfController.MobileHcfDto;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.FacilitySettingsRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcfServiceTest {

    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private AgreementNumberGeneratorService agreementNumberGenerator;
    @Mock
    private FacilitySettingsRepository facilitySettingsRepository;
    @Mock
    private FacilityTermsService facilityTermsService;
    @Mock
    private FacilityTemplateService facilityTemplateService;
    @Mock
    private PdfService pdfService;
    @Mock
    private EmailService emailService;

    private HcfService service;

    @BeforeEach
    void setUp() {
        service = new HcfService(
                hcfRepository,
                agreementRepository,
                facilityRepository,
                userRepository,
                auditLogService,
                agreementNumberGenerator,
                facilitySettingsRepository,
                facilityTermsService,
                facilityTemplateService,
                pdfService,
                emailService,
                new ObjectMapper(),
                25);
    }

    @Test
    void listActiveHcfsForMobileUsesDatabaseFilteredActiveHcfs() {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        hcf.setName("City Clinic");
        hcf.setAddress("Main Road");
        hcf.setCity("Gurgaon");
        hcf.setState("Haryana");
        hcf.setPincode("122001");
        hcf.setContactPhone("9999999999");
        hcf.setGpsLat(28.4595);
        hcf.setGpsLon(77.0266);
        hcf.setStatus("ACTIVE");
        when(agreementRepository.findMobileActiveHcfsByFacilityId(facilityId)).thenReturn(List.of(hcf));

        List<MobileHcfDto> result = service.listActiveHcfsForMobile(facilityId);

        assertEquals(1, result.size());
        assertEquals(hcfId.toString(), result.get(0).id());
        assertEquals("Gurgaon", result.get(0).city());
        assertEquals(28.4595, result.get(0).latitude());
        assertEquals(77.0266, result.get(0).longitude());
        verify(agreementRepository).findMobileActiveHcfsByFacilityId(facilityId);
        verify(agreementRepository, never()).findHcfsByFacilityId(facilityId);
    }

    @Test
    void registerRejectsMissingFacilityContextBeforeRepositoryAccess() {
        com.smartcbwtf.dto.HcfRegistrationRequest request = new com.smartcbwtf.dto.HcfRegistrationRequest();

        assertThrows(IllegalArgumentException.class, () -> service.register(request));

        verifyNoInteractions(hcfRepository, facilityRepository, userRepository);
    }

    @Test
    void registerRejectsUnsafeRentAgreementUrlBeforePersistence() {
        com.smartcbwtf.dto.HcfRegistrationRequest request = new com.smartcbwtf.dto.HcfRegistrationRequest();
        request.setFacilityId(UUID.randomUUID());
        request.setOwnershipType("RENTED");
        request.setRentAgreementUrl("/uploads/rent-agreements/../secret.pdf");
        request.setBedded(false);
        request.setTermsAccepted(true);
        request.setRegistrationGpsLat(28.4595);
        request.setRegistrationGpsLon(77.0266);
        request.setRegistrationGpsAccuracy(20.0);

        assertThrows(IllegalArgumentException.class, () -> service.register(request));

        verifyNoInteractions(hcfRepository, facilityRepository, userRepository);
    }

    @Test
    void registerRejectsRentAgreementUrlOwnedByDifferentFacility() {
        UUID facilityId = UUID.randomUUID();
        com.smartcbwtf.dto.HcfRegistrationRequest request = new com.smartcbwtf.dto.HcfRegistrationRequest();
        request.setFacilityId(facilityId);
        request.setOwnershipType("RENTED");
        request.setRentAgreementUrl("/uploads/rent-agreements/" + UUID.randomUUID() + "_abc12345.pdf");
        request.setBedded(false);
        request.setTermsAccepted(true);
        request.setRegistrationGpsLat(28.4595);
        request.setRegistrationGpsLon(77.0266);
        request.setRegistrationGpsAccuracy(20.0);

        assertThrows(IllegalArgumentException.class, () -> service.register(request));

        verifyNoInteractions(hcfRepository, facilityRepository, userRepository);
    }
}
