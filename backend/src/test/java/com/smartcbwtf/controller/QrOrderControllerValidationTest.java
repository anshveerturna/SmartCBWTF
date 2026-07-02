package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.QrLabelOrder;
import com.smartcbwtf.dto.QrGenerateRequest;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.service.PdfService;
import com.smartcbwtf.service.QrOrderService;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrOrderControllerValidationTest {

    @Mock
    private QrOrderService qrOrderService;
    @Mock
    private PdfService pdfService;
    @Mock
    private HcfAccessGuard accessGuard;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void qrOrderMutationEndpointsValidateRequestBodies() throws NoSuchMethodException {
        assertValidatedRequestBody(HcfQrOrderController.class, "requestFromCbwtf",
                HcfQrOrderController.QrOrderRequest.class, 0);
        assertValidatedRequestBody(HcfQrOrderController.class, "selfGenerate",
                HcfQrOrderController.QrOrderRequest.class, 0);
        assertValidatedRequestBody(CbwtfQrOrderController.class, "generateForHcf",
                CbwtfQrOrderController.GenerateForHcfRequest.class, 0);
        assertValidatedRequestBody(CbwtfQrOrderController.class, "rejectOrder",
                CbwtfQrOrderController.RejectRequest.class, 1, UUID.class);
    }

    @Test
    void qrOrderRequestsBoundClientControlledFields() {
        assertFieldViolation(new HcfQrOrderController.QrOrderRequest("BLACK", 1, null, null, null),
                "wasteCategory");
        assertFieldViolation(new HcfQrOrderController.QrOrderRequest("YELLOW", 0, null, null, null),
                "quantity");
        assertFieldViolation(new HcfQrOrderController.QrOrderRequest("YELLOW", 1, "x".repeat(1001), null, null),
                "notes");
        assertFieldViolation(new HcfQrOrderController.QrOrderRequest(null, null, null,
                Map.of("YELLOW", 0), null), "categoryQuantities");
        assertFieldViolation(new HcfQrOrderController.QrOrderRequest(null, null, null,
                fiveCategoryMap(), null), "categoryQuantities");
        assertFieldViolation(new HcfQrOrderController.QrOrderRequest(null, null, null,
                Map.of("YELLOW", 1), LocalDate.now().minusDays(1)), "validUntil");
        assertTrue(validator.validate(new HcfQrOrderController.QrOrderRequest(null, null, null,
                Map.of("YELLOW", 1, "RED", 2), LocalDate.now())).isEmpty());
    }

    @Test
    void adminQrOrderRequestsRequireTenantTargetAndBoundReason() {
        assertFieldViolation(new CbwtfQrOrderController.GenerateForHcfRequest(
                null, null, null, Map.of("YELLOW", 1), null), "hcfId");
        assertFieldViolation(new CbwtfQrOrderController.GenerateForHcfRequest(
                UUID.randomUUID(), "BLACK", 1, null, null), "wasteCategory");
        assertFieldViolation(new CbwtfQrOrderController.GenerateForHcfRequest(
                UUID.randomUUID(), null, null, Map.of("YELLOW", 100_001), null), "categoryQuantities");
        assertFieldViolation(new CbwtfQrOrderController.RejectRequest("x".repeat(1001)), "reason");
        assertTrue(validator.validate(new CbwtfQrOrderController.GenerateForHcfRequest(
                UUID.randomUUID(), null, null, Map.of("WHITE", 1), LocalDate.now())).isEmpty());
    }

    @Test
    void legacyQrGenerateRequestUsesSameWasteCategoryContract() {
        QrGenerateRequest request = new QrGenerateRequest();
        request.setHcfId(UUID.randomUUID());
        request.setWasteCategory("BLACK");
        request.setValidFrom(Instant.now());
        request.setValidTo(Instant.now().plusSeconds(3600));

        assertFieldViolation(request, "wasteCategory");
    }

    @Test
    void hcfQrGenerateRejectsInvalidCategoryBeforeServiceCall() {
        UUID hcfId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
        HcfQrOrderController controller = new HcfQrOrderController(qrOrderService, pdfService, accessGuard);

        var response = controller.selfGenerate(new HcfQrOrderController.QrOrderRequest(
                null, null, null, Map.of("BLACK", 1), null));

        assertEquals(400, response.getStatusCode().value());
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
        verifyNoInteractions(qrOrderService);
    }

    @Test
    void cbwtfQrGenerateRejectsInvalidCategoryBeforeServiceCall() {
        TenantContext.set(new TenantContext.TenantInfo(
                UUID.randomUUID(), UUID.randomUUID(), null, "CBWTF_ADMIN", "admin"));
        CbwtfQrOrderController controller = new CbwtfQrOrderController(qrOrderService, pdfService);

        var response = controller.generateForHcf(new CbwtfQrOrderController.GenerateForHcfRequest(
                UUID.randomUUID(), null, null, Map.of("BLACK", 1), null));

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(qrOrderService);
    }

    @Test
    void hcfQrOrderPdfDownloadUsesNoStoreCacheHeader() throws Exception {
        UUID hcfId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        QrLabelOrder order = new QrLabelOrder();
        order.setId(orderId);
        order.setPdfUrl("/files/labels/order.pdf");
        Path pdf = tempDir.resolve("order.pdf");
        Files.write(pdf, new byte[] { 1, 2, 3 });
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
        when(qrOrderService.getOrderPdfForHcf(orderId, hcfId, facilityId)).thenReturn(order);
        when(pdfService.generatedFilePath(order.getPdfUrl())).thenReturn(pdf);
        HcfQrOrderController controller = new HcfQrOrderController(qrOrderService, pdfService, accessGuard);

        var response = controller.downloadOrderPdf(orderId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
    }

    private void assertValidatedRequestBody(Class<?> controllerClass, String methodName, Class<?> requestType,
            int parameterIndex, Class<?>... leadingParameterTypes) throws NoSuchMethodException {
        Class<?>[] parameterTypes = new Class<?>[leadingParameterTypes.length + 1];
        System.arraycopy(leadingParameterTypes, 0, parameterTypes, 0, leadingParameterTypes.length);
        parameterTypes[parameterTypes.length - 1] = requestType;
        Method method = controllerClass.getDeclaredMethod(methodName, parameterTypes);
        Parameter parameter = method.getParameters()[parameterIndex];

        assertTrue(parameter.isAnnotationPresent(Valid.class), methodName + " request must be validated");
        assertTrue(parameter.isAnnotationPresent(RequestBody.class), methodName + " request must remain a body");
    }

    private void assertFieldViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> {
                    String property = violation.getPropertyPath().toString();
                    return field.equals(property)
                            || property.endsWith("." + field)
                            || property.startsWith(field + "[")
                            || property.startsWith(field + ".");
                }),
                () -> "Expected validation violation for " + field);
    }

    private Map<String, Integer> fiveCategoryMap() {
        Map<String, Integer> categories = new LinkedHashMap<>();
        categories.put("YELLOW", 1);
        categories.put("RED", 1);
        categories.put("BLUE", 1);
        categories.put("WHITE", 1);
        categories.put("GREEN", 1);
        return categories;
    }
}
