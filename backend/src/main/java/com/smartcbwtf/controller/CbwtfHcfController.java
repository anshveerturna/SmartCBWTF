package com.smartcbwtf.controller;

import com.smartcbwtf.dto.*;
import com.smartcbwtf.service.CbwtfHcfService;
import com.smartcbwtf.service.BillingConfigService;
import com.smartcbwtf.config.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for CBWTF Admin HCF Management.
 * 
 * All endpoints are tenant-scoped via TenantContext.
 * facility_id is NEVER accepted from request body.
 */
@RestController
@RequestMapping("/api/cbwtf/hcfs")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfHcfController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CbwtfHcfController.class);

    private final CbwtfHcfService hcfService;
    private final BillingConfigService billingConfigService;

    public CbwtfHcfController(CbwtfHcfService hcfService, BillingConfigService billingConfigService) {
        this.hcfService = hcfService;
        this.billingConfigService = billingConfigService;
    }

    /**
     * List all HCFs with active agreements for the current CBWTF.
     */
    @GetMapping
    public ResponseEntity<List<HcfListItemDTO>> listHcfs() {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.listByFacility(facilityId));
    }

    /**
     * Get HCF detail with agreement, billing config, and summary.
     */
    @GetMapping("/{id}")
    public ResponseEntity<HcfDetailDTO> getHcfDetail(@PathVariable("id") UUID id) {
        log.info("Controller request: getHcfDetail for ID: {}", id);
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.getHcfDetail(id, facilityId));
    }

    /**
     * Update HCF profile (name, email, phone, address).
     * Audit logged.
     */
    @PutMapping("/{id}")
    public ResponseEntity<HcfDetailDTO> updateHcf(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateHcfRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.updateHcf(id, facilityId, request));
    }

    /**
     * Update HCF registered location.
     * Only allowed if agreement is ACTIVE.
     * Audit logged.
     */
    @PutMapping("/{id}/location")
    public ResponseEntity<HcfDetailDTO> updateLocation(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateLocationRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.updateLocation(id, facilityId, request));
    }

    /**
     * Deactivate HCF by expiring/terminating its agreement.
     * Audit logged.
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateHcf(
            @PathVariable("id") UUID id,
            @Valid @RequestBody DeactivateHcfRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        hcfService.deactivate(id, facilityId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current billing configuration for HCF's agreement.
     */
    @GetMapping("/{id}/billing")
    public ResponseEntity<HcfDetailDTO.BillingConfigInfo> getBillingConfig(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(billingConfigService.getCurrentConfig(id, facilityId));
    }

    /**
     * Update billing configuration (creates new, expires previous).
     * Only allowed if agreement is ACTIVE.
     * Audit logged.
     */
    @PutMapping("/{id}/billing")
    public ResponseEntity<HcfDetailDTO.BillingConfigInfo> updateBillingConfig(
            @PathVariable("id") UUID id,
            @Valid @RequestBody BillingConfigRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(billingConfigService.createConfig(id, facilityId, request));
    }

    /**
     * List HCFs pending approval (registered via Android app).
     */
    @GetMapping("/pending")
    public ResponseEntity<List<HcfListItemDTO>> listPendingHcfs() {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.listPending(facilityId));
    }

    /**
     * Approve a pending HCF registration.
     * Creates agreement + default billing config.
     * Audit logged.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<HcfDetailDTO> approveHcf(
            @PathVariable("id") UUID id,
            @Valid @RequestBody HcfApprovalRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.approveHcf(id, facilityId, request));
    }

    /**
     * Reject a pending HCF registration.
     * Audit logged with reason.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectHcf(
            @PathVariable("id") UUID id,
            @RequestBody HcfRejectionRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        hcfService.rejectHcf(id, facilityId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Renew an expired agreement by creating a NEW agreement.
     * Old agreement remains immutable.
     */
    @PostMapping("/{id}/agreements/renew")
    public ResponseEntity<HcfDetailDTO> renewAgreement(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RenewAgreementRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(hcfService.renewAgreement(id, facilityId, request));
    }
}
