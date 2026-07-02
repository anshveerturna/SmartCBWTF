package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.SystemError;
import com.smartcbwtf.repository.SystemErrorRepository;
import com.smartcbwtf.service.SystemErrorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static com.smartcbwtf.util.PaginationUtils.pageRequest;

/**
 * REST API for error reporting and management.
 * - Any authenticated user can report errors
 * - SuperAdmin can view and manage all errors
 */
@RestController
@RequestMapping("/api")
public class SystemErrorController {
    private static final String ERROR_SEVERITY_PATTERN = "(?i)CRITICAL|ERROR|WARNING|INFO";
    private static final String ERROR_STATUS_PATTERN = "(?i)OPEN|IN_PROGRESS|RESOLVED|IGNORED";
    private static final int MAX_FILTER_LENGTH = 40;
    private static final int MAX_SEARCH_LENGTH = 120;
    private static final Set<String> ALLOWED_STATUSES = Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "IGNORED");
    private static final Set<String> ALLOWED_SEVERITIES = Set.of("CRITICAL", "ERROR", "WARNING", "INFO");

    private final SystemErrorService errorService;
    private final SystemErrorRepository errorRepository;

    public SystemErrorController(SystemErrorService errorService, SystemErrorRepository errorRepository) {
        this.errorService = errorService;
        this.errorRepository = errorRepository;
    }

    // ========== Error Reporting (Any Authenticated User) ==========

    /**
     * Report an error or issue from any authenticated user.
     */
    @PostMapping("/errors/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ErrorReportResponse> reportError(@Valid @RequestBody ReportErrorRequest request) {
        TenantContext.TenantInfo info = TenantContext.get();
        UUID reportedById = info != null ? info.userId() : null;
        UUID facilityId = info != null ? info.tenantId() : null;
        UUID hcfId = resolveReportHcfId(info, request.hcfId());

        SystemError error = errorService.reportError(
                request.title(),
                request.description(),
                request.component(),
                request.severity(),
                reportedById,
                facilityId,
                hcfId);

        return ResponseEntity.ok(new ErrorReportResponse(
                error.getId(),
                "Error reported successfully. Our team will investigate.",
                error.getStatus()));
    }

    /**
     * Report an error from mobile app (with stack trace).
     */
    @PostMapping("/errors/mobile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ErrorReportResponse> reportMobileError(@Valid @RequestBody MobileErrorRequest request) {
        TenantContext.TenantInfo info = TenantContext.get();
        UUID reportedById = info != null ? info.userId() : null;

        SystemError error = errorService.reportMobileAppError(
                request.title(),
                request.stackTrace(),
                request.deviceInfo(),
                reportedById);

        return ResponseEntity.ok(new ErrorReportResponse(
                error.getId(),
                "Error reported. Thank you for helping us improve.",
                error.getStatus()));
    }

    private UUID resolveReportHcfId(TenantContext.TenantInfo info, UUID requestedHcfId) {
        if (info == null || info.hcfId() == null) {
            return requestedHcfId;
        }
        if (requestedHcfId != null && !info.hcfId().equals(requestedHcfId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot report errors for another HCF");
        }
        return info.hcfId();
    }

    // ========== Admin Error Management (SuperAdmin Only) ==========

    /**
     * List all errors with filtering (SuperAdmin only).
     */
    @GetMapping("/admin/errors")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<SystemErrorDTO>> listErrors(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = pageRequest(page, size, 20);
        Page<SystemError> errors;
        String normalizedStatus = normalizeEnumFilter(status, "status", ALLOWED_STATUSES);
        String normalizedSeverity = normalizeEnumFilter(severity, "severity", ALLOWED_SEVERITIES);
        String normalizedSearch = normalizeSearch(search);

        if (normalizedSearch != null) {
            errors = errorRepository.searchByTitleOrDescription(normalizedSearch, pageable);
        } else if (normalizedStatus != null && normalizedSeverity != null) {
            errors = errorRepository.findByStatusAndSeverityOrderByCreatedAtDesc(normalizedStatus, normalizedSeverity,
                    pageable);
        } else if (normalizedStatus != null) {
            errors = errorRepository.findByStatusOrderByCreatedAtDesc(normalizedStatus, pageable);
        } else if (normalizedSeverity != null) {
            errors = errorRepository.findBySeverityOrderByCreatedAtDesc(normalizedSeverity, pageable);
        } else {
            errors = errorRepository.findAllOrdered(pageable);
        }

        return ResponseEntity.ok(errors.map(this::toDTO));
    }

    /**
     * Get a single error by ID (SuperAdmin only).
     */
    @GetMapping("/admin/errors/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SystemErrorDTO> getError(@PathVariable UUID id) {
        return errorRepository.findById(id)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update error status (SuperAdmin only).
     */
    @PutMapping("/admin/errors/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SystemErrorDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        SystemError error = errorService.updateStatus(id, request.status());
        return ResponseEntity.ok(toDTO(error));
    }

    /**
     * Resolve an error (SuperAdmin only).
     */
    @PutMapping("/admin/errors/{id}/resolve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SystemErrorDTO> resolveError(
            @PathVariable("id") UUID id,
            @Valid @RequestBody(required = false) ResolveErrorRequest request) {
        TenantContext.TenantInfo info = TenantContext.get();
        UUID resolvedById = info != null ? info.userId() : null;
        String notes = request != null ? request.notes() : null;

        try {
            SystemError error = errorService.resolveError(id, resolvedById, notes);
            return ResponseEntity.ok(toDTO(error));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get error statistics for dashboard (SuperAdmin only).
     */
    @GetMapping("/admin/errors/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ErrorStatsDTO> getErrorStats() {
        return ResponseEntity.ok(new ErrorStatsDTO(
                errorRepository.countOpen(),
                errorRepository.countInProgress(),
                errorRepository.countOpenCritical(),
                errorRepository.countOpenErrors(),
                errorRepository.countOpenWarnings()));
    }

    private static String normalizeSearch(String search) {
        return normalizeQueryText(search, MAX_SEARCH_LENGTH, "search");
    }

    private static String normalizeEnumFilter(String value, String label, Set<String> allowedValues) {
        String normalized = normalizeQueryText(value, MAX_FILTER_LENGTH, label);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported " + label + ": " + normalized);
        }
        return normalized;
    }

    private static String normalizeQueryText(String value, int maxLength, String label) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " must be " + maxLength + " characters or fewer");
        }
        for (int i = 0; i < normalized.length(); i++) {
            if (Character.isISOControl(normalized.charAt(i))) {
                throw new IllegalArgumentException(label + " contains unsupported control characters");
            }
        }
        return normalized;
    }

    // ========== DTOs ==========

    private SystemErrorDTO toDTO(SystemError error) {
        return new SystemErrorDTO(
                error.getId(),
                error.getErrorCode(),
                error.getSeverity(),
                error.getSource(),
                error.getComponent(),
                error.getFacility() != null ? error.getFacility().getCode() : null,
                error.getHcf() != null ? error.getHcf().getCode() : null,
                error.getReportedBy() != null ? error.getReportedBy().getName() : null,
                error.getTitle(),
                error.getDescription(),
                error.getStatus(),
                error.getResolvedBy() != null ? error.getResolvedBy().getName() : null,
                error.getResolvedAt() != null ? error.getResolvedAt().toString() : null,
                error.getResolutionNotes(),
                error.getCreatedAt().toString());
    }

    public record ReportErrorRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 5000) String description,
            @Size(max = 50) String component,
            @Pattern(regexp = ERROR_SEVERITY_PATTERN, message = "must be one of CRITICAL, ERROR, WARNING, INFO") String severity,
            UUID hcfId) {
    }

    public record MobileErrorRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 20000) String stackTrace,
            @Size(max = 2000) String deviceInfo) {
    }

    public record UpdateStatusRequest(
            @NotBlank @Pattern(regexp = ERROR_STATUS_PATTERN, message = "must be one of OPEN, IN_PROGRESS, RESOLVED, IGNORED") String status) {
    }

    public record ResolveErrorRequest(@Size(max = 4000) String notes) {
    }

    public record ErrorReportResponse(UUID id, String message, String status) {
    }

    public record SystemErrorDTO(
            UUID id,
            String errorCode,
            String severity,
            String source,
            String component,
            String cbwtfCode,
            String hcfCode,
            String reportedBy,
            String title,
            String description,
            String status,
            String resolvedBy,
            String resolvedAt,
            String resolutionNotes,
            String createdAt) {
    }

    public record ErrorStatsDTO(
            long open,
            long inProgress,
            long critical,
            long errors,
            long warnings) {
    }
}
