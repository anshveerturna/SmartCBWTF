package com.smartcbwtf.service;

import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.LabelIssueRequest;
import com.smartcbwtf.dto.LabelIssueResponse;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class LabelService {

    private final BagLabelRepository bagLabelRepository;
    private final HcfRepository hcfRepository;
    private final FacilityRepository facilityRepository;
    private final PdfService pdfService;
    private final AuditLogService auditLogService;

    public LabelService(BagLabelRepository bagLabelRepository,
                        HcfRepository hcfRepository,
                        FacilityRepository facilityRepository,
                        PdfService pdfService,
                        AuditLogService auditLogService) {
        this.bagLabelRepository = bagLabelRepository;
        this.hcfRepository = hcfRepository;
        this.facilityRepository = facilityRepository;
        this.pdfService = pdfService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public LabelIssueResponse issue(LabelIssueRequest request) {
        Hcf hcf = hcfRepository.findById(request.getHcfId()).orElseThrow();
        Facility facility = facilityRepository.findById(request.getFacilityId()).orElseThrow();
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
        String pdfUrl = pdfService.generateLabelBatchPdf(hcf, facility, request.getCategory(), qrCodes.toArray(new String[0]));
        auditLogService.log("BAG_LABEL", null, "ISSUE", null, "{\"quantity\":" + quantity + "}");
        return new LabelIssueResponse(hcf.getId(), facility.getId(), request.getCategory(), quantity, qrCodes, pdfUrl);
    }
}
