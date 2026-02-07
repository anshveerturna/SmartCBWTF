package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.LabelIssueRequest;
import com.smartcbwtf.dto.LabelIssueResponse;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LabelService {

    private final BagLabelRepository bagLabelRepository;
    private final HcfRepository hcfRepository;
    private final FacilityRepository facilityRepository;
    private final AgreementRepository agreementRepository;
    private final AgreementGuardService agreementGuard;
    private final PdfService pdfService;
    private final AuditLogService auditLogService;

    public LabelService(BagLabelRepository bagLabelRepository,
            HcfRepository hcfRepository,
            FacilityRepository facilityRepository,
            AgreementRepository agreementRepository,
            AgreementGuardService agreementGuard,
            PdfService pdfService,
            AuditLogService auditLogService) {
        this.bagLabelRepository = bagLabelRepository;
        this.hcfRepository = hcfRepository;
        this.facilityRepository = facilityRepository;
        this.agreementRepository = agreementRepository;
        this.agreementGuard = agreementGuard;
        this.pdfService = pdfService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public LabelIssueResponse issue(LabelIssueRequest request) {
        Hcf hcf = hcfRepository.findById(request.getHcfId()).orElseThrow();
        Facility facility = facilityRepository.findById(request.getFacilityId()).orElseThrow();

        // *** CRITICAL: Agreement Guard Check ***
        Agreement agreement = agreementRepository.findActiveByHcfAndFacility(hcf.getId(), facility.getId())
                .orElseThrow(() -> new IllegalStateException("No active agreement for HCF under this facility"));
        agreementGuard.assertAgreementActive(agreement.getId(), "LABEL_ISSUE");

        int quantity = request.getQuantity();
        List<String> qrCodes = new ArrayList<>(quantity);
        long existingCount = bagLabelRepository.count();
        for (int i = 0; i < quantity; i++) {
            String serial = String.format("%08d", existingCount + i + 1);
            String qrCode = "CBWTF|" + hcf.getCode() + "|" + request.getCategory() + "|" + serial;
            BagLabel label = new BagLabel();
            label.setHcf(hcf);
            label.setFacility(facility);
            label.setCategory(request.getCategory());
            label.setSerialNo(serial);
            label.setQrCode(qrCode);
            label.setStatus("ISSUED");
            bagLabelRepository.save(label);
            qrCodes.add(qrCode);
        }
        String pdfUrl = pdfService.generateLabelBatchPdf(hcf, facility, request.getCategory(),
                qrCodes.toArray(new String[0]));
        auditLogService.log("BAG_LABEL", null, "ISSUE", null,
                "{\"quantity\":" + quantity + ",\"agreementId\":\"" + agreement.getId() + "\"}");
        return new LabelIssueResponse(hcf.getId(), facility.getId(), request.getCategory(), quantity, qrCodes, pdfUrl);
    }

    /**
     * Issue labels for multiple waste categories and produce a single combined PDF.
     *
     * @param hcfId              HCF id
     * @param facilityId         Facility id
     * @param categoryQuantities ordered map of category -> quantity
     * @return combined response with all QR codes and a single PDF URL
     */
    @Transactional
    public LabelIssueResponse issueMultiCategory(java.util.UUID hcfId, java.util.UUID facilityId,
            Map<String, Integer> categoryQuantities) {
        Hcf hcf = hcfRepository.findById(hcfId).orElseThrow();
        Facility facility = facilityRepository.findById(facilityId).orElseThrow();

        Agreement agreement = agreementRepository.findActiveByHcfAndFacility(hcf.getId(), facility.getId())
                .orElseThrow(() -> new IllegalStateException("No active agreement for HCF under this facility"));
        agreementGuard.assertAgreementActive(agreement.getId(), "LABEL_ISSUE");

        long existingCount = bagLabelRepository.count();
        int totalQuantity = 0;
        List<String> allQrCodes = new ArrayList<>();
        Map<String, String[]> categoryQrCodes = new LinkedHashMap<>();

        for (var entry : categoryQuantities.entrySet()) {
            String category = entry.getKey();
            int qty = entry.getValue();
            if (qty <= 0) continue;

            List<String> catCodes = new ArrayList<>(qty);
            for (int i = 0; i < qty; i++) {
                String serial = String.format("%08d", existingCount + totalQuantity + i + 1);
                String qrCode = "CBWTF|" + hcf.getCode() + "|" + category + "|" + serial;
                BagLabel label = new BagLabel();
                label.setHcf(hcf);
                label.setFacility(facility);
                label.setCategory(category);
                label.setSerialNo(serial);
                label.setQrCode(qrCode);
                label.setStatus("ISSUED");
                bagLabelRepository.save(label);
                catCodes.add(qrCode);
            }
            allQrCodes.addAll(catCodes);
            categoryQrCodes.put(category, catCodes.toArray(new String[0]));
            totalQuantity += qty;
        }

        String pdfUrl = pdfService.generateMultiCategoryLabelBatchPdf(hcf, facility, categoryQrCodes);
        auditLogService.log("BAG_LABEL", null, "ISSUE_MULTI", null,
                "{\"totalQuantity\":" + totalQuantity + ",\"categories\":" + categoryQuantities.size()
                        + ",\"agreementId\":\"" + agreement.getId() + "\"}");
        return new LabelIssueResponse(hcf.getId(), facility.getId(), "MULTI", totalQuantity, allQrCodes, pdfUrl);
    }
}
