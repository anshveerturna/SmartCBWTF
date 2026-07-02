package com.smartcbwtf.controller;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.AgreementVerificationDTO;
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

class PublicAgreementControllerSecurityTest {

    private final AgreementRepository agreementRepository = mock(AgreementRepository.class);
    private final PublicAgreementController controller = new PublicAgreementController(agreementRepository);

    @Test
    void publicVerificationOmitsSensitiveContactAddressAndBillingFields() {
        UUID agreementId = UUID.randomUUID();
        Agreement agreement = activeAgreement();
        when(agreementRepository.findById(agreementId)).thenReturn(Optional.of(agreement));

        ResponseEntity<AgreementVerificationDTO> response = controller.verifyAgreement(agreementId);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        AgreementVerificationDTO body = response.getBody();
        assertEquals("ACTIVE", body.getStatus());
        assertTrue(body.isValid());
        assertEquals("City Hospital", body.getHcfName());
        assertEquals("HCF-001", body.getHcfCode());
        assertEquals("AGR-2026-001", body.getAgreementNumber());
        assertEquals("Green CBWTF", body.getFacilityName());

        assertNull(body.getHcfAddress());
        assertNull(body.getHcfState());
        assertNull(body.getHcfPincode());
        assertNull(body.getHcfCategory());
        assertNull(body.getHcfEmail());
        assertNull(body.getHcfDoctorName());
        assertNull(body.getHcfContactNumber());
        assertNull(body.getNumberOfBeds());
        assertNull(body.getFacilityAddress());
        assertNull(body.getFacilityContact());
        assertNull(body.getBillingModel());
    }

    @Test
    void publicVerificationReportsExpiredActiveAgreementAsInvalidForUse() {
        UUID agreementId = UUID.randomUUID();
        Agreement agreement = activeAgreement();
        agreement.setEndDate(LocalDate.now().minusDays(1));
        when(agreementRepository.findById(agreementId)).thenReturn(Optional.of(agreement));

        ResponseEntity<AgreementVerificationDTO> response = controller.verifyAgreement(agreementId);

        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("EXPIRED", response.getBody().getStatus());
        assertEquals(false, response.getBody().isValid());
    }

    @Test
    void publicVerificationHandlesIncompleteAgreementRowsWithoutPublicServerError() {
        UUID agreementId = UUID.randomUUID();
        Agreement agreement = activeAgreement();
        agreement.setHcf(null);
        when(agreementRepository.findById(agreementId)).thenReturn(Optional.of(agreement));

        ResponseEntity<AgreementVerificationDTO> response = controller.verifyAgreement(agreementId);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("INVALID", response.getBody().getStatus());
        assertEquals(false, response.getBody().isValid());
        assertNull(response.getBody().getHcfName());
    }

    @Test
    void publicVerificationNotFoundResponseIsNotCached() {
        UUID agreementId = UUID.randomUUID();
        when(agreementRepository.findById(agreementId)).thenReturn(Optional.empty());

        ResponseEntity<AgreementVerificationDTO> response = controller.verifyAgreement(agreementId);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
    }

    private Agreement activeAgreement() {
        Hcf hcf = new Hcf();
        hcf.setName("City Hospital");
        hcf.setCode("HCF-001");
        hcf.setAddress("1 Private Road");
        hcf.setState("Uttarakhand");
        hcf.setPincode("263153");
        hcf.setContactEmail("doctor@example.com");
        hcf.setContactPhone("+91 99999 00000");
        hcf.setDoctorName("Dr. Private");
        hcf.setNumberOfBeds(42);

        Facility facility = new Facility();
        facility.setName("Green CBWTF");
        facility.setCode("CBWTF-001");
        facility.setAddress("Facility Address");
        facility.setContactEmail("ops@example.com");
        facility.setContactPhone("+91 88888 00000");

        Agreement agreement = new Agreement();
        agreement.setAgreementNumber("AGR-2026-001");
        agreement.setStatus(Agreement.Status.ACTIVE.name());
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setStartDate(LocalDate.of(2026, 1, 1));
        agreement.setEndDate(LocalDate.of(2026, 12, 31));
        return agreement;
    }
}
