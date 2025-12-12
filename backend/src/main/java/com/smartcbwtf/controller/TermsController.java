package com.smartcbwtf.controller;

import com.smartcbwtf.domain.FacilityTerms;
import com.smartcbwtf.dto.TermsCreateRequest;
import com.smartcbwtf.dto.TermsListItem;
import com.smartcbwtf.dto.TermsResponse;
import com.smartcbwtf.service.FacilityTermsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for Terms & Conditions management.
 */
@RestController
@RequestMapping("/api")
public class TermsController {

    private final FacilityTermsService termsService;

    public TermsController(FacilityTermsService termsService) {
        this.termsService = termsService;
    }

    /**
     * Get the latest active Terms for a facility.
     * Returns fallback default if no facility-specific terms.
     */
    @GetMapping("/terms/latest")
    public ResponseEntity<TermsResponse> getLatestTerms(
            @RequestParam(name = "facilityId", required = false) UUID facilityId) {
        
        // Service handles null facilityId by returning global default
        return termsService.getLatestTerms(facilityId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List all terms versions for a facility.
     */
    @GetMapping("/facilities/{facilityId}/terms")
    public ResponseEntity<List<TermsListItem>> listTerms(@PathVariable UUID facilityId) {
        return ResponseEntity.ok(termsService.listTerms(facilityId));
    }

    /**
     * Create a new terms version for a facility.
     */
    @PostMapping("/facilities/{facilityId}/terms")
    public ResponseEntity<TermsResponse> createTerms(
            @PathVariable UUID facilityId,
            @Valid @RequestBody TermsCreateRequest request) {
        
        FacilityTerms terms = termsService.createTerms(facilityId, request);
        TermsResponse response = new TermsResponse(
                terms.getId(),
                terms.getFacility().getId(),
                terms.getFacility().getName(),
                terms.getVersion(),
                terms.getEffectiveFrom(),
                terms.getTextHtml(),
                Boolean.TRUE.equals(terms.getActive())
        );
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Activate a specific terms version.
     */
    @PatchMapping("/facilities/{facilityId}/terms/{termsId}/activate")
    public ResponseEntity<TermsResponse> activateTerms(
            @PathVariable UUID facilityId,
            @PathVariable UUID termsId) {
        
        FacilityTerms terms = termsService.activateTerms(facilityId, termsId);
        TermsResponse response = new TermsResponse(
                terms.getId(),
                terms.getFacility().getId(),
                terms.getFacility().getName(),
                terms.getVersion(),
                terms.getEffectiveFrom(),
                terms.getTextHtml(),
                Boolean.TRUE.equals(terms.getActive())
        );
        return ResponseEntity.ok(response);
    }
}
