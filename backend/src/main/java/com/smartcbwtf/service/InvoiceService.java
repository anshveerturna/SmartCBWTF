package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.AgreementSnapshot;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.Invoice;
import com.smartcbwtf.dto.InvoiceGenerateRequest;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final HcfRepository hcfRepository;
    private final AgreementRepository agreementRepository;
    private final AgreementGuardService agreementGuard;
    private final AgreementSnapshotService snapshotService;
    private final PdfService pdfService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    public InvoiceService(InvoiceRepository invoiceRepository,
            HcfRepository hcfRepository,
            AgreementRepository agreementRepository,
            AgreementGuardService agreementGuard,
            AgreementSnapshotService snapshotService,
            PdfService pdfService,
            AuditLogService auditLogService,
            EmailService emailService) {
        this.invoiceRepository = invoiceRepository;
        this.hcfRepository = hcfRepository;
        this.agreementRepository = agreementRepository;
        this.agreementGuard = agreementGuard;
        this.snapshotService = snapshotService;
        this.pdfService = pdfService;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
    }

    @Transactional
    public Invoice generate(InvoiceGenerateRequest request) {
        Hcf hcf = hcfRepository.findById(request.getHcfId()).orElseThrow();
        Agreement agreement = agreementRepository.findFirstByHcfIdAndStatusOrderByStartDateDesc(hcf.getId(), "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active agreement for HCF"));

        // *** CRITICAL: Agreement Guard Check ***
        agreementGuard.assertAgreementActive(agreement.getId(), "INVOICE_GENERATE");

        // Tax rate priority: request override > HCF's stored rate > default 5%
        double taxRate = request.getTaxRate() != null ? request.getTaxRate()
                : hcf.getTaxRateDecimal();

        LocalDate start = request.getPeriodStart();
        LocalDate end = request.getPeriodEnd();
        long days = ChronoUnit.DAYS.between(start, end.plusDays(1));
        int beds = hcf.getNumberOfBeds() != null ? hcf.getNumberOfBeds() : 0;

        BigDecimal base = agreement.getPerBedPerDayRate()
                .multiply(BigDecimal.valueOf(beds))
                .multiply(BigDecimal.valueOf(days));
        BigDecimal tax = base.multiply(BigDecimal.valueOf(taxRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = base.add(tax).setScale(2, RoundingMode.HALF_UP);

        // Create snapshot BEFORE invoice for historical accuracy
        AgreementSnapshot snapshot = snapshotService.createInvoiceSnapshot(agreement.getId(), null);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber(hcf.getCode()));
        invoice.setHcf(hcf);
        invoice.setFacility(agreement.getFacility());
        invoice.setAgreement(agreement);
        invoice.setPeriodStart(start);
        invoice.setPeriodEnd(end);
        invoice.setBeds(beds);
        invoice.setPerBedPerDayRate(agreement.getPerBedPerDayRate());
        invoice.setBaseAmount(base);
        invoice.setTaxAmount(tax);
        invoice.setTotalAmount(total);
        invoice.setStatus("DRAFT");
        invoiceRepository.save(invoice);

        String pdfPath = pdfService.generateInvoicePdf(invoice);
        invoice.setPdfUrl(pdfPath);
        invoiceRepository.save(invoice);
        auditLogService.log("INVOICE", invoice.getId(), "GENERATE", null,
                "{\"snapshotId\":\"" + snapshot.getId() + "\"}");

        // Send invoice email to HCF
        if (hcf.getContactEmail() != null && !hcf.getContactEmail().isBlank()) {
            try {
                String period = start.toString() + " to " + end.toString();
                String dueDate = end.plusDays(30).toString();
                String html = emailService.getTemplates().invoiceGenerated(
                        hcf.getName(),
                        invoice.getInvoiceNumber(),
                        period,
                        total.toString(),
                        dueDate);
                emailService.sendHtmlEmail(hcf.getContactEmail(),
                        "Invoice Generated - " + invoice.getInvoiceNumber(), html);
                log.info("Invoice email sent to HCF: {}", hcf.getContactEmail());
            } catch (Exception e) {
                log.warn("Failed to send invoice email to {}: {}", hcf.getContactEmail(), e.getMessage());
            }
        }

        return invoice;
    }

    public Invoice get(UUID id) {
        return invoiceRepository.findById(id).orElseThrow();
    }

    private String generateInvoiceNumber(String hcfCode) {
        long seq = invoiceRepository.count() + 1;
        return hcfCode + "-INV-" + String.format("%06d", seq);
    }
}
