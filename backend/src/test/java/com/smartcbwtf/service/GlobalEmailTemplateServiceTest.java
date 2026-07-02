package com.smartcbwtf.service;

import com.smartcbwtf.domain.GlobalEmailTemplate;
import com.smartcbwtf.repository.GlobalEmailTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalEmailTemplateServiceTest {

    @Mock
    private GlobalEmailTemplateRepository templateRepository;

    private GlobalEmailTemplateService service;

    @BeforeEach
    void setUp() {
        service = new GlobalEmailTemplateService(templateRepository);
    }

    @Test
    void renderTemplateEscapesHtmlPlaceholderValues() {
        GlobalEmailTemplate template = template(
                "Hello {{hcfName}}",
                "<p>{{hcfName}}</p><p>{{facilityName}}</p>");
        when(templateRepository.findByTemplateCodeAndIsActiveTrue("HCF_WELCOME"))
                .thenReturn(Optional.of(template));

        GlobalEmailTemplateService.RenderedEmail rendered = service.renderTemplate(
                "HCF_WELCOME",
                Map.of(
                        "hcfName", "Sunrise\r\n<script>alert(1)</script> & Co",
                        "facilityName", "Metro <b>CBWTF</b>"));

        assertEquals("Hello Sunrise <script>alert(1)</script> & Co", rendered.subject());
        assertTrue(rendered.bodyHtml().contains("Sunrise\r\n&lt;script&gt;alert(1)&lt;/script&gt; &amp; Co"));
        assertTrue(rendered.bodyHtml().contains("Metro &lt;b&gt;CBWTF&lt;/b&gt;"));
        assertFalse(rendered.bodyHtml().contains("<script>"));
        assertFalse(rendered.bodyHtml().contains("<b>CBWTF</b>"));
    }

    @Test
    void updateTemplateNormalizesSubjectPlaceholdersAndSanitizesHtml() {
        when(templateRepository.findByTemplateCodeAndIsActiveTrue("HCF_WELCOME"))
                .thenReturn(Optional.empty());
        when(templateRepository.findLatestVersion("HCF_WELCOME")).thenReturn(Optional.of(2));
        when(templateRepository.save(any(GlobalEmailTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateTemplate(
                "hcf_welcome",
                "  Hello\r\n{{hcfName}}  ",
                "<p>{{hcfName}}</p><script>alert(1)</script>",
                new String[] { " hcfName ", "hcfName" },
                null,
                null);

        ArgumentCaptor<GlobalEmailTemplate> savedTemplate = ArgumentCaptor.forClass(GlobalEmailTemplate.class);
        org.mockito.Mockito.verify(templateRepository).save(savedTemplate.capture());
        GlobalEmailTemplate saved = savedTemplate.getValue();
        assertEquals("HCF_WELCOME", saved.getTemplateCode());
        assertEquals("Hello {{hcfName}}", saved.getSubject());
        assertEquals(3, saved.getVersion());
        assertTrue(saved.getBodyHtml().contains("<p>{{hcfName}}</p>"));
        assertFalse(saved.getBodyHtml().contains("<script>"));
        assertEquals(Arrays.asList("hcfName"), Arrays.asList(saved.getRequiredPlaceholders()));
        assertEquals(0, saved.getOptionalPlaceholders().length);
    }

    @Test
    void renderTemplateRejectsOversizedOrInvalidSampleData() {
        GlobalEmailTemplate template = template("Hello {{hcfName}}", "<p>{{hcfName}}</p>");
        when(templateRepository.findByTemplateCodeAndIsActiveTrue("HCF_WELCOME"))
                .thenReturn(Optional.of(template));

        assertThrows(GlobalEmailTemplateService.TemplateValidationException.class,
                () -> service.renderTemplate("HCF_WELCOME", Map.of("bad key", "value")));

        assertThrows(GlobalEmailTemplateService.TemplateValidationException.class,
                () -> service.renderTemplate("HCF_WELCOME", Map.of("hcfName", "x".repeat(2_001))));
    }

    private static GlobalEmailTemplate template(String subject, String bodyHtml) {
        GlobalEmailTemplate template = new GlobalEmailTemplate();
        template.setTemplateCode("HCF_WELCOME");
        template.setVersion(1);
        template.setSubject(subject);
        template.setBodyHtml(bodyHtml);
        return template;
    }
}
