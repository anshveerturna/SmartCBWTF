package com.smartcbwtf.service;

import com.smartcbwtf.domain.EmailTemplate;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for validating and rendering email templates.
 * Enforces placeholder requirements and sanitizes HTML.
 */
@Service
public class TemplateValidationService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([a-zA-Z]+)\\}\\}");

    // Define required placeholders per template type
    private static final Map<String, Set<String>> REQUIRED_PLACEHOLDERS = Map.of(
            "HCF_WELCOME", Set.of("hcfName", "facilityName"),
            "HCF_CREDENTIALS", Set.of("hcfName", "username", "loginUrl"),
            "AGREEMENT_EXPIRING", Set.of("hcfName", "agreementNumber", "expiryDate"),
            "INVOICE_GENERATED", Set.of("hcfName", "invoiceNumber", "amount", "dueDate"),
            "PAYMENT_REMINDER", Set.of("hcfName", "invoiceNumber", "amountDue", "dueDate"),
            "PAYMENT_OVERDUE", Set.of("hcfName", "invoiceNumber", "amountDue", "daysOverdue"));

    // All known placeholders
    private static final Set<String> ALL_KNOWN_PLACEHOLDERS = Set.of(
            "hcfName", "facilityName", "agreementNumber", "registrationDate",
            "username", "loginUrl", "expiryDate", "renewalUrl",
            "invoiceNumber", "amount", "dueDate", "viewUrl",
            "amountDue", "paymentUrl", "daysOverdue");

    // HTML sanitizer policy
    private final PolicyFactory htmlPolicy = new HtmlPolicyBuilder()
            .allowElements("html", "head", "body", "div", "span", "p", "br", "hr",
                    "h1", "h2", "h3", "h4", "h5", "h6",
                    "strong", "em", "b", "i", "u",
                    "ul", "ol", "li",
                    "table", "thead", "tbody", "tr", "th", "td",
                    "a", "img")
            .allowAttributes("href").onElements("a")
            .allowAttributes("src", "alt", "width", "height").onElements("img")
            .allowAttributes("style").globally()
            .allowAttributes("class").globally()
            .allowUrlProtocols("http", "https", "mailto")
            .toFactory();

    /**
     * Validate a template against requirements.
     * 
     * @throws TemplateValidationException if validation fails
     */
    public void validateTemplate(String templateCode, String bodyTemplate) {
        Set<String> required = REQUIRED_PLACEHOLDERS.getOrDefault(templateCode, Set.of());
        Set<String> found = extractPlaceholders(bodyTemplate);

        // Check all required placeholders exist
        Set<String> missing = new HashSet<>(required);
        missing.removeAll(found);
        if (!missing.isEmpty()) {
            throw new TemplateValidationException("Missing required placeholders: " + missing);
        }

        // Check no unknown placeholders
        Set<String> unknown = new HashSet<>(found);
        unknown.removeAll(ALL_KNOWN_PLACEHOLDERS);
        if (!unknown.isEmpty()) {
            throw new TemplateValidationException("Unknown placeholders: " + unknown);
        }
    }

    /**
     * Sanitize HTML to remove dangerous elements.
     */
    public String sanitizeHtml(String html) {
        if (html == null)
            return null;
        return htmlPolicy.sanitize(html);
    }

    /**
     * Render a template by replacing placeholders with values.
     */
    public String render(String template, Map<String, String> variables) {
        if (template == null)
            return null;
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    /**
     * Extract all placeholders from a template.
     */
    public Set<String> extractPlaceholders(String template) {
        Set<String> placeholders = new HashSet<>();
        if (template == null)
            return placeholders;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders;
    }

    /**
     * Get required placeholders for a template type.
     */
    public Set<String> getRequiredPlaceholders(String templateCode) {
        return REQUIRED_PLACEHOLDERS.getOrDefault(templateCode, Set.of());
    }

    /**
     * Get all known placeholders.
     */
    public Set<String> getAllKnownPlaceholders() {
        return ALL_KNOWN_PLACEHOLDERS;
    }

    /**
     * Exception for template validation failures.
     */
    public static class TemplateValidationException extends RuntimeException {
        public TemplateValidationException(String message) {
            super(message);
        }
    }
}
