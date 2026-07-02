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
        <li>All payments shall be made in advance for the month. In case payments are not received within the month, service may be suspended and the HCF shall be responsible for non-compliance of BMW rules and regulations. Service shall resume only after payment, and charges shall apply from the date of stoppage/resumption as applicable.</li>
        <li>No cash payment shall be made to any company staff or to any personal account. If any such payment is made, the HCF shall be solely responsible for that transaction. Payment shall be made only in the company account by cheque, NEFT, RTGS, IMPS, UPI, or other approved online mode.</li>
        <li>Cheque bouncing charges of Rs. 500 shall be payable for each dishonoured cheque.</li>
        <li>GST on BMW services shall be charged extra as per applicable government rules.</li>
        <li>HCFs whose monthly billing charges are Rs. 2,500 or less may be billed quarterly and must pay within 7 days of billing.</li>
        <li>If the HCF wants to stop the service or switch to another service provider, all outstanding dues must be cleared first. Otherwise, dues may be recovered by legal action. All disputes shall be subject to Rudrapur jurisdiction.</li>
        </ol>
        
        <h4>2. RESPONSIBILITIES OF THE SERVICE PROVIDER</h4>
        <ol>
        <li>The Service Provider shall comply with the provisions stipulated in Schedule-I of the Bio-medical Waste Management Rules, 2016, as amended from time to time.</li>
        <li>The Service Provider shall collect segregated bio-medical waste from one designated waste collection point within the premises of the Waste Generator on alternate days, except Sundays and national/government holidays.</li>
        <li>The Service Provider shall schedule the timings for collecting waste in consultation with the Waste Generator.</li>
        <li>The Service Provider shall transport segregated waste in a closed container vehicle to its treatment facility.</li>
        <li>The Service Provider shall not be held liable for violations made by the Waste Generator or its staff under the Bio-medical Waste Management Rules, 2016, as amended from time to time.</li>
        </ol>
        
        <h4>3. RESPONSIBILITIES OF THE WASTE GENERATOR</h4>
        <ol>
        <li>The Waste Generator shall segregate bio-medical waste at the point of generation in accordance with the Bio-medical Waste Management Rules, 2016, as amended from time to time.</li>
        <li>The Waste Generator shall collect, pack, label, and hand over segregated BMW in non-chlorinated bags as stipulated under the Bio-medical Waste Management Rules, 2016, as amended from time to time, arranged by the Waste Generator at its own cost.</li>
        <li>The Waste Generator shall keep BMW under lock and key to protect it from mishandling before handover to the authorised person of the Service Provider.</li>
        <li>The Waste Generator shall disinfect and mutilate sharps and hand them over in sealed puncture-proof containers to the Service Provider.</li>
        <li>The Waste Generator shall take all necessary steps to ensure waste is handled without causing adverse effect to human health and environment.</li>
        <li>The Waste Generator shall establish a common secured waste collection point within its premises for collection and storage of BMW before handover to the Service Provider.</li>
        <li>The Waste Generator shall designate a Nodal Officer to interact with the Service Provider.</li>
        <li>The Waste Generator shall apply for and obtain necessary authorisation from the Prescribed Authority under the Bio-medical Waste Management Rules, 2016, as amended from time to time, or submit necessary returns to the Prescribed Authority from time to time as laid down in the said Rules.</li>
        </ol>
        
        <h4>DECLARATION</h4>
        <p>I/We have read and understood the contents of this agreement and agree that both parties shall remain bound by the terms and conditions stated herein.</p>
        """;

    /**
     * Get the latest active Terms for a facility.
     * Returns fallback default if no facility-specific terms exist in database.
     */
    @GetMapping("/terms/latest")
    public ResponseEntity<TermsResponse> getLatestTerms(
            @RequestParam(name = "facilityId", required = false) UUID facilityId) {
        
        // Service returns only facility-specific terms; fallback is the built-in
        // default so one facility's custom terms never leak to another.
        return termsService.getLatestTerms(facilityId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
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
