package com.smartcbwtf.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        log.warn("Validation failed for {}: {}", request.getRequestURI(), errors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, Map.of("errors", errors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidation(
            HandlerMethodValidationException ex,
            HttpServletRequest request) {
        List<String> errors = ex.getAllErrors().stream()
                .map(this::formatResolvableError)
                .toList();
        log.warn("Method validation failed for {}: {}", request.getRequestURI(), errors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, Map.of("errors", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        log.warn("Constraint validation failed for {}: {}", request.getRequestURI(), errors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, Map.of("errors", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Malformed request body at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", request, null);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "expected type";
        String error = String.format("Parameter '%s' should be of type '%s'", ex.getName(), expectedType);
        log.warn("Type mismatch for {}: {} (Value: '{}')", request.getRequestURI(), error, ex.getValue());
        return build(HttpStatus.BAD_REQUEST, "Type mismatch", request, Map.of("error", error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access denied", request, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus resolvedStatus = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getReason() != null && !ex.getReason().isBlank()
                ? ex.getReason()
                : resolvedStatus.getReasonPhrase();
        if (resolvedStatus.is5xxServerError()) {
            log.error("Response status exception at {}: {}", request.getRequestURI(), message, ex);
        } else {
            log.warn("Response status exception at {}: {}", request.getRequestURI(), message);
        }
        return build(resolvedStatus, message, request, null);
    }

    @ExceptionHandler({ NoSuchElementException.class, UsernameNotFoundException.class })
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.error("IllegalStateException at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadData(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Bad request at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(com.smartcbwtf.service.TenantAssertionService.TenantAccessDeniedException.class)
    public ResponseEntity<ApiError> handleTenantAccessDenied(
            com.smartcbwtf.service.TenantAssertionService.TenantAccessDeniedException ex, HttpServletRequest request) {
        Map<String, Object> details = Map.of("error", "TENANT_ACCESS_DENIED");
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request, details);
    }

    @ExceptionHandler(com.smartcbwtf.service.FacilitySettingsService.SettingsLockedException.class)
    public ResponseEntity<ApiError> handleSettingsLocked(
            com.smartcbwtf.service.FacilitySettingsService.SettingsLockedException ex, HttpServletRequest request) {
        Map<String, Object> details = Map.of("error", "SETTINGS_LOCKED");
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, details);
    }

    @ExceptionHandler(com.smartcbwtf.exception.AgreementBlockedException.class)
    public ResponseEntity<ApiError> handleAgreementBlocked(
            com.smartcbwtf.exception.AgreementBlockedException ex, HttpServletRequest request) {
        Map<String, Object> details = new java.util.HashMap<>();
        details.put("error", "AGREEMENT_BLOCKED");
        details.put("reason", ex.getReason().name());
        if (ex.getBlockingAgreementId() != null) {
            details.put("blockingAgreementId", ex.getBlockingAgreementId().toString());
        }
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, details);
    }

    @ExceptionHandler(com.smartcbwtf.exception.AgreementNotActiveException.class)
    public ResponseEntity<ApiError> handleAgreementNotActive(
            com.smartcbwtf.exception.AgreementNotActiveException ex, HttpServletRequest request) {
        Map<String, Object> details = new java.util.HashMap<>();
        details.put("error", "AGREEMENT_NOT_ACTIVE");
        if (ex.getCurrentStatus() != null) {
            details.put("currentStatus", ex.getCurrentStatus().name());
        }
        if (ex.getAgreementId() != null) {
            details.put("agreementId", ex.getAgreementId().toString());
        }
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, details);
    }

    @ExceptionHandler(com.smartcbwtf.exception.IllegalTransitionException.class)
    public ResponseEntity<ApiError> handleIllegalTransition(
            com.smartcbwtf.exception.IllegalTransitionException ex, HttpServletRequest request) {
        Map<String, Object> details = Map.of(
                "error", "ILLEGAL_TRANSITION",
                "from", ex.getFrom().name(),
                "to", ex.getTo().name());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, details);
    }

    @ExceptionHandler(com.smartcbwtf.exception.FeatureDisabledException.class)
    public ResponseEntity<ApiError> handleFeatureDisabled(
            com.smartcbwtf.exception.FeatureDisabledException ex, HttpServletRequest request) {
        Map<String, Object> details = Map.of(
                "error", "FEATURE_DISABLED",
                "feature", ex.getFeatureKey(),
                "message", "This feature is not enabled for your CBWTF");
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request, details);
    }

    /**
     * Handles duplicate HCF registration attempts.
     * Returns CONFLICT (409) status with details about which field triggered the
     * detection
     * and which existing HCF conflicts.
     */
    @ExceptionHandler(com.smartcbwtf.exception.DuplicateHcfException.class)
    public ResponseEntity<ApiError> handleDuplicateHcf(
            com.smartcbwtf.exception.DuplicateHcfException ex, HttpServletRequest request) {
        Map<String, Object> details = new java.util.HashMap<>();
        details.put("error", "DUPLICATE_HCF");
        details.put("duplicateField", ex.getDuplicateField());
        if (ex.getExistingHcfCode() != null) {
            details.put("existingHcfCode", ex.getExistingHcfCode());
        }
        if (ex.getDistanceMeters() != null) {
            details.put("distanceMeters", ex.getDistanceMeters());
        }

        log.warn("Duplicate HCF registration blocked: field={}, existingHcf={}, distance={}",
                ex.getDuplicateField(), ex.getExistingHcfCode(), ex.getDistanceMeters());

        return build(HttpStatus.CONFLICT, ex.getMessage(), request, details);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(
            org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication failed", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleInternal(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request,
            Map<String, Object> details) {
        ApiError apiError = new ApiError(
                status.value(),
                message,
                OffsetDateTime.now(),
                request.getRequestURI(),
                details);
        return ResponseEntity.status(status).body(apiError);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private String formatResolvableError(MessageSourceResolvable error) {
        String message = error.getDefaultMessage();
        return message != null ? message : error.toString();
    }

    public static class ApiError {
        private final int code;
        private final String message;
        private final OffsetDateTime timestamp;
        private final String path;
        private final Map<String, Object> details;

        public ApiError(int code, String message, OffsetDateTime timestamp, String path, Map<String, Object> details) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
            this.path = path;
            this.details = details;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        public String getPath() {
            return path;
        }

        public Map<String, Object> getDetails() {
            return details;
        }
    }
}
