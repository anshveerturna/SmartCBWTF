package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.service.AgreementService;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.service.PdfService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcfAgreementControllerTest {

    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private HcfAccessGuard accessGuard;
    @Mock
    private AgreementService agreementService;
    @Mock
    private PdfService pdfService;

    private HcfAgreementController controller;
    private UUID hcfId;
    private UUID facilityId;
    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        controller = new HcfAgreementController(agreementRepository, accessGuard, agreementService, pdfService);
        hcfId = UUID.randomUUID();
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getAgreementUsesTenantScopedActiveAgreementLookup() {
        Agreement agreement = activeAgreement(hcfId, facilityId);
        when(agreementRepository.findActiveByHcfAndFacility(hcfId, facilityId)).thenReturn(Optional.of(agreement));

        var response = controller.getAgreement();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(agreement.getId(), response.getBody().id());
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
        verify(agreementRepository, never()).findFirstByHcfIdAndStatusOrderByStartDateDesc(
                hcfId, Agreement.Status.ACTIVE.name());
        verify(agreementRepository, never()).findByHcfIdAndStatus(hcfId, Agreement.Status.ACTIVE.name());
    }

    @Test
    void downloadPdfUsesTenantScopedActiveAgreementLookup() {
        when(agreementRepository.findActiveByHcfAndFacility(hcfId, facilityId)).thenReturn(Optional.empty());

        var response = controller.downloadPdf();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
        verify(agreementRepository, never()).findFirstByHcfIdAndStatusOrderByStartDateDesc(
                hcfId, Agreement.Status.ACTIVE.name());
    }

    @Test
    void downloadPdfUsesNoStoreCacheHeader() throws Exception {
        Agreement agreement = activeAgreement(hcfId, facilityId);
        agreement.setPdfUrl("/files/agreements/AGR-001.pdf");
        Path pdf = tempDir.resolve("AGR-001.pdf");
        Files.write(pdf, new byte[] { 1, 2, 3 });
        when(agreementRepository.findActiveByHcfAndFacility(hcfId, facilityId)).thenReturn(Optional.of(agreement));
        when(agreementService.regeneratePdf(agreement)).thenReturn(agreement);
        when(pdfService.storedGeneratedFilePath(agreement.getPdfUrl())).thenReturn(pdf);

        var response = controller.downloadPdf();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("no-store", response.getHeaders().getCacheControl());
    }

    private static Agreement activeAgreement(UUID hcfId, UUID facilityId) {
        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        hcf.setCode("HCF-001");
        hcf.setName("City Clinic");

        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setCode("CBWTF-001");
        facility.setName("Facility");

        Agreement agreement = new Agreement();
        agreement.setId(UUID.randomUUID());
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setStatus(Agreement.Status.ACTIVE.name());
        agreement.setAgreementNumber("AGR-001");
        agreement.setStartDate(LocalDate.now().minusDays(1));
        agreement.setEndDate(LocalDate.now().plusDays(30));
        return agreement;
    }
}
