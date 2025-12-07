package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.Invoice;
import com.smartcbwtf.dto.InvoiceGenerateRequest;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final HcfRepository hcfRepository;
    private final AgreementRepository agreementRepository;
    private final PdfService pdfService;
    private final AuditLogService auditLogService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          HcfRepository hcfRepository,
                          AgreementRepository agreementRepository,
                          PdfService pdfService,
                          AuditLogService auditLogService) {
        this.invoiceRepository = invoiceRepository;
        this.hcfRepository = hcfRepository;
        this.agreementRepository = agreementRepository;
        this.pdfService = pdfService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Invoice generate(InvoiceGenerateRequest request) {
        Hcf hcf = hcfRepository.findById(request.getHcfId()).orElseThrow();
        Agreement agreement = agreementRepository.findFirstByHcfIdAndStatusOrderByStartDateDesc(hcf.getId(), "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active agreement for HCF"));
        double taxRate = request.getTaxRate() != null ? request.getTaxRate() : 0.18;

        LocalDate start = request.getPeriodStart();
        LocalDate end = request.getPeriodEnd();
        long days = ChronoUnit.DAYS.between(start, end.plusDays(1));
        int beds = hcf.getNumberOfBeds() != null ? hcf.getNumberOfBeds() : 0;

        BigDecimal base = agreement.getPerBedPerDayRate()
                .multiply(BigDecimal.valueOf(beds))
                .multiply(BigDecimal.valueOf(days));
        BigDecimal tax = base.multiply(BigDecimal.valueOf(taxRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = base.add(tax).setScale(2, RoundingMode.HALF_UP);

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
        auditLogService.log("INVOICE", invoice.getId(), "GENERATE", null, null);
        return invoice;
    }

    public List<Invoice> listAll() {
        return invoiceRepository.findAll();
    }

    public Invoice get(UUID id) {
        return invoiceRepository.findById(id).orElseThrow();
    }

    private String generateInvoiceNumber(String hcfCode) {
        long seq = invoiceRepository.count() + 1;
        return hcfCode + "-INV-" + String.format("%06d", seq);
    }
}
