package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Bill;
import com.smartcbwtf.domain.BillVersion;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Invoice;
import com.smartcbwtf.dto.BillAdjustmentRequest;
import com.smartcbwtf.repository.BillRepository;
import com.smartcbwtf.repository.BillVersionRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import com.smartcbwtf.service.AuditLogService;
import com.smartcbwtf.service.BillAdjustmentService;
import com.smartcbwtf.service.BillGenerationService;
import com.smartcbwtf.service.BillPdfService;
import com.smartcbwtf.service.InvoicePdfService;
import com.smartcbwtf.service.TallyExportService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Billing Controller for CBWTF Admin Portal.
 * ACCESS: CBWTF_ADMIN only
 * All operations are READ-ONLY (no edit/delete capabilities).
 */
@RestController
@RequestMapping("/api/cbwtf/billing")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);

    private final BillRepository billRepository;
    private final InvoiceRepository invoiceRepository;
    private final FacilityRepository facilityRepository;
    private final BillGenerationService billGenerationService;
    private final AuditLogService auditLogService;
    private final InvoicePdfService invoicePdfService;
    private final BillAdjustmentService billAdjustmentService;
    private final BillPdfService billPdfService;
    private final TallyExportService tallyExportService;
    private final BillVersionRepository billVersionRepository;

    public BillingController(
            BillRepository billRepository,
            InvoiceRepository invoiceRepository,
            FacilityRepository facilityRepository,
            BillGenerationService billGenerationService,
            AuditLogService auditLogService,
            InvoicePdfService invoicePdfService,
            BillAdjustmentService billAdjustmentService,
            BillPdfService billPdfService,
            TallyExportService tallyExportService,
            BillVersionRepository billVersionRepository) {
        this.billRepository = billRepository;
        this.invoiceRepository = invoiceRepository;
        this.facilityRepository = facilityRepository;
        this.billGenerationService = billGenerationService;
        this.auditLogService = auditLogService;
        this.invoicePdfService = invoicePdfService;
        this.billAdjustmentService = billAdjustmentService;
        this.billPdfService = billPdfService;
        this.tallyExportService = tallyExportService;
        this.billVersionRepository = billVersionRepository;
    }

    @GetMapping("/bills")
    public ResponseEntity<Page<BillSummaryDTO>> listBills(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        UUID facilityId = TenantContext.getTenantId();
        Page<Bill> bills = billRepository.findByFacilityId(
                facilityId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "billingMonth")));
        return ResponseEntity.ok(bills.map(BillSummaryDTO::from));
    }

    @GetMapping("/bills/{id}")
    public ResponseEntity<BillDetailDTO> getBill(@PathVariable UUID id) {
        return billRepository.findById(id)
                .filter(b -> b.getFacility().getId().equals(TenantContext.getTenantId()))
                .map(BillDetailDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ============= NEW: Bill Adjustment Endpoints =============

    /**
     * Apply adjustment (concession) to a finalized bill.
     * Only CBWTF_ADMIN can apply adjustments.
     */
    @PostMapping("/bills/{id}/adjust")
    public ResponseEntity<?> applyAdjustment(
            @PathVariable UUID id,
            @Valid @RequestBody BillAdjustmentRequest request) {
        UUID userId = TenantContext.getUserId();

        // Verify bill belongs to this facility
        Bill bill = billRepository.findById(id)
                .filter(b -> b.getFacility().getId().equals(TenantContext.getTenantId()))
                .orElse(null);
        if (bill == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Bill adjustedBill = billAdjustmentService.applyAdjustment(
                    id, request.adjustmentAmount(), request.reason(), userId);

            log.info("Bill {} adjusted by user {}: amount={}", id, userId, request.adjustmentAmount());
            return ResponseEntity.ok(BillDetailDTO.from(adjustedBill));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get version history for a bill (audit trail).
     */
    @GetMapping("/bills/{id}/versions")
    public ResponseEntity<List<BillVersionDTO>> getBillVersions(@PathVariable UUID id) {
        // Verify bill belongs to this facility
        Bill bill = billRepository.findById(id)
                .filter(b -> b.getFacility().getId().equals(TenantContext.getTenantId()))
                .orElse(null);
        if (bill == null) {
            return ResponseEntity.notFound().build();
        }

        List<BillVersion> versions = billVersionRepository.findByBillIdOrderByVersionDesc(id);
        return ResponseEntity.ok(versions.stream().map(BillVersionDTO::from).toList());
    }

    /**
     * Download operational bill PDF (NOT invoice).
     */
    @GetMapping("/bills/{id}/pdf")
    public ResponseEntity<byte[]> downloadBillPdf(@PathVariable UUID id) {
        Bill bill = billRepository.findById(id)
                .filter(b -> b.getFacility().getId().equals(TenantContext.getTenantId()))
                .orElse(null);
        if (bill == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] pdf = billPdfService.generatePdf(id);
            String hcfName = bill.getAgreement() != null && bill.getAgreement().getHcf() != null
                    ? bill.getAgreement().getHcf().getName().replaceAll("[^a-zA-Z0-9]", "_")
                    : "HCF";
            String filename = String.format("bill_%s_%s.pdf", hcfName, bill.getBillingMonth());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate bill PDF for bill {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export bills for Tally (Excel format).
     */
    @GetMapping("/bills/export/tally")
    public ResponseEntity<byte[]> exportForTally(
            @RequestParam int year,
            @RequestParam int month) {
        UUID facilityId = TenantContext.getTenantId();
        YearMonth yearMonth = YearMonth.of(year, month);

        try {
            byte[] excelBytes = tallyExportService.exportBillsForMonth(facilityId, yearMonth);
            String filename = String.format("tally_export_%s_%02d.xlsx", year, month);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Failed to generate Tally export for {}/{}: {}", year, month, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/bills/month/{year}/{month}")
    public ResponseEntity<List<BillSummaryDTO>> getBillsForMonth(
            @PathVariable int year, @PathVariable int month) {
        UUID facilityId = TenantContext.getTenantId();
        LocalDate monthStart = LocalDate.of(year, month, 1);
        List<Bill> bills = billRepository.findByFacilityAndMonth(facilityId, monthStart);
        return ResponseEntity.ok(bills.stream().map(BillSummaryDTO::from).toList());
    }

    @GetMapping("/bills/{billId}/invoice")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable UUID billId) {
        return invoiceRepository.findByBillId(billId)
                .filter(i -> i.getFacility().getId().equals(TenantContext.getTenantId()))
                .map(InvoiceDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/bills/{billId}/invoice/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable UUID billId) {
        // Verify access
        Bill bill = billRepository.findById(billId)
                .filter(b -> b.getFacility().getId().equals(TenantContext.getTenantId()))
                .orElse(null);
        if (bill == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] pdf = invoicePdfService.generatePdf(billId);
            Invoice invoice = invoiceRepository.findByBillId(billId).orElse(null);
            String filename = "invoice_" + (invoice != null ? invoice.getInvoiceNumber().replace("/", "_") : billId)
                    + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for bill {}: {}", billId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ============= Invoice List API =============

    @GetMapping("/invoices")
    public ResponseEntity<Page<InvoiceSummaryDTO>> listInvoices(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        UUID facilityId = TenantContext.getTenantId();
        Page<Invoice> invoices = invoiceRepository.findByFacilityId(
                facilityId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "invoiceDate")));
        return ResponseEntity.ok(invoices.map(InvoiceSummaryDTO::from));
    }

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<InvoiceDetailDTO> getInvoiceDetail(@PathVariable UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .filter(i -> i.getFacility().getId().equals(TenantContext.getTenantId()))
                .map(i -> {
                    Bill bill = i.getBill();
                    String hcfName = bill != null && bill.getAgreement() != null && bill.getAgreement().getHcf() != null
                            ? bill.getAgreement().getHcf().getName()
                            : "Unknown";
                    LocalDate billingMonth = bill != null ? bill.getBillingMonth() : null;
                    UUID billId = bill != null ? bill.getId() : null;
                    return new InvoiceDetailDTO(
                            i.getId(), i.getInvoiceNumber(), i.getInvoiceDate(), i.getFinancialYear(),
                            hcfName, billingMonth, i.getTotalAmount(), i.getPdfUrl(), i.getIntegrityHash(), billId);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/invoices/{invoiceId}/pdf")
    public ResponseEntity<byte[]> downloadInvoiceById(@PathVariable UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .filter(i -> i.getFacility().getId().equals(TenantContext.getTenantId()))
                .orElse(null);
        if (invoice == null || invoice.getBill() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] pdf = invoicePdfService.generatePdf(invoice.getBill().getId());
            String filename = "invoice_" + invoice.getInvoiceNumber().replace("/", "_") + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice {}: {}", invoiceId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> triggerBillGeneration(@RequestBody GenerateBillsRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();
        try {
            int count = billGenerationService.generateBillsForMonth(facilityId, request.billingMonth(), userId);
            log.info("Manual bill generation: {} bills for facility {}", count, facilityId);
            return ResponseEntity.ok(Map.of("billsGenerated", count));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/config")
    public ResponseEntity<BillingConfigDTO> getBillingConfig() {
        UUID facilityId = TenantContext.getTenantId();
        return facilityRepository.findById(facilityId)
                .map(f -> new BillingConfigDTO(f.getExcessRatePerKg(), f.getExcessRateEffectiveFrom()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/config/excess-rate")
    public ResponseEntity<?> updateExcessRate(@RequestBody UpdateExcessRateRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        UUID userId = TenantContext.getUserId();

        if (request.effectiveFrom().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Effective date cannot be in the past"));
        }
        if (request.ratePerKg() == null || request.ratePerKg().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rate must be positive"));
        }

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));

        BigDecimal oldRate = facility.getExcessRatePerKg();
        LocalDate oldEffective = facility.getExcessRateEffectiveFrom();

        facility.setExcessRatePerKg(request.ratePerKg());
        facility.setExcessRateEffectiveFrom(request.effectiveFrom());
        facilityRepository.save(facility);

        auditLogService.log("FACILITY", facilityId, "EXCESS_RATE_CHANGED", userId,
                String.format("{\"oldRate\":%s,\"newRate\":%s,\"oldEffective\":\"%s\",\"newEffective\":\"%s\"}",
                        oldRate, request.ratePerKg(), oldEffective, request.effectiveFrom()));

        log.info("Excess rate updated for facility {}: {} -> {} effective from {}",
                facilityId, oldRate, request.ratePerKg(), request.effectiveFrom());

        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/config/excess-rate/history")
    public ResponseEntity<List<ExcessRateHistoryDTO>> getExcessRateHistory() {
        return ResponseEntity.ok(List.of());
    }

    // DTOs
    public record BillSummaryDTO(UUID id, String hcfName, LocalDate billingMonth,
            BigDecimal totalAmount, String status, String invoiceNumber) {
        public static BillSummaryDTO from(Bill b) {
            String hcfName = b.getAgreement() != null && b.getAgreement().getHcf() != null
                    ? b.getAgreement().getHcf().getName()
                    : "Unknown";
            return new BillSummaryDTO(b.getId(), hcfName, b.getBillingMonth(),
                    b.getTotalAmount(), b.getStatus(), null);
        }
    }

    public record BillDetailDTO(
            // Bill Identity
            UUID id,
            LocalDate billingMonth,
            String status,

            // HCF & Agreement Info
            String hcfName,
            String agreementCode,
            int agreementVersion,

            // Pickup Snapshot
            int pickupEventCount,
            BigDecimal pickupWeightKg,
            String pickupEventHash,

            // Rate Snapshot (Frozen at billing time)
            int bedCount,
            BigDecimal baseGramsPerBedPerDay,
            BigDecimal baseRatePerBedPerDay,
            BigDecimal excessRatePerKg,
            LocalDate excessRateEffectiveFrom,

            // Calculation Breakdown
            BigDecimal baseAllowanceKg,
            BigDecimal excessWeightKg,
            BigDecimal baseAmount,
            BigDecimal excessAmount,
            BigDecimal subtotal,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal totalAmount,

            // Invoice reference (if exists)
            String invoiceNumber) {
        public static BillDetailDTO from(Bill b) {
            String hcfName = b.getAgreement() != null && b.getAgreement().getHcf() != null
                    ? b.getAgreement().getHcf().getName()
                    : "Unknown";
            String agreementCode = b.getAgreement() != null
                    ? b.getAgreement().getAgreementNumber()
                    : "N/A";

            var snapshot = b.getSnapshot();
            int beds = snapshot != null ? snapshot.getBedCount() : 0;
            BigDecimal baseGrams = snapshot != null ? snapshot.getBaseGramsPerBedPerDay() : BigDecimal.ZERO;
            BigDecimal baseRate = snapshot != null ? snapshot.getBaseRatePerBedPerDay() : BigDecimal.ZERO;
            BigDecimal excessRate = snapshot != null ? snapshot.getExcessRatePerKg() : BigDecimal.ZERO;
            LocalDate excessEffective = snapshot != null ? snapshot.getExcessRateEffectiveFrom() : null;
            int agreementVer = snapshot != null ? snapshot.getAgreementVersion() : 1;

            return new BillDetailDTO(
                    b.getId(),
                    b.getBillingMonth(),
                    b.getStatus(),
                    hcfName,
                    agreementCode,
                    agreementVer,
                    b.getPickupEventCount(),
                    b.getPickupWeightKg(),
                    b.getPickupEventHash(),
                    beds,
                    baseGrams,
                    baseRate,
                    excessRate,
                    excessEffective,
                    b.getBaseAllowanceKg(),
                    b.getExcessWeightKg(),
                    b.getBaseAmount(),
                    b.getExcessAmount(),
                    b.getSubtotal(),
                    b.getCgst(),
                    b.getSgst(),
                    b.getTotalAmount(),
                    null // TODO: Fetch invoice number if exists
            );
        }
    }

    public record InvoiceDTO(UUID id, String invoiceNumber, LocalDate invoiceDate,
            String financialYear, BigDecimal totalAmount, String pdfUrl, String integrityHash) {
        public static InvoiceDTO from(Invoice i) {
            return new InvoiceDTO(i.getId(), i.getInvoiceNumber(), i.getInvoiceDate(),
                    i.getFinancialYear(), i.getTotalAmount(), i.getPdfUrl(), i.getIntegrityHash());
        }
    }

    // Invoice Summary for list view
    public record InvoiceSummaryDTO(
            UUID id,
            String invoiceNumber,
            String hcfName,
            LocalDate billingMonth,
            LocalDate invoiceDate,
            BigDecimal totalAmount) {
        public static InvoiceSummaryDTO from(Invoice i) {
            Bill bill = i.getBill();
            String hcfName = bill != null && bill.getAgreement() != null && bill.getAgreement().getHcf() != null
                    ? bill.getAgreement().getHcf().getName()
                    : "Unknown";
            LocalDate billingMonth = bill != null ? bill.getBillingMonth() : null;
            return new InvoiceSummaryDTO(
                    i.getId(), i.getInvoiceNumber(), hcfName, billingMonth, i.getInvoiceDate(), i.getTotalAmount());
        }
    }

    // Invoice Detail with bill reference
    public record InvoiceDetailDTO(
            UUID id,
            String invoiceNumber,
            LocalDate invoiceDate,
            String financialYear,
            String hcfName,
            LocalDate billingMonth,
            BigDecimal totalAmount,
            String pdfUrl,
            String integrityHash,
            UUID billId) {
    }

    public record GenerateBillsRequest(LocalDate billingMonth) {
    }

    public record BillingConfigDTO(BigDecimal excessRatePerKg, LocalDate excessRateEffectiveFrom) {
    }

    public record UpdateExcessRateRequest(BigDecimal ratePerKg, LocalDate effectiveFrom) {
    }

    public record ExcessRateHistoryDTO(BigDecimal ratePerKg, LocalDate effectiveFrom, String changedAt,
            String changedBy) {
    }

    // Bill Version DTO for audit trail
    public record BillVersionDTO(
            UUID id,
            Integer version,
            BigDecimal originalTotal,
            BigDecimal adjustmentAmount,
            BigDecimal finalAmount,
            String adjustmentReason,
            UUID adjustedBy,
            Instant adjustedAt) {
        public static BillVersionDTO from(BillVersion v) {
            return new BillVersionDTO(
                    v.getId(),
                    v.getVersion(),
                    v.getOriginalTotal(),
                    v.getAdjustmentAmount(),
                    v.getFinalAmount(),
                    v.getAdjustmentReason(),
                    v.getAdjustedBy(),
                    v.getAdjustedAt());
        }
    }
}
