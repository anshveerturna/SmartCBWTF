package com.smartcbwtf.controller;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgreementVerificationControllerSecurityTest {

    private final AgreementRepository agreementRepository = mock(AgreementRepository.class);
    private final AgreementVerificationController controller = new AgreementVerificationController(agreementRepository);

    @Test
    void legacyQrVerificationReportsExpiredActiveAgreementAsExpired() {
        UUID agreementId = UUID.randomUUID();
        Agreement agreement = activeAgreement(agreementId);
        agreement.setEndDate(LocalDate.now().minusDays(1));
        when(agreementRepository.findById(agreementId)).thenReturn(Optional.of(agreement));

        ResponseEntity<AgreementVerificationController.AgreementVerificationDTO> response =
                controller.verifyAgreement(agreementId);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("EXPIRED", response.getBody().status());
        assertEquals("This agreement is verified but has expired.", response.getBody().message());
    }

    @Test
    void legacyQrVerificationHandlesIncompleteAgreementRowsWithoutPublicServerError() {
        UUID agreementId = UUID.randomUUID();
        Agreement agreement = activeAgreement(agreementId);
        agreement.setFacility(null);
        when(agreementRepository.findById(agreementId)).thenReturn(Optional.of(agreement));

        ResponseEntity<AgreementVerificationController.AgreementVerificationDTO> response =
                controller.verifyAgreement(agreementId);

        assertEquals("INVALID", response.getBody().status());
        assertEquals("This agreement record is incomplete and cannot be verified as active.",
                response.getBody().message());
    }

    @Test
    void legacyQrVerificationNotFoundResponseIsNotCached() {
        UUID agreementId = UUID.randomUUID();
        when(agreementRepository.findById(agreementId)).thenReturn(Optional.empty());

        ResponseEntity<AgreementVerificationController.AgreementVerificationDTO> response =
                controller.verifyAgreement(agreementId);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
        assertNull(response.getBody());
    }

    private Agreement activeAgreement(UUID agreementId) {
        Hcf hcf = new Hcf();
        hcf.setName("City Hospital");
        hcf.setCode("HCF-001");

        Facility facility = new Facility();
        facility.setName("Green CBWTF");
        facility.setCode("CBWTF-001");

        Agreement agreement = new Agreement();
        agreement.setId(agreementId);
        agreement.setAgreementNumber("AGR-2026-001");
        agreement.setStatus(Agreement.Status.ACTIVE.name());
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setStartDate(LocalDate.now().minusMonths(1));
        agreement.setEndDate(LocalDate.now().plusMonths(1));
        return agreement;
    }
}
