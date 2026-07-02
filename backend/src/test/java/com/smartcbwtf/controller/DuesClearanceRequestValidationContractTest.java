package com.smartcbwtf.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DuesClearanceRequestValidationContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void stateChangingDuesEndpointsValidateRequestBodies() throws NoSuchMethodException {
        assertValidatedBody(CbwtfDuesClearanceController.class, "submitForApproval",
                UUID.class, CbwtfDuesClearanceController.SubmitForApprovalRequest.class);
        assertValidatedBody(CbwtfDuesClearanceController.class, "rejectRequest",
                UUID.class, CbwtfDuesClearanceController.RejectClearanceRequest.class);
        assertValidatedBody(ManagementDuesApprovalController.class, "reject",
                UUID.class, ManagementDuesApprovalController.RejectRequest.class);
        assertValidatedBody(ManagementDuesApprovalController.class, "bulkApprove",
                ManagementDuesApprovalController.BulkApproveRequest.class);
        assertValidatedBody(HcfComplianceController.class, "requestDuesClearance",
                HcfComplianceController.DuesAccessRequest.class);
        assertValidatedBody(HcfDuesClearanceController.class, "requestReportAccess",
                HcfDuesClearanceController.DuesRequestBody.class);
    }

    @Test
    void cbwtfClearanceRequestsBoundAmountAndTextFields() {
        assertFieldViolation(new CbwtfDuesClearanceController.SubmitForApprovalRequest(null, null), "amount");
        assertFieldViolation(
                new CbwtfDuesClearanceController.SubmitForApprovalRequest(new BigDecimal("-1.00"), null),
                "amount");
        assertFieldViolation(
                new CbwtfDuesClearanceController.SubmitForApprovalRequest(new BigDecimal("1.001"), null),
                "amount");
        assertFieldViolation(
                new CbwtfDuesClearanceController.SubmitForApprovalRequest(BigDecimal.ONE, "x".repeat(1001)),
                "notes");
        assertTrue(validator.validate(
                new CbwtfDuesClearanceController.SubmitForApprovalRequest(new BigDecimal("99.99"), "ok"))
                .isEmpty());

        assertFieldViolation(new CbwtfDuesClearanceController.RejectClearanceRequest("   ", BigDecimal.ONE),
                "reason");
        assertFieldViolation(new CbwtfDuesClearanceController.RejectClearanceRequest("x".repeat(1001), BigDecimal.ONE),
                "reason");
        assertFieldViolation(new CbwtfDuesClearanceController.RejectClearanceRequest("reason", null), "amount");
    }

    @Test
    void managementApprovalRequestsBoundReasonAndBulkSize() {
        assertFieldViolation(new ManagementDuesApprovalController.RejectRequest("   "), "reason");
        assertFieldViolation(new ManagementDuesApprovalController.RejectRequest("x".repeat(1001)), "reason");

        assertFieldViolation(new ManagementDuesApprovalController.BulkApproveRequest(Collections.emptyList()), "ids");
        assertPathContainingViolation(new ManagementDuesApprovalController.BulkApproveRequest(
                Arrays.asList((UUID) null)), "ids");
        assertFieldViolation(new ManagementDuesApprovalController.BulkApproveRequest(
                java.util.stream.IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID()).toList()), "ids");
    }

    @Test
    void hcfReportAccessRequestsBoundReportPeriodAndNotes() {
        assertFieldViolation(new HcfComplianceController.DuesAccessRequest(0, 2026), "month");
        assertFieldViolation(new HcfComplianceController.DuesAccessRequest(13, 2026), "month");
        assertFieldViolation(new HcfComplianceController.DuesAccessRequest(7, 1999), "year");

        HcfDuesClearanceController.DuesRequestBody notes = new HcfDuesClearanceController.DuesRequestBody();
        notes.notes = "x".repeat(1001);
        assertFieldViolation(notes, "notes");
    }

    private void assertValidatedBody(Class<?> controllerClass, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = controllerClass.getDeclaredMethod(methodName, parameterTypes);
        Parameter bodyParameter = method.getParameters()[method.getParameterCount() - 1];

        assertTrue(bodyParameter.isAnnotationPresent(Valid.class), methodName + " request body must be validated");
        assertTrue(bodyParameter.isAnnotationPresent(RequestBody.class), methodName + " must remain a request body");
    }

    private void assertFieldViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field + " on " + request.getClass().getSimpleName());
    }

    private void assertPathContainingViolation(Object request, String pathFragment) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().contains(pathFragment)),
                () -> "Expected validation violation containing " + pathFragment + " on "
                        + request.getClass().getSimpleName());
    }
}
