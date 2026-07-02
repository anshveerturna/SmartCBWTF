package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.FacilityTemplate;
import com.smartcbwtf.service.FacilityTemplateService;
import com.smartcbwtf.service.TenantAssertionService;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateControllerValidationTest {

    @Mock
    private FacilityTemplateService templateService;
    @Mock
    private TenantAssertionService tenantAssertionService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private TemplateController controller;
    private UUID userId;

    @BeforeEach
    void setUp() {
        controller = new TemplateController(templateService, tenantAssertionService);
        userId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, UUID.randomUUID(), null, "CBWTF_ADMIN", "admin"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void htmlTemplateEndpointValidatesTypedRequestBody() throws NoSuchMethodException {
        Method method = TemplateController.class.getDeclaredMethod(
                "createHtmlTemplate", UUID.class, TemplateController.HtmlTemplateRequest.class);
        Parameter parameter = method.getParameters()[1];

        assertTrue(parameter.isAnnotationPresent(Valid.class));
        assertTrue(parameter.isAnnotationPresent(RequestBody.class));
    }

    @Test
    void uploadTemplateEndpointValidatesFormMetadata() throws NoSuchMethodException {
        assertTrue(TemplateController.class.isAnnotationPresent(Validated.class));
        Method method = TemplateController.class.getDeclaredMethod(
                "uploadTemplate", UUID.class, String.class, String.class, String.class,
                MultipartFile.class, boolean.class);

        assertConstrainedStringParameter(method.getParameters()[1], 120);
        assertConstrainedStringParameter(method.getParameters()[2], 10);
        assertTrue(method.getParameters()[2].isAnnotationPresent(Pattern.class));
        assertConstrainedStringParameter(method.getParameters()[3], 80);
    }

    @Test
    void htmlTemplateRequestBoundsTemplateMetadataAndContent() {
        assertFieldViolation(new TemplateController.HtmlTemplateRequest("", "v1", "<p>ok</p>", false), "name");
        assertFieldViolation(new TemplateController.HtmlTemplateRequest("Agreement", "", "<p>ok</p>", false),
                "version");
        assertFieldViolation(new TemplateController.HtmlTemplateRequest("x".repeat(121), "v1", "<p>ok</p>", false),
                "name");
        assertFieldViolation(new TemplateController.HtmlTemplateRequest("Agreement", "x".repeat(81), "<p>ok</p>",
                false), "version");
        assertFieldViolation(new TemplateController.HtmlTemplateRequest("Agreement", "v1", "", false),
                "htmlContent");
    }

    @Test
    void createHtmlTemplateTrimsMetadataBeforeServiceCall() throws Exception {
        UUID facilityId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        FacilityTemplate template = new FacilityTemplate();
        template.setId(templateId);
        template.setName("Agreement");
        template.setVersion("v1");
        template.setActive(true);
        when(templateService.createHtmlTemplate(eq(facilityId), eq("Agreement"), eq("v1"),
                eq("  <p>keep content spacing</p>  "), eq(userId), eq(true))).thenReturn(template);

        var response = controller.createHtmlTemplate(facilityId, new TemplateController.HtmlTemplateRequest(
                "  Agreement  ", "  v1  ", "  <p>keep content spacing</p>  ", true));

        assertEquals(201, response.getStatusCode().value());
        verify(tenantAssertionService).assertCanAccessFacility(facilityId);
        verify(templateService).createHtmlTemplate(facilityId, "Agreement", "v1",
                "  <p>keep content spacing</p>  ", userId, true);
    }

    @Test
    void uploadTemplateTrimsMetadataBeforeServiceCall() throws Exception {
        UUID facilityId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "template.html", "text/html", "<p>Template</p>".getBytes());
        FacilityTemplate template = new FacilityTemplate();
        template.setId(templateId);
        template.setName("Agreement");
        template.setVersion("v2");
        template.setActive(false);
        when(templateService.uploadTemplate(eq(facilityId), eq("Agreement"), eq("html"), eq("v2"),
                same(file), eq(userId), eq(false))).thenReturn(template);

        var response = controller.uploadTemplate(facilityId, "  Agreement  ", "  html  ", "  v2  ",
                file, false);

        assertEquals(201, response.getStatusCode().value());
        verify(tenantAssertionService).assertCanAccessFacility(facilityId);
        verify(templateService).uploadTemplate(facilityId, "Agreement", "html", "v2", file, userId, false);
    }

    private void assertConstrainedStringParameter(Parameter parameter, int maxSize) {
        assertTrue(parameter.isAnnotationPresent(NotBlank.class));
        Size size = parameter.getAnnotation(Size.class);
        assertEquals(maxSize, size.max());
    }

    private void assertFieldViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field + " on " + request.getClass().getSimpleName());
    }
}
