package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.SystemError;
import com.smartcbwtf.repository.SystemErrorRepository;
import com.smartcbwtf.service.SystemErrorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for error reporting and management.
 * - Any authenticated user can report errors
 * - SuperAdmin can view and manage all errors
 */
@RestController
@RequestMapping("/api")
public class SystemErrorController {

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
    public ResponseEntity<ErrorReportResponse> reportError(@Valid @RequestBody ReportErrorRequest request) {
        TenantContext.TenantInfo info = TenantContext.get();
        UUID reportedById = info != null ? info.userId() : null;
        UUID facilityId = info != null ? info.tenantId() : null;

        SystemError error = errorService.reportError(
                request.title(),
                request.description(),
                request.component(),
                request.severity(),
                reportedById,
                facilityId,
                request.hcfId());

        return ResponseEntity.ok(new ErrorReportResponse(
                error.getId(),
                "Error reported successfully. Our team will investigate.",
                error.getStatus()));
    }

    /**
     * Report an error from mobile app (with stack trace).
     */
    @PostMapping("/errors/mobile")
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

        Pageable pageable = PageRequest.of(page, size);
        Page<SystemError> errors;

        if (search != null && !search.isBlank()) {
            errors = errorRepository.searchByTitleOrDescription(search, pageable);
        } else if (status != null && severity != null) {
            errors = errorRepository.findByStatusAndSeverityOrderByCreatedAtDesc(status, severity, pageable);
        } else if (status != null) {
            errors = errorRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (severity != null) {
            errors = errorRepository.findBySeverityOrderByCreatedAtDesc(severity, pageable);
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
            @RequestBody UpdateStatusRequest request) {
        SystemError error = errorService.updateStatus(id, request.status());
        return ResponseEntity.ok(toDTO(error));
    }

    /**
     * Resolve an error (SuperAdmin only).
     */
    @PutMapping("/admin/errors/{id}/resolve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SystemErrorDTO> resolveError(
            @PathVariable UUID id,
            @RequestBody ResolveErrorRequest request) {
        TenantContext.TenantInfo info = TenantContext.get();
        UUID resolvedById = info != null ? info.userId() : null;

        SystemError error = errorService.resolveError(id, resolvedById, request.notes());
        return ResponseEntity.ok(toDTO(error));
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
            @NotBlank String title,
            String description,
            String component,
            String severity,
            UUID hcfId) {
    }

    public record MobileErrorRequest(
            @NotBlank String title,
            String stackTrace,
            String deviceInfo) {
    }

    public record UpdateStatusRequest(String status) {
    }

    public record ResolveErrorRequest(String notes) {
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
