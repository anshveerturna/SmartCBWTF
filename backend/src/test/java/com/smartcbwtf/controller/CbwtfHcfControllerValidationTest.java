package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.HcfDetailDTO;
import com.smartcbwtf.dto.HcfRejectionRequest;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.service.AgreementService;
import com.smartcbwtf.service.BillingConfigService;
import com.smartcbwtf.service.CbwtfHcfService;
import com.smartcbwtf.service.PdfService;
import com.smartcbwtf.service.UploadFileValidator;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CbwtfHcfControllerValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    @TempDir
    private Path tempDir;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void rejectHcfValidatesRejectionRequestBody() throws NoSuchMethodException {
        Method method = CbwtfHcfController.class.getDeclaredMethod("rejectHcf", UUID.class,
                HcfRejectionRequest.class);
        Parameter parameter = method.getParameters()[1];

        assertTrue(parameter.isAnnotationPresent(Valid.class));
        assertTrue(parameter.isAnnotationPresent(RequestBody.class));
    }

    @Test
    void hcfRejectionRequestRequiresBoundedReason() {
        HcfRejectionRequest blank = new HcfRejectionRequest();
        blank.setReason("   ");
        assertFieldViolation(blank, "reason");

        HcfRejectionRequest oversized = new HcfRejectionRequest();
        oversized.setReason("x".repeat(501));
        assertFieldViolation(oversized, "reason");
    }

    @Test
    void agreementPdfDownloadsUseNoStoreCacheHeader() throws Exception {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        Agreement agreement = activeAgreement(hcfId, facilityId);
        agreement.setPdfUrl("/files/agreements/AGR-001.pdf");
        Path pdf = tempDir.resolve("agreement.pdf");
        Path printPdf = tempDir.resolve("agreement-print.pdf");
        Files.write(pdf, new byte[] { 1, 2, 3 });
        Files.write(printPdf, new byte[] { 4, 5, 6 });
        AgreementRepository agreementRepository = mock(AgreementRepository.class);
        AgreementService agreementService = mock(AgreementService.class);
        PdfService pdfService = mock(PdfService.class);
        CbwtfHcfController controller = new CbwtfHcfController(
                mock(CbwtfHcfService.class),
                mock(BillingConfigService.class),
                agreementRepository,
                agreementService,
                pdfService);
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(agreementRepository.findActiveOrUpcomingByHcfAndFacility(hcfId, facilityId))
                .thenReturn(Optional.of(agreement));
        when(agreementService.regeneratePdf(agreement)).thenReturn(agreement);
        when(agreementService.generatePrintPdf(agreement)).thenReturn("/files/agreements/AGR-001-print.pdf");
        when(pdfService.storedGeneratedFilePath(agreement.getPdfUrl())).thenReturn(pdf);
        when(pdfService.storedGeneratedFilePath("/files/agreements/AGR-001-print.pdf")).thenReturn(printPdf);

        var normalResponse = controller.downloadAgreementPdf(hcfId);
        var printResponse = controller.downloadAgreementPrintPdf(hcfId);

        assertEquals("no-store", normalResponse.getHeaders().getCacheControl());
        assertEquals("no-store", printResponse.getHeaders().getCacheControl());
    }

    @Test
    void rentAgreementDownloadsUseNoStoreCacheHeader() throws Exception {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        String publicUrl = "/uploads/rent-agreements/" + facilityId + "_" + UUID.randomUUID() + ".pdf";
        Path agreementPath = UploadFileValidator.uploadedAssetPath(publicUrl, "/uploads/rent-agreements/");
        CbwtfHcfService hcfService = mock(CbwtfHcfService.class);
        HcfDetailDTO detail = new HcfDetailDTO();
        detail.setRentAgreementUrl(publicUrl);
        CbwtfHcfController controller = new CbwtfHcfController(
                hcfService,
                mock(BillingConfigService.class),
                mock(AgreementRepository.class),
                mock(AgreementService.class),
                mock(PdfService.class));
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(hcfService.getHcfDetail(hcfId, facilityId)).thenReturn(detail);

        try {
            Files.createDirectories(agreementPath.getParent());
            Files.write(agreementPath, "%PDF-1.4".getBytes(StandardCharsets.US_ASCII));

            var response = controller.downloadRentAgreement(hcfId);

            assertEquals("no-store", response.getHeaders().getCacheControl());
        } finally {
            Files.deleteIfExists(agreementPath);
        }
    }

    @Test
    void portalCredentialResponsesUseNoStoreNoCacheHeaders() {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        CbwtfHcfService hcfService = mock(CbwtfHcfService.class);
        CbwtfHcfController controller = new CbwtfHcfController(
                hcfService,
                mock(BillingConfigService.class),
                mock(AgreementRepository.class),
                mock(AgreementService.class),
                mock(PdfService.class));
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        CbwtfHcfController.ResetPasswordRequest resetRequest = new CbwtfHcfController.ResetPasswordRequest();
        resetRequest.newPassword = "Str0ng@123";
        when(hcfService.createPortalAdmin(hcfId, facilityId))
                .thenReturn(java.util.Map.of("tempPassword", "Tmp@123456", "success", true));
        when(hcfService.resetPortalAdminPassword(hcfId, facilityId, "Str0ng@123"))
                .thenReturn(java.util.Map.of("success", true));
        when(hcfService.enablePortalAccessForSmallHcf(hcfId, facilityId))
                .thenReturn(java.util.Map.of("tempPassword", "Tmp@654321", "success", true));

        var createResponse = controller.createPortalAdmin(hcfId);
        var resetResponse = controller.resetPortalAdminPassword(hcfId, resetRequest);
        var enableResponse = controller.enablePortalAccess(hcfId);

        assertPrivateCredentialHeaders(createResponse);
        assertPrivateCredentialHeaders(resetResponse);
        assertPrivateCredentialHeaders(enableResponse);
    }

    @Test
    void portalAdminResetPasswordRequestRequiresBoundedPassword() {
        CbwtfHcfController.ResetPasswordRequest blank = new CbwtfHcfController.ResetPasswordRequest();
        blank.newPassword = "   ";
        assertFieldViolation(blank, "newPassword");

        CbwtfHcfController.ResetPasswordRequest oversized = new CbwtfHcfController.ResetPasswordRequest();
        oversized.newPassword = "x".repeat(129);
        assertFieldViolation(oversized, "newPassword");
    }

    private void assertFieldViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field);
    }

    private static void assertPrivateCredentialHeaders(org.springframework.http.ResponseEntity<?> response) {
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
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
        agreement.setPerBedPerDayRate(BigDecimal.TEN);
        return agreement;
    }
}
