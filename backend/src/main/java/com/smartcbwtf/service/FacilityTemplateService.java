package com.smartcbwtf.service;

import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.FacilityTemplate;
import com.smartcbwtf.dto.TemplateListItem;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.FacilityTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    static final long HTML_TEMPLATE_MAX_BYTES = 1024L * 1024L;
    static final long PDF_TEMPLATE_MAX_BYTES = 10L * 1024L * 1024L;

    private final FacilityTemplateRepository templateRepository;
    private final FacilityRepository facilityRepository;
    private final AppUserRepository userRepository;
    private final Path templatesDir;

    @Autowired
    public FacilityTemplateService(
            FacilityTemplateRepository templateRepository,
            FacilityRepository facilityRepository,
            AppUserRepository userRepository) {
        this(templateRepository, facilityRepository, userRepository, Paths.get("files", "templates"));
    }

    FacilityTemplateService(
            FacilityTemplateRepository templateRepository,
            FacilityRepository facilityRepository,
            AppUserRepository userRepository,
            Path templatesDir) {
        this.templateRepository = templateRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
        this.templatesDir = templatesDir.toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.templatesDir);
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

        String normalizedTemplateType = normalizeTemplateType(templateType);

        String extension = validateTemplateFile(normalizedTemplateType, file);
        String filename = String.format("%s_%s_%s.%s",
                safeFilenameToken(facility.getCode(), "facility code"),
                safeFilenameToken(version, "version"),
                System.currentTimeMillis(),
                extension);
        Path filePath = resolveTemplateFile(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create template record
        FacilityTemplate template = new FacilityTemplate();
        template.setFacility(facility);
        template.setName(name);
        template.setTemplateType(normalizedTemplateType);
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

        validateHtmlContent(htmlContent);
        String filename = String.format("%s_%s_%s.html", 
                safeFilenameToken(facility.getCode(), "facility code"),
                safeFilenameToken(version, "version"),
                System.currentTimeMillis());
        Path filePath = resolveTemplateFile(filename);
        Files.writeString(filePath, htmlContent, StandardCharsets.UTF_8);

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
        Path path = resolveStoredTemplatePath(template.getContentLocation());
        if (Files.exists(path)) {
            return Files.readString(path);
        }
        throw new IOException("Template file not found: " + template.getContentLocation());
    }

    private String normalizeTemplateType(String templateType) {
        if (templateType == null) {
            throw new IllegalArgumentException("Template type must be HTML or PDF");
        }
        String normalized = templateType.trim().toUpperCase();
        if (!normalized.equals("HTML") && !normalized.equals("PDF")) {
            throw new IllegalArgumentException("Template type must be HTML or PDF");
        }
        return normalized;
    }

    private String validateTemplateFile(String templateType, MultipartFile file) {
        if ("HTML".equals(templateType)) {
            return UploadFileValidator.htmlTemplateExtension(file, HTML_TEMPLATE_MAX_BYTES);
        }
        return UploadFileValidator.rentAgreementExtension(file, PDF_TEMPLATE_MAX_BYTES);
    }

    private void validateHtmlContent(String htmlContent) {
        if (htmlContent == null || htmlContent.isBlank()) {
            throw new IllegalArgumentException("HTML template content is required");
        }
        if (htmlContent.getBytes(StandardCharsets.UTF_8).length > HTML_TEMPLATE_MAX_BYTES) {
            throw new IllegalArgumentException("HTML template must be under 1MB");
        }
    }

    private String safeFilenameToken(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Template " + label + " is required");
        }
        String token = value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        token = token.replaceAll("\\.+", ".");
        token = token.replaceAll("^\\.+", "");
        token = token.replaceAll("\\.+$", "");
        if (token.isBlank() || token.contains("..") || token.contains("/") || token.contains("\\")) {
            throw new IllegalArgumentException("Invalid template " + label);
        }
        return token;
    }

    private Path resolveTemplateFile(String filename) {
        Path path = templatesDir.resolve(filename).normalize();
        if (!path.startsWith(templatesDir)) {
            throw new IllegalArgumentException("Invalid template storage path");
        }
        return path;
    }

    private Path resolveStoredTemplatePath(String contentLocation) {
        if (contentLocation == null || contentLocation.isBlank()) {
            throw new IllegalArgumentException("Template content location is required");
        }
        if (contentLocation.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid template content location");
        }
        Path rawPath = Paths.get(contentLocation);
        Path resolvedPath;
        if (rawPath.isAbsolute() || contentLocation.contains("/") || contentLocation.contains("\\")) {
            resolvedPath = rawPath.toAbsolutePath().normalize();
        } else {
            resolvedPath = templatesDir.resolve(rawPath).normalize();
        }
        if (!resolvedPath.startsWith(templatesDir)) {
            throw new IllegalArgumentException("Invalid template content location");
        }
        Path filename = resolvedPath.getFileName();
        if (filename == null || filename.toString().isBlank() || filename.toString().contains("..")
                || filename.toString().contains("/") || filename.toString().contains("\\")) {
            throw new IllegalArgumentException("Invalid template content location");
        }
        return resolvedPath;
    }
}
