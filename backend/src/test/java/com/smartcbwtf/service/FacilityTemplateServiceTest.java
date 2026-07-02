package com.smartcbwtf.service;

import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.FacilityTemplate;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.FacilityTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FacilityTemplateServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadTemplateSanitizesFilenameTokensAndStoresUnderTemplateRoot() throws Exception {
        UUID facilityId = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setCode("FAC/../EAST\\001");

        FacilityRepository facilityRepository = mock(FacilityRepository.class);
        FacilityTemplateRepository templateRepository = mock(FacilityTemplateRepository.class);
        AppUserRepository userRepository = mock(AppUserRepository.class);
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(templateRepository.findByFacilityIdAndVersion(facilityId, "v1/../draft")).thenReturn(Optional.empty());
        when(templateRepository.save(any(FacilityTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FacilityTemplateService service = new FacilityTemplateService(
                templateRepository,
                facilityRepository,
                userRepository,
                tempDir);

        FacilityTemplate template = service.uploadTemplate(
                facilityId,
                "Agreement",
                "html",
                "v1/../draft",
                new MockMultipartFile("file", "template.html", "text/html", "<h1>Terms</h1>".getBytes()),
                null,
                false);

        Path stored = Path.of(template.getContentLocation());
        assertTrue(stored.startsWith(tempDir.toAbsolutePath().normalize()));
        assertTrue(Files.exists(stored));
        assertFalse(stored.getFileName().toString().contains(".."));
        assertFalse(stored.getFileName().toString().contains("/"));
        assertFalse(stored.getFileName().toString().contains("\\"));
    }

    @Test
    void uploadTemplateRejectsSpoofedPdfContent() {
        UUID facilityId = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setCode("FAC");

        FacilityRepository facilityRepository = mock(FacilityRepository.class);
        FacilityTemplateRepository templateRepository = mock(FacilityTemplateRepository.class);
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(templateRepository.findByFacilityIdAndVersion(facilityId, "v1")).thenReturn(Optional.empty());

        FacilityTemplateService service = new FacilityTemplateService(
                templateRepository,
                facilityRepository,
                mock(AppUserRepository.class),
                tempDir);

        assertThrows(IllegalArgumentException.class, () -> service.uploadTemplate(
                facilityId,
                "Agreement",
                "PDF",
                "v1",
                new MockMultipartFile("file", "template.pdf", "application/pdf", "<html>".getBytes()),
                null,
                false));
    }

    @Test
    void readTemplateContentRejectsStoredTraversalLocation() {
        FacilityTemplateService service = new FacilityTemplateService(
                mock(FacilityTemplateRepository.class),
                mock(FacilityRepository.class),
                mock(AppUserRepository.class),
                tempDir);
        FacilityTemplate template = new FacilityTemplate();
        template.setContentLocation("../secret.html");

        assertThrows(IllegalArgumentException.class, () -> service.readTemplateContent(template));
    }
}
