package com.smartcbwtf.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, Map.of("errors", errors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access denied", request, null);
    }

    @ExceptionHandler({NoSuchElementException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadData(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication failed", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleInternal(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request, Map<String, Object> details) {
        ApiError apiError = new ApiError(
                status.value(),
                message,
                OffsetDateTime.now(),
                request.getRequestURI(),
                details
        );
        return ResponseEntity.status(status).body(apiError);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
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

        public int getCode() { return code; }
        public String getMessage() { return message; }
        public OffsetDateTime getTimestamp() { return timestamp; }
        public String getPath() { return path; }
        public Map<String, Object> getDetails() { return details; }
    }
}
