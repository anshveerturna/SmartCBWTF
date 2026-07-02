package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.HcfRegistrationRequest;
import com.smartcbwtf.dto.HcfRegistrationResponse;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.service.HcfService;
import com.smartcbwtf.service.UploadFileValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcfControllerSecurityTest {

    @Mock
    private HcfService hcfService;

    @Mock
    private AgreementRepository agreementRepository;

    private HcfController controller;

    @BeforeEach
    void setUp() {
        controller = new HcfController(hcfService, agreementRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void registerBindsTenantAndUserFromAuthenticatedContext() {
        UUID facilityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, facilityId, null, "DRIVER", "driver"));

        HcfRegistrationRequest request = new HcfRegistrationRequest();
        HcfRegistrationResponse serviceResponse = new HcfRegistrationResponse(
                "PENDING_APPROVAL",
                UUID.randomUUID(),
                "HCF-123");
        when(hcfService.register(request)).thenReturn(serviceResponse);

        ResponseEntity<HcfRegistrationResponse> response = controller.register(request);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(facilityId, request.getFacilityId());
        assertEquals(userId, request.getRegisteredByUserId());
        verify(hcfService).register(request);
    }

    @Test
    void registerRejectsClientSuppliedFacilityMismatch() {
        UUID facilityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, facilityId, null, "DRIVER", "driver"));

        HcfRegistrationRequest request = new HcfRegistrationRequest();
        request.setFacilityId(UUID.randomUUID());

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.register(request));

        assertEquals(HttpStatus.FORBIDDEN, thrown.getStatusCode());
        verifyNoInteractions(hcfService);
    }

    @Test
    void getByIdMasksCrossTenantHcfAsNotFound() {
        UUID facilityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, facilityId, null, "CBWTF_ADMIN", "admin"));
        when(agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)).thenReturn(List.of());

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.getById(hcfId));

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
        verify(hcfService, never()).findById(hcfId);
    }

    @Test
    void pendingUsesBoundedTenantQuery() {
        UUID facilityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, facilityId, null, "CBWTF_ADMIN", "admin"));
        Agreement agreement = pendingAgreement(facilityId);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(agreementRepository.findLatestPendingHcfAgreementsByFacilityId(eq(facilityId), pageable.capture()))
                .thenReturn(List.of(agreement));

        List<?> response = controller.pending(5000);

        assertEquals(1, response.size());
        assertEquals(250, pageable.getValue().getPageSize());
        verify(agreementRepository, never()).findLatestAgreementsByFacilityId(facilityId);
    }

    @Test
    void rentAgreementUploadStoresFacilityOwnedUrl() throws Exception {
        UUID facilityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, facilityId, null, "DRIVER", "driver"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "agreement.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes());
        String url = null;

        try {
            ResponseEntity<Map<String, String>> response = controller.uploadRentAgreement(file);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            url = response.getBody().get("url");
            assertTrue(url.startsWith("/uploads/rent-agreements/" + facilityId + "_"));
        } finally {
            if (url != null) {
                Files.deleteIfExists(UploadFileValidator.uploadedAssetPath(
                        url,
                        UploadFileValidator.RENT_AGREEMENT_PUBLIC_PREFIX));
            }
        }
    }

    private Agreement pendingAgreement(UUID facilityId) {
        Facility facility = new Facility();
        facility.setId(facilityId);

        Hcf hcf = new Hcf();
        hcf.setId(UUID.randomUUID());
        hcf.setName("Hospital");
        hcf.setStatus("PENDING_APPROVAL");

        Agreement agreement = new Agreement();
        agreement.setId(UUID.randomUUID());
        agreement.setFacility(facility);
        agreement.setHcf(hcf);
        agreement.setStatus(Agreement.Status.PENDING_APPROVAL.name());
        return agreement;
    }
}
