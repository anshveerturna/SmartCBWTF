package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Bill Generation Service - Orchestrates the monthly billing process.
 * 
 * Flow:
 * 1. Acquire billing lock (prevents concurrent runs)
 * 2. Fetch ACTIVE agreements
 * 3. Create billing snapshot (freeze rates)
 * 4. Aggregate pickup events
 * 5. Calculate bill via BillingCalculationService
 * 6. Persist bill with status FINALIZED
 * 7. Audit log
 * 
 * NOTE: Invoice generation is NOT performed here. GST invoices are
 * handled externally via Tally accounting software.
 */
@Service
public class BillGenerationService {

    private static final Logger log = LoggerFactory.getLogger(BillGenerationService.class);
    private static final String PICKUP_EVENT_SOURCE_SQL = """
            FROM bag_event e
            JOIN agreement a ON a.hcf_id = e.hcf_id
                AND a.facility_id = e.facility_id
            WHERE a.id = ?
                AND e.event_type = 'HCF_COLLECTION'
                AND e.event_ts >= ?
                AND e.event_ts < ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AgreementRepository agreementRepository;
    private final BillingSnapshotRepository snapshotRepository;
    private final BillRepository billRepository;
    private final InvoiceRepository invoiceRepository;
    private final FacilityRepository facilityRepository;
    private final BillingCalculationService calculationService;
    private final AuditLogService auditLogService;

    public BillGenerationService(
            JdbcTemplate jdbcTemplate,
            AgreementRepository agreementRepository,
            BillingSnapshotRepository snapshotRepository,
            BillRepository billRepository,
            InvoiceRepository invoiceRepository,
            FacilityRepository facilityRepository,
            BillingCalculationService calculationService,
            AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.agreementRepository = agreementRepository;
        this.snapshotRepository = snapshotRepository;
        this.billRepository = billRepository;
        this.invoiceRepository = invoiceRepository;
        this.facilityRepository = facilityRepository;
        this.calculationService = calculationService;
        this.auditLogService = auditLogService;
    }

    /**
     * Generate bills for a specific facility and month.
     * 
     * @param facilityId   Facility UUID
     * @param billingMonth First day of billing month
     * @param triggeredBy  User ID who triggered (null for scheduled)
     * @return Number of bills generated
     */
    @Transactional
    public int generateBillsForMonth(UUID facilityId, LocalDate billingMonth, UUID triggeredBy) {
        log.info("Starting bill generation for facility {} month {}", facilityId, billingMonth);

        // Ensure billing month is first of month
        LocalDate monthStart = billingMonth.withDayOfMonth(1);

        // Acquire lock
        if (!acquireBillingLock(facilityId, monthStart, triggeredBy)) {
            log.warn("Billing lock already held for facility {} month {}", facilityId, monthStart);
            throw new IllegalStateException("Billing already in progress for this month");
        }

        try {
            Facility facility = facilityRepository.findById(facilityId)
                    .orElseThrow(() -> new IllegalArgumentException("Facility not found"));

            // Get ACTIVE agreements for this facility
            List<Agreement> activeAgreements = agreementRepository
                    .findActiveByFacilityId(facilityId);

            int billsGenerated = 0;
            for (Agreement agreement : activeAgreements) {
                try {
                    if (generateBillForAgreement(agreement, facility, monthStart, triggeredBy)) {
                        billsGenerated++;
                    }
                } catch (Exception e) {
                    log.error("Failed to generate bill for agreement {}: {}",
                            agreement.getId(), e.getMessage(), e);
                    auditLogService.log("BILLING", agreement.getId(), "BILLING_FAILED",
                            triggeredBy, e.getMessage());
                }
            }

            log.info("Bill generation complete: {} bills for facility {}", billsGenerated, facilityId);
            return billsGenerated;

        } finally {
            releaseBillingLock(facilityId, monthStart);
        }
    }

    /**
     * Generate bill for a single agreement.
     */
    private boolean generateBillForAgreement(
            Agreement agreement,
            Facility facility,
            LocalDate monthStart,
            UUID triggeredBy) {

        // Skip if bill already exists
        if (billRepository.existsByAgreementIdAndBillingMonth(agreement.getId(), monthStart)) {
            log.debug("Bill already exists for agreement {} month {}", agreement.getId(), monthStart);
            return false;
        }

        // Get billing config for agreement
        BigDecimal baseGrams = getAgreementBillingConfig(agreement.getId(), "base_grams_per_bed_per_day");
        BigDecimal baseRate = getAgreementBillingConfig(agreement.getId(), "base_rate_per_bed_per_day");

        if (baseGrams == null || baseRate == null) {
            log.warn("No billing config for agreement {}", agreement.getId());
            return false;
        }

        int bedCount = agreement.getHcf().getNumberOfBeds() != null ? agreement.getHcf().getNumberOfBeds() : 0;
        if (bedCount == 0) {
            log.warn("Agreement {} HCF has 0 beds, skipping", agreement.getId());
            return false;
        }

        // Get days in month
        YearMonth ym = YearMonth.from(monthStart);
        int daysInMonth = ym.lengthOfMonth();

        // Get excess rate: HCF-specific > facility default > hardcoded default
        Hcf billingHcf = agreement.getHcf();
        BigDecimal excessRate;
        if (billingHcf.getExcessRatePerKg() != null) {
            excessRate = BigDecimal.valueOf(billingHcf.getExcessRatePerKg());
        } else if (facility.getExcessRatePerKg() != null) {
            excessRate = facility.getExcessRatePerKg();
        } else {
            excessRate = new BigDecimal("50.00"); // Default
        }
        LocalDate excessRateEffectiveFrom = facility.getExcessRateEffectiveFrom();
        if (excessRateEffectiveFrom == null) {
            excessRateEffectiveFrom = LocalDate.of(2020, 1, 1);
        }

        // Aggregate pickup weight for this agreement in this month
        BigDecimal pickupWeightKg = aggregatePickupWeight(agreement.getId(), monthStart, ym.atEndOfMonth());
        int pickupEventCount = countPickupEvents(agreement.getId(), monthStart, ym.atEndOfMonth());
        String pickupEventHash = computePickupEventHash(agreement.getId(), monthStart, ym.atEndOfMonth());

        // Create snapshot
        BillingSnapshot snapshot = new BillingSnapshot();
        snapshot.setAgreement(agreement);
        snapshot.setFacility(facility);
        snapshot.setBillingMonth(monthStart);
        snapshot.setBedCount(bedCount);
        snapshot.setBaseGramsPerBedPerDay(baseGrams);
        snapshot.setBaseRatePerBedPerDay(baseRate);
        snapshot.setAgreementVersion(agreement.getVersion() != null ? agreement.getVersion() : 1);
        snapshot.setExcessRatePerKg(excessRate);
        snapshot.setExcessRateEffectiveFrom(excessRateEffectiveFrom);
        snapshot.setSnapshotHash(computeSnapshotHash(snapshot));
        snapshotRepository.save(snapshot);

        // Calculate bill (use HCF's configured tax rate, or default 5%)
        BillingCalculationService.BillCalculation calc = calculationService.calculate(
                bedCount, daysInMonth, baseGrams, baseRate, excessRate, pickupWeightKg,
                agreement.getHcf().getTaxRate());

        // Create bill
        Bill bill = new Bill();
        bill.setSnapshot(snapshot);
        bill.setAgreement(agreement);
        bill.setFacility(facility);
        bill.setBillingMonth(monthStart);
        bill.setPickupWeightKg(calc.pickupWeightKg());
        bill.setPickupEventCount(pickupEventCount);
        bill.setPickupEventHash(pickupEventHash);
        bill.setBaseAllowanceKg(calc.baseAllowanceKg());
        bill.setExcessWeightKg(calc.excessWeightKg());
        bill.setBaseAmount(calc.baseAmount());
        bill.setExcessAmount(calc.excessAmount());
        bill.setSubtotal(calc.subtotal());
        bill.setCgst(calc.cgst());
        bill.setSgst(calc.sgst());
        bill.setTotalAmount(calc.totalAmount());
        // Set final payable amount (same as total for new bills, adjusted later if
        // concession applied)
        bill.setFinalPayableAmount(calc.totalAmount());
        bill.setStatus(Bill.Status.FINALIZED.name());
        billRepository.save(bill);

        // NOTE: Invoice generation removed - GST invoices are handled via Tally

        auditLogService.log("BILL", bill.getId(), "BILL_GENERATED", triggeredBy,
                String.format("{\"total\":%.2f,\"events\":%d}", calc.totalAmount(), pickupEventCount));

        log.info("Generated bill {} for agreement {} total {}",
                bill.getId(), agreement.getId(), calc.totalAmount());

        return true;
    }

    /**
     * Generate invoice for a bill.
     * 
     * @deprecated Invoice generation is now handled externally via Tally.
     *             This method is kept for reference only and will be removed.
     */
    @Deprecated(forRemoval = true)
    @SuppressWarnings("unused")
    private void generateInvoice(Bill bill, Facility facility, LocalDate monthStart) {
        String financialYear = getFinancialYear(monthStart);
        int sequence = getNextInvoiceSequence(facility.getId(), financialYear);

        String facilityCode = facility.getCode() != null ? facility.getCode() : "CBWTF";
        String invoiceNumber = String.format("%s/%s/%06d", facilityCode, financialYear, sequence);

        Invoice invoice = new Invoice();
        invoice.setBill(bill);
        invoice.setFacility(facility);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setFinancialYear(financialYear);
        invoice.setTotalAmount(bill.getTotalAmount());
        invoice.setIntegrityHash(computeInvoiceHash(bill.getId(), bill.getTotalAmount(), invoiceNumber));
        invoiceRepository.save(invoice);

        auditLogService.log("INVOICE", invoice.getId(), "INVOICE_GENERATED", null,
                String.format("{\"number\":\"%s\"}", invoiceNumber));
    }

    // ============= Helper Methods =============

    private boolean acquireBillingLock(UUID facilityId, LocalDate month, UUID lockedBy) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO billing_lock (billing_month, facility_id, locked_by) VALUES (?, ?, ?)",
                    month, facilityId, lockedBy != null ? lockedBy : UUID.randomUUID());
            auditLogService.log("BILLING_LOCK", facilityId, "BILLING_LOCK_ACQUIRED", lockedBy, month.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void releaseBillingLock(UUID facilityId, LocalDate month) {
        jdbcTemplate.update(
                "DELETE FROM billing_lock WHERE billing_month = ? AND facility_id = ?",
                month, facilityId);
        auditLogService.log("BILLING_LOCK", facilityId, "BILLING_LOCK_RELEASED", null, month.toString());
    }

    private BigDecimal getAgreementBillingConfig(UUID agreementId, String field) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT " + field + " FROM agreement_billing_config " +
                            "WHERE agreement_id = ? AND effective_to IS NULL",
                    BigDecimal.class, agreementId);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal aggregatePickupWeight(UUID agreementId, LocalDate start, LocalDate end) {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(e.weight_kg), 0) " + PICKUP_EVENT_SOURCE_SQL,
                BigDecimal.class, agreementId, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        return total != null ? total : BigDecimal.ZERO;
    }

    private int countPickupEvents(UUID agreementId, LocalDate start, LocalDate end) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(e.id) " + PICKUP_EVENT_SOURCE_SQL,
                Integer.class, agreementId, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        return count != null ? count : 0;
    }

    private String computePickupEventHash(UUID agreementId, LocalDate start, LocalDate end) {
        List<UUID> eventIds = jdbcTemplate.queryForList(
                "SELECT e.id " + PICKUP_EVENT_SOURCE_SQL + " ORDER BY e.id",
                UUID.class, agreementId, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        return sha256(eventIds.toString());
    }

    private String computeSnapshotHash(BillingSnapshot s) {
        String data = String.format("%d|%s|%s|%s|%s",
                s.getBedCount(),
                s.getBaseGramsPerBedPerDay(),
                s.getBaseRatePerBedPerDay(),
                s.getExcessRatePerKg(),
                s.getExcessRateEffectiveFrom());
        return sha256(data);
    }

    private String computeInvoiceHash(UUID billId, BigDecimal total, String invoiceNumber) {
        return sha256(billId.toString() + total.toPlainString() + invoiceNumber);
    }

    private String getFinancialYear(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        if (month < 4) { // Before April
            return String.format("%d-%02d", year - 1, (year % 100));
        } else {
            return String.format("%d-%02d", year, ((year + 1) % 100));
        }
    }

    private int getNextInvoiceSequence(UUID facilityId, String financialYear) {
        jdbcTemplate.update(
                "INSERT INTO invoice_sequence (facility_id, financial_year, last_number) VALUES (?, ?, 0) " +
                        "ON CONFLICT (facility_id, financial_year) DO NOTHING",
                facilityId, financialYear);

        return jdbcTemplate.queryForObject(
                "UPDATE invoice_sequence SET last_number = last_number + 1 " +
                        "WHERE facility_id = ? AND financial_year = ? RETURNING last_number",
                Integer.class, facilityId, financialYear);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "hash_error";
        }
    }
}
