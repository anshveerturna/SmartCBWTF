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
    private static final Pattern PLACEHOLDER_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,63}$");
    private static final int MAX_TEMPLATE_CODE_LENGTH = 50;
    private static final int MAX_SUBJECT_LENGTH = 200;
    private static final int MAX_BODY_HTML_LENGTH = 100_000;
    private static final int MAX_PLACEHOLDERS = 50;
    private static final int MAX_PLACEHOLDER_LENGTH = 64;
    private static final int MAX_RENDER_DATA_ENTRIES = 100;
    private static final int MAX_RENDER_VALUE_LENGTH = 2_000;

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
        String safeSubject = normalizeSubject(subject);
        String safeBodyHtml = requireText(bodyHtml, "Body HTML", MAX_BODY_HTML_LENGTH);
        String[] required = normalizePlaceholders(requiredPlaceholders);
        String[] optional = normalizePlaceholders(optionalPlaceholders);

        // Validate placeholders
        validatePlaceholders(code, safeSubject, safeBodyHtml, required, optional);

        String sanitizedBody = sanitizeHtml(safeBodyHtml);

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
        newTemplate.setSubject(safeSubject);
        newTemplate.setBodyHtml(sanitizedBody);
        newTemplate.setRequiredPlaceholders(required);
        newTemplate.setOptionalPlaceholders(optional);
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
        String safeSubject = subject == null ? "" : subject;
        String safeBodyHtml = bodyHtml == null ? "" : bodyHtml;
        String[] requiredPlaceholdersSafe = normalizePlaceholders(requiredPlaceholders);
        String[] optionalPlaceholdersSafe = normalizePlaceholders(optionalPlaceholders);

        Set<String> found = extractPlaceholders(safeSubject + " " + safeBodyHtml);
        Set<String> required = new HashSet<>(Arrays.asList(requiredPlaceholdersSafe));
        Set<String> allowed = new HashSet<>(Arrays.asList(requiredPlaceholdersSafe));
        allowed.addAll(Arrays.asList(optionalPlaceholdersSafe));

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
        String cleaned = cleanLineRequired(codeStr, "Template code", MAX_TEMPLATE_CODE_LENGTH).toUpperCase(Locale.ROOT);
        try {
            return TemplateCode.valueOf(cleaned);
        } catch (IllegalArgumentException e) {
            throw new TemplateValidationException("Invalid template code: " + cleaned);
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
        return html == null ? "" : Jsoup.clean(html, EMAIL_SAFELIST);
    }

    public RenderedEmail renderTemplate(String templateCodeStr, Map<String, String> data) {
        GlobalEmailTemplate template = getActiveTemplate(templateCodeStr);
        Map<String, String> safeData = normalizeRenderData(data);

        String renderedSubject = replacePlaceholders(template.getSubject(), safeData, false);
        String renderedBody = replacePlaceholders(template.getBodyHtml(), safeData, true);
        String checksum = computeChecksum(renderedSubject + renderedBody);

        return new RenderedEmail(
                template.getTemplateCode(),
                template.getVersion(),
                checksum,
                renderedSubject,
                renderedBody);
    }

    private String replacePlaceholders(String text, Map<String, String> data, boolean htmlContext) {
        String result = text == null ? "" : text;
        if (data == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getValue() != null) {
                String value = htmlContext
                        ? escapeHtml(entry.getValue())
                        : cleanSubjectValue(entry.getValue());
                result = result.replace("{{" + entry.getKey() + "}}", value);
            }
        }
        return result;
    }

    private String cleanSubjectValue(String value) {
        return value.replaceAll("[\\r\\n\\t]+", " ");
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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

    private String normalizeSubject(String subject) {
        return cleanLineRequired(subject, "Subject", MAX_SUBJECT_LENGTH);
    }

    private String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new TemplateValidationException(fieldName + " is required");
        }
        if (value.length() > maxLength) {
            throw new TemplateValidationException(fieldName + " must be " + maxLength + " characters or less");
        }
        return value;
    }

    private String[] normalizePlaceholders(String[] placeholders) {
        if (placeholders == null || placeholders.length == 0) {
            return new String[0];
        }
        if (placeholders.length > MAX_PLACEHOLDERS) {
            throw new TemplateValidationException("Too many placeholders");
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String placeholder : placeholders) {
            normalized.add(normalizePlaceholderName(placeholder));
        }
        return normalized.toArray(String[]::new);
    }

    private Map<String, String> normalizeRenderData(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return Map.of();
        }
        if (data.size() > MAX_RENDER_DATA_ENTRIES) {
            throw new TemplateValidationException("Sample data contains too many entries");
        }

        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = normalizePlaceholderName(entry.getKey());
            String value = entry.getValue();
            if (value != null && value.length() > MAX_RENDER_VALUE_LENGTH) {
                throw new TemplateValidationException("Sample value for " + key + " is too long");
            }
            normalized.put(key, value);
        }
        return normalized;
    }

    private String normalizePlaceholderName(String value) {
        String cleaned = cleanLineRequired(value, "Placeholder name", MAX_PLACEHOLDER_LENGTH);
        if (!PLACEHOLDER_NAME_PATTERN.matcher(cleaned).matches()) {
            throw new TemplateValidationException("Invalid placeholder name: " + cleaned);
        }
        return cleaned;
    }

    private String cleanLineRequired(String value, String fieldName, int maxLength) {
        String cleaned = cleanLine(value);
        if (cleaned.isBlank()) {
            throw new TemplateValidationException(fieldName + " is required");
        }
        if (cleaned.length() > maxLength) {
            throw new TemplateValidationException(fieldName + " must be " + maxLength + " characters or less");
        }
        return cleaned;
    }

    private String cleanLine(String value) {
        return value == null ? "" : value.trim().replaceAll("[\\r\\n\\t]+", " ");
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

    public static class TemplateValidationException extends IllegalArgumentException {
        public TemplateValidationException(String message) {
            super(message);
        }
    }
}
