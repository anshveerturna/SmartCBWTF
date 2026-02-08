package com.smartcbwtf.controller;

import com.smartcbwtf.domain.FacilityTerms;
import com.smartcbwtf.dto.TermsCreateRequest;
import com.smartcbwtf.dto.TermsListItem;
import com.smartcbwtf.dto.TermsResponse;
import com.smartcbwtf.service.FacilityTermsService;
import com.smartcbwtf.service.TenantAssertionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final TenantAssertionService tenantAssertionService;

    public TermsController(FacilityTermsService termsService, TenantAssertionService tenantAssertionService) {
        this.termsService = termsService;
        this.tenantAssertionService = tenantAssertionService;
    }

    /**
     * Default Terms HTML content when no database terms exist.
     */
    private static final String DEFAULT_TERMS_HTML = """
        <h2>BIO MEDICAL WASTE COLLECTION & DISPOSAL SERVICES</h2>
        <h3>TERMS &amp; CONDITIONS</h3>
        
        <h4>1. TERMS OF PAYMENT</h4>
        <ol>
        <li>All payment will be made advance of the month. In case payments are not received within month, service will be suspended.</li>
        <li>Payment should be transfer by NEFT/RTGS/IMPS/Cheque &amp; online. (No Cash)</li>
        <li>GST on BMW Services is 5% will be charged extra as per Govt. rule.</li>
        </ol>
        
        <h4>2. RESPONSIBILITIES OF THE SERVICE PROVIDER</h4>
        <ol>
        <li>The Service Provider shall comply with provisions as stipulated in Schedule-1 of the BMW Rule 2025.</li>
        <li>The Service Provider shall collect the segregated bio-medical waste from the designated collection point.</li>
        <li>The Service Provider shall transport the segregated waste in closed container vehicle to its treatment facility.</li>
        </ol>
        
        <h4>3. RESPONSIBILITIES OF THE WASTE GENERATOR</h4>
        <ol>
        <li>The Waste Generator shall segregate the Bio-Medical waste at the point of generation in accordance with the BMW Rules 2025.</li>
        <li>The Waste Generator shall collect, pack, label and handover the segregated BMW in non-chlorinated bags.</li>
        <li>The Waste Generator shall take all necessary steps to ensure that the waste is handled without causing any adverse effect to human health and environment.</li>
        </ol>
        
        <h4>DECLARATION</h4>
        <p>I/We have read and understood the entire contents of this agreement and give my/our free consent to the terms and conditions set out herein above.</p>
        """;

    /**
     * Get the latest active Terms for a facility.
     * Returns fallback default if no facility-specific terms exist in database.
     */
    @GetMapping("/terms/latest")
    public ResponseEntity<TermsResponse> getLatestTerms(
            @RequestParam(name = "facilityId", required = false) UUID facilityId) {
        
        // Service handles null facilityId by returning global default
        return termsService.getLatestTerms(facilityId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    // Return hardcoded default when no terms exist in database
                    TermsResponse defaultTerms = new TermsResponse(
                            null,                           // id
                            null,                           // facilityId
                            null,                           // facilityName
                            "2025-01-01-default",           // version
                            java.time.LocalDate.of(2025, 1, 1), // effectiveFrom
                            DEFAULT_TERMS_HTML,             // textHtml
                            true                            // active
                    );
                    return ResponseEntity.ok(defaultTerms);
                });
    }

    /**
     * List all terms versions for a facility.
     */
    @GetMapping("/facilities/{facilityId}/terms")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CBWTF_ADMIN')")
    public ResponseEntity<List<TermsListItem>> listTerms(@PathVariable UUID facilityId) {
        tenantAssertionService.assertCanAccessFacility(facilityId);
        return ResponseEntity.ok(termsService.listTerms(facilityId));
    }

    /**
     * Create a new terms version for a facility.
     */
    @PostMapping("/facilities/{facilityId}/terms")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CBWTF_ADMIN')")
    public ResponseEntity<TermsResponse> createTerms(
            @PathVariable UUID facilityId,
            @Valid @RequestBody TermsCreateRequest request) {

        tenantAssertionService.assertCanAccessFacility(facilityId);
        request.setCreatedByUserId(com.smartcbwtf.config.TenantContext.getUserId());
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CBWTF_ADMIN')")
    public ResponseEntity<TermsResponse> activateTerms(
            @PathVariable UUID facilityId,
            @PathVariable UUID termsId) {

        tenantAssertionService.assertCanAccessFacility(facilityId);
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
