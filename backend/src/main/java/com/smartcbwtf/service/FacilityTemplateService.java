package com.smartcbwtf.service;

import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.FacilityTemplate;
import com.smartcbwtf.dto.TemplateListItem;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.FacilityTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing facility-specific agreement templates.
 */
@Service
public class FacilityTemplateService {

    private final FacilityTemplateRepository templateRepository;
    private final FacilityRepository facilityRepository;
    private final AppUserRepository userRepository;
    private final Path templatesDir;

    public FacilityTemplateService(
            FacilityTemplateRepository templateRepository,
            FacilityRepository facilityRepository,
            AppUserRepository userRepository) {
        this.templateRepository = templateRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
        this.templatesDir = Paths.get("files", "templates");
        
        try {
            Files.createDirectories(templatesDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create templates directory", e);
        }
    }

    /**
     * Get the active template for a facility.
     * Falls back to any active template if facility-specific not found.
     */
    public Optional<FacilityTemplate> getActiveTemplate(UUID facilityId) {
        return templateRepository.findByFacilityIdAndActiveTrue(facilityId);
    }

    /**
     * List all templates for a facility.
     */
    public List<TemplateListItem> listTemplates(UUID facilityId) {
        return templateRepository.findByFacilityIdOrderByCreatedAtDesc(facilityId)
                .stream()
                .map(t -> new TemplateListItem(
                        t.getId(),
                        t.getName(),
                        t.getTemplateType(),
                        t.getVersion(),
                        t.getActive(),
                        t.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Upload and create a new template.
     */
    @Transactional
    public FacilityTemplate uploadTemplate(
            UUID facilityId,
            String name,
            String templateType,
            String version,
            MultipartFile file,
            UUID createdByUserId,
            boolean setActive) throws IOException {

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));

        // Check for duplicate version
        if (templateRepository.findByFacilityIdAndVersion(facilityId, version).isPresent()) {
            throw new IllegalArgumentException("Template version already exists: " + version);
        }

        // Validate template type
        if (!templateType.equals("HTML") && !templateType.equals("PDF")) {
            throw new IllegalArgumentException("Template type must be HTML or PDF");
        }

        // Save file
        String extension = templateType.equals("HTML") ? ".html" : ".pdf";
        String filename = String.format("%s_%s_%s%s", 
                facility.getCode(), 
                version.replace("/", "_"), 
                System.currentTimeMillis(),
                extension);
        Path filePath = templatesDir.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        // Create template record
        FacilityTemplate template = new FacilityTemplate();
        template.setFacility(facility);
        template.setName(name);
        template.setTemplateType(templateType);
        template.setContentLocation(filePath.toString());
        template.setVersion(version);
        template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());

        if (createdByUserId != null) {
            userRepository.findById(createdByUserId)
                    .ifPresent(template::setCreatedBy);
        }

        // Handle activation
        if (setActive) {
            templateRepository.deactivateAllForFacility(facilityId);
            template.setActive(true);
        } else {
            template.setActive(false);
        }

        return templateRepository.save(template);
    }

    /**
     * Create a template with content string (for HTML templates).
     */
    @Transactional
    public FacilityTemplate createHtmlTemplate(
            UUID facilityId,
            String name,
            String version,
            String htmlContent,
            UUID createdByUserId,
            boolean setActive) throws IOException {

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));

        // Check for duplicate version
        if (templateRepository.findByFacilityIdAndVersion(facilityId, version).isPresent()) {
            throw new IllegalArgumentException("Template version already exists: " + version);
        }

        // Save HTML content to file
        String filename = String.format("%s_%s_%s.html", 
                facility.getCode(), 
                version.replace("/", "_"), 
                System.currentTimeMillis());
        Path filePath = templatesDir.resolve(filename);
        Files.writeString(filePath, htmlContent);

        // Create template record
        FacilityTemplate template = new FacilityTemplate();
        template.setFacility(facility);
        template.setName(name);
        template.setTemplateType("HTML");
        template.setContentLocation(filePath.toString());
        template.setVersion(version);
        template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());

        if (createdByUserId != null) {
            userRepository.findById(createdByUserId)
                    .ifPresent(template::setCreatedBy);
        }

        // Handle activation
        if (setActive) {
            templateRepository.deactivateAllForFacility(facilityId);
            template.setActive(true);
        } else {
            template.setActive(false);
        }

        return templateRepository.save(template);
    }

    /**
     * Activate a specific template.
     */
    @Transactional
    public FacilityTemplate activateTemplate(UUID facilityId, UUID templateId) {
        FacilityTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        if (!template.getFacility().getId().equals(facilityId)) {
            throw new IllegalArgumentException("Template does not belong to facility");
        }

        // Deactivate all other templates for this facility
        templateRepository.deactivateAllForFacility(facilityId);

        // Activate this one
        template.setActive(true);
        template.setUpdatedAt(Instant.now());
        return templateRepository.save(template);
    }

    /**
     * Read template content.
     */
    public String readTemplateContent(FacilityTemplate template) throws IOException {
        Path path = Paths.get(template.getContentLocation());
        if (Files.exists(path)) {
            return Files.readString(path);
        }
        throw new IOException("Template file not found: " + template.getContentLocation());
    }
}
