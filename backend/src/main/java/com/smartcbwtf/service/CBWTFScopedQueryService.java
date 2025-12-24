package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CBWTF-scoped query service.
 * All queries are automatically filtered by the current tenant's facility_id.
 * 
 * This is the ONLY way CBWTF Admin Portal should access tenant data.
 */
@Service
public class CBWTFScopedQueryService {

    private static final Logger log = LoggerFactory.getLogger(CBWTFScopedQueryService.class);

    private final TenantAssertionService tenantAssertion;
    private final AgreementRepository agreementRepo;
    private final BagLabelRepository bagLabelRepo;
    private final InvoiceRepository invoiceRepo;
    private final FacilityRepository facilityRepo;

    public CBWTFScopedQueryService(TenantAssertionService tenantAssertion,
            AgreementRepository agreementRepo,
            BagLabelRepository bagLabelRepo,
            InvoiceRepository invoiceRepo,
            FacilityRepository facilityRepo) {
        this.tenantAssertion = tenantAssertion;
        this.agreementRepo = agreementRepo;
        this.bagLabelRepo = bagLabelRepo;
        this.invoiceRepo = invoiceRepo;
        this.facilityRepo = facilityRepo;
    }

    /**
     * Get the current tenant's facility ID.
     * Throws if no tenant context is available.
     */
    public UUID getCurrentFacilityId() {
        return tenantAssertion.getRequiredTenantId();
    }

    /**
     * Get the current tenant's facility.
     */
    public Optional<Facility> getCurrentFacility() {
        UUID facilityId = getCurrentFacilityId();
        return facilityRepo.findById(facilityId);
    }

    // ============ AGREEMENT QUERIES ============

    /**
     * Get all active agreements for the current facility.
     */
    public List<Agreement> getActiveAgreements() {
        UUID facilityId = getCurrentFacilityId();
        return agreementRepo.findActiveByFacilityId(facilityId);
    }

    /**
     * Get all HCFs under the current facility's active agreements.
     */
    public List<Hcf> getActiveHcfs() {
        UUID facilityId = getCurrentFacilityId();
        return agreementRepo.findHcfsByFacilityId(facilityId);
    }

    /**
     * Count active agreements for the current facility.
     */
    public long countActiveAgreements() {
        UUID facilityId = getCurrentFacilityId();
        return agreementRepo.countActiveByFacilityId(facilityId);
    }

    /**
     * Get specific agreement, verifying it belongs to current tenant.
     */
    public Optional<Agreement> getAgreement(UUID agreementId) {
        return agreementRepo.findById(agreementId)
                .filter(agreement -> {
                    UUID facilityId = agreement.getFacility().getId();
                    try {
                        tenantAssertion.assertCanAccessFacility(facilityId);
                        return true;
                    } catch (TenantAssertionService.TenantAccessDeniedException e) {
                        log.warn("Access denied to agreement {} - wrong tenant", agreementId);
                        return false;
                    }
                });
    }

    // ============ BAG LABEL QUERIES ============

    /**
     * Get all bag labels for the current facility.
     */
    public List<BagLabel> getBagLabels() {
        UUID facilityId = getCurrentFacilityId();
        return bagLabelRepo.findByFacilityId(facilityId);
    }

    /**
     * Get bag labels with specific status for current facility.
     */
    public List<BagLabel> getBagLabelsByStatus(String status) {
        UUID facilityId = getCurrentFacilityId();
        return bagLabelRepo.findByFacilityIdAndStatus(facilityId, status);
    }

    /**
     * Get specific bag label, verifying it belongs to current tenant.
     */
    public Optional<BagLabel> getBagLabel(UUID labelId) {
        return bagLabelRepo.findById(labelId)
                .filter(label -> {
                    if (label.getFacility() == null)
                        return false;
                    UUID facilityId = label.getFacility().getId();
                    try {
                        tenantAssertion.assertCanAccessFacility(facilityId);
                        return true;
                    } catch (TenantAssertionService.TenantAccessDeniedException e) {
                        log.warn("Access denied to bag label {} - wrong tenant", labelId);
                        return false;
                    }
                });
    }

    /**
     * Count bag labels for current facility.
     */
    public long countBagLabels() {
        UUID facilityId = getCurrentFacilityId();
        return bagLabelRepo.countByFacilityId(facilityId);
    }

    /**
     * Count bag labels by status for current facility.
     */
    public long countBagLabelsByStatus(String status) {
        UUID facilityId = getCurrentFacilityId();
        return bagLabelRepo.countByFacilityIdAndStatus(facilityId, status);
    }

    // ============ INVOICE QUERIES ============

    /**
     * Get all invoices for the current facility.
     */
    public List<Invoice> getInvoices() {
        UUID facilityId = getCurrentFacilityId();
        return invoiceRepo.findByFacilityId(facilityId);
    }

    /**
     * Get invoices with specific status for current facility.
     */
    public List<Invoice> getInvoicesByStatus(String status) {
        UUID facilityId = getCurrentFacilityId();
        return invoiceRepo.findByFacilityIdAndStatus(facilityId, status);
    }

    /**
     * Get specific invoice, verifying it belongs to current tenant.
     */
    public Optional<Invoice> getInvoice(UUID invoiceId) {
        return invoiceRepo.findById(invoiceId)
                .filter(invoice -> {
                    if (invoice.getFacility() == null)
                        return false;
                    UUID facilityId = invoice.getFacility().getId();
                    try {
                        tenantAssertion.assertCanAccessFacility(facilityId);
                        return true;
                    } catch (TenantAssertionService.TenantAccessDeniedException e) {
                        log.warn("Access denied to invoice {} - wrong tenant", invoiceId);
                        return false;
                    }
                });
    }
}
