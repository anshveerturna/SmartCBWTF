package com.smartcbwtf.service;

import com.smartcbwtf.domain.GlobalEmailTemplate;
import com.smartcbwtf.domain.enums.TemplateCode;
import com.smartcbwtf.repository.GlobalEmailTemplateRepository;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for managing global email templates.
 * SuperAdmin-only operations.
 * Includes placeholder validation, HTML sanitization, and version management.
 */
@Service
public class GlobalEmailTemplateService {

    private static final Logger log = LoggerFactory.getLogger(GlobalEmailTemplateService.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private static final Safelist EMAIL_SAFELIST = Safelist.relaxed()
            .addTags("table", "tr", "td", "th", "thead", "tbody")
            .addAttributes("table", "style", "border", "cellpadding", "cellspacing")
            .addAttributes("td", "style", "align", "valign", "colspan", "rowspan")
            .addAttributes("th", "style", "align", "valign", "colspan", "rowspan")
            .addAttributes("tr", "style")
            .addAttributes("div", "style")
            .addAttributes("p", "style")
            .addAttributes("span", "style")
            .addAttributes("img", "src", "alt", "style", "width", "height")
            .addAttributes("a", "href", "style", "target")
            .removeProtocols("a", "href", "javascript");

    private final GlobalEmailTemplateRepository templateRepository;

    public GlobalEmailTemplateService(GlobalEmailTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Get the active template for a given code.
     * FAIL-CLOSED: Throws exception if no active template exists.
     */
    public GlobalEmailTemplate getActiveTemplate(String templateCodeStr) {
        TemplateCode code = validateCode(templateCodeStr);
        return templateRepository.findByTemplateCodeAndIsActiveTrue(code.name())
                .orElseThrow(() -> {
                    log.error("FAIL-CLOSED: No active template for code: {}", code);
                    return new TemplateNotFoundException("No active template for: " + code);
                });
    }

    public GlobalEmailTemplate getActiveTemplate(TemplateCode code) {
        return templateRepository.findByTemplateCodeAndIsActiveTrue(code.name())
                .orElseThrow(() -> {
                    log.error("FAIL-CLOSED: No active template for code: {}", code);
                    return new TemplateNotFoundException("No active template for: " + code);
                });
    }

    public List<GlobalEmailTemplate> getAllActiveTemplates() {
        return templateRepository.findAllActiveTemplates();
    }

    public List<GlobalEmailTemplate> getTemplateVersions(String templateCodeStr) {
        TemplateCode code = validateCode(templateCodeStr);
        return templateRepository.findByTemplateCodeOrderByVersionDesc(code.name());
    }

    @Transactional
    public GlobalEmailTemplate updateTemplate(String templateCodeStr, String subject, String bodyHtml,
            String[] requiredPlaceholders, String[] optionalPlaceholders,
            UUID createdBy) {

        TemplateCode code = validateCode(templateCodeStr);

        // Validate placeholders
        validatePlaceholders(code, subject, bodyHtml, requiredPlaceholders, optionalPlaceholders);

        String sanitizedBody = sanitizeHtml(bodyHtml);

        // Deactivate current active
        templateRepository.findByTemplateCodeAndIsActiveTrue(code.name())
                .ifPresent(t -> {
                    t.setIsActive(false);
                    templateRepository.save(t);
                });

        int nextVersion = templateRepository.findLatestVersion(code.name()).orElse(0) + 1;

        GlobalEmailTemplate newTemplate = new GlobalEmailTemplate();
        newTemplate.setTemplateCode(code.name());
        newTemplate.setCategory(code.getCategory());
        newTemplate.setSubject(subject);
        newTemplate.setBodyHtml(sanitizedBody);
        newTemplate.setRequiredPlaceholders(requiredPlaceholders);
        newTemplate.setOptionalPlaceholders(optionalPlaceholders);
        newTemplate.setVersion(nextVersion);
        newTemplate.setIsActive(true);
        newTemplate.setCreatedBy(createdBy);
        newTemplate.setCreatedAt(Instant.now());
        newTemplate.setUpdatedAt(Instant.now());

        GlobalEmailTemplate saved = templateRepository.save(newTemplate);
        log.info("Created new template version: {} v{} [{}]", code, nextVersion, code.getCategory());

        return saved;
    }

    @Transactional
    public GlobalEmailTemplate activateVersion(String templateCodeStr, int version) {
        TemplateCode code = validateCode(templateCodeStr);

        templateRepository.findByTemplateCodeAndIsActiveTrue(code.name())
                .ifPresent(t -> {
                    t.setIsActive(false);
                    templateRepository.save(t);
                });

        GlobalEmailTemplate target = templateRepository.findByTemplateCodeAndVersion(code.name(), version)
                .orElseThrow(
                        () -> new TemplateNotFoundException("Version not found: " + code + " v" + version));

        target.setIsActive(true);
        target.setUpdatedAt(Instant.now());
        // Ensure category is consistent (in case of legacy data)
        if (target.getCategory() == null) {
            target.setCategory(code.getCategory());
        }

        log.info("Activated template version: {} v{}", code, version);
        return templateRepository.save(target);
    }

    public void validatePlaceholders(TemplateCode code, String subject, String bodyHtml,
            String[] requiredPlaceholders, String[] optionalPlaceholders) {
        Set<String> found = extractPlaceholders(subject + " " + bodyHtml);
        Set<String> required = new HashSet<>(Arrays.asList(requiredPlaceholders));
        Set<String> allowed = new HashSet<>(Arrays.asList(requiredPlaceholders));
        allowed.addAll(Arrays.asList(optionalPlaceholders));

        Set<String> missing = new HashSet<>(required);
        missing.removeAll(found);
        if (!missing.isEmpty()) {
            throw new TemplateValidationException("Missing required placeholders for " + code + ": " + missing);
        }

        Set<String> unknown = new HashSet<>(found);
        unknown.removeAll(allowed);
        if (!unknown.isEmpty()) {
            throw new TemplateValidationException("Unknown placeholders for " + code + ": " + unknown);
        }
    }

    private TemplateCode validateCode(String codeStr) {
        try {
            return TemplateCode.valueOf(codeStr);
        } catch (IllegalArgumentException e) {
            throw new TemplateValidationException("Invalid template code: " + codeStr);
        }
    }

    public Set<String> extractPlaceholders(String text) {
        Set<String> placeholders = new HashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders;
    }

    public String sanitizeHtml(String html) {
        return Jsoup.clean(html, EMAIL_SAFELIST);
    }

    public RenderedEmail renderTemplate(String templateCodeStr, Map<String, String> data) {
        GlobalEmailTemplate template = getActiveTemplate(templateCodeStr);

        String renderedSubject = replacePlaceholders(template.getSubject(), data);
        String renderedBody = replacePlaceholders(template.getBodyHtml(), data);
        String checksum = computeChecksum(renderedSubject + renderedBody);

        return new RenderedEmail(
                template.getTemplateCode(),
                template.getVersion(),
                checksum,
                renderedSubject,
                renderedBody);
    }

    private String replacePlaceholders(String text, Map<String, String> data) {
        String result = text;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getValue() != null) {
                result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return result;
    }

    public String computeChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public record RenderedEmail(
            String templateCode,
            int templateVersion,
            String templateChecksum,
            String subject,
            String bodyHtml) {
    }

    public static class TemplateNotFoundException extends RuntimeException {
        public TemplateNotFoundException(String message) {
            super(message);
        }
    }

    public static class TemplateValidationException extends RuntimeException {
        public TemplateValidationException(String message) {
            super(message);
        }
    }
}
