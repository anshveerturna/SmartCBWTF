package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.FacilitySettings;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.FacilitySettingsRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfServiceTest {

    @Test
    void generatedFilePathResolvesOnlyUnderFilesRoot() {
        PdfService service = new PdfService(null);

        Path path = service.generatedFilePath("/files/agreements/facility/agreement.pdf");

        assertTrue(path.startsWith(Path.of("files").toAbsolutePath().normalize()));
        assertEquals("agreement.pdf", path.getFileName().toString());
    }

    @Test
    void generatedFilePathRejectsWrongPrefixesAndTraversal() {
        PdfService service = new PdfService(null);

        assertThrows(IllegalArgumentException.class, () -> service.generatedFilePath("/uploads/logo.png"));
        assertThrows(IllegalArgumentException.class, () -> service.generatedFilePath("/files/../secret.pdf"));
        assertThrows(IllegalArgumentException.class, () -> service.generatedFilePath("/files/facility/../../secret.pdf"));
        assertThrows(IllegalArgumentException.class, () -> service.generatedFilePath("/files/facility\\secret.pdf"));
    }

    @Test
    void storedGeneratedFilePathSupportsLegacyRelativeAndAbsolutePathsUnderFilesRoot() {
        PdfService service = new PdfService(null);
        Path root = Path.of("files").toAbsolutePath().normalize();

        assertTrue(service.storedGeneratedFilePath("files/agreements/facility/agreement.pdf").startsWith(root));
        assertTrue(service.storedGeneratedFilePath("agreements/facility/agreement.pdf").startsWith(root));
        assertTrue(service.storedGeneratedFilePath(root.resolve("agreements/facility/agreement.pdf").toString())
                .startsWith(root));
    }

    @Test
    void storedGeneratedFilePathRejectsPathsOutsideFilesRoot() {
        PdfService service = new PdfService(null);

        assertThrows(IllegalArgumentException.class, () -> service.storedGeneratedFilePath("../secret.pdf"));
        assertThrows(IllegalArgumentException.class, () -> service.storedGeneratedFilePath("files/../secret.pdf"));
        assertThrows(IllegalArgumentException.class, () -> service.storedGeneratedFilePath("/tmp/secret.pdf"));
        assertThrows(IllegalArgumentException.class, () -> service.storedGeneratedFilePath("agreements\\secret.pdf"));
    }

    @Test
    void safeFileTokenRemovesPathSeparatorsAndDotDot() {
        String token = PdfService.safeFileToken(" FAC/../evil\\label.pdf ");

        assertFalse(token.contains(".."));
        assertFalse(token.contains("/"));
        assertFalse(token.contains("\\"));
        assertEquals("unknown", PdfService.safeFileToken("../"));
        assertEquals(PdfService.safeFileToken("A/B"), PdfService.safeDownloadToken("A/B"));
    }

    @Test
    void monthlyCompliancePdfUsesFacilityAuthorizationNumberFromSettings() throws Exception {
        UUID facilityId = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setCode("CBWTF-001");
        facility.setName("Smart CBWTF");
        facility.setAddress("Facility address");
        facility.setContactPhone("+91 99999 99999");

        Hcf hcf = new Hcf();
        hcf.setId(UUID.randomUUID());
        hcf.setCode("HCF-001");
        hcf.setName("City Hospital");
        hcf.setAddress("Hospital address");
        hcf.setNumberOfBeds(20);
        hcf.setBedded(true);

        Agreement agreement = new Agreement();
        agreement.setAgreementNumber("AGR-2026-001");
        agreement.setFacility(facility);
        agreement.setHcf(hcf);
        agreement.setStartDate(LocalDate.of(2026, 1, 1));
        agreement.setPerBedPerDayRate(new BigDecimal("10.00"));

        FacilitySettings settings = new FacilitySettings();
        settings.setAuthorizationNumber("AUTH-REAL-123");
        settings.setLegalName("Smart CBWTF Legal");
        settings.setRegisteredAddress("Registered CBWTF address");
        settings.setOfficialPhone("+91 88888 88888");

        FacilitySettingsRepository settingsRepository = mock(FacilitySettingsRepository.class);
        when(settingsRepository.findById(facilityId)).thenReturn(Optional.of(settings));

        PdfService service = new PdfService(settingsRepository);
        byte[] pdf = service.generateMonthlyCompliancePdf(agreement, LocalDate.of(2026, 1, 1), List.of());

        String text;
        try (PDDocument document = PDDocument.load(pdf)) {
            text = new PDFTextStripper().getText(document);
        }

        assertTrue(text.contains("Auth No: AUTH-REAL-123"));
        assertFalse(text.contains("CBWTF-AUTH-202X-001"));
    }
}
