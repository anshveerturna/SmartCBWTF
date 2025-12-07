package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.HcfApprovalRequest;
import com.smartcbwtf.dto.HcfApprovalResponse;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.UUID;

@Service
public class AgreementService {

    private final AgreementRepository agreementRepository;
    private final FacilityRepository facilityRepository;
    private final HcfRepository hcfRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public AgreementService(AgreementRepository agreementRepository,
                            FacilityRepository facilityRepository,
                            HcfRepository hcfRepository,
                            PdfService pdfService,
                            EmailService emailService,
                            AuditLogService auditLogService) {
        this.agreementRepository = agreementRepository;
        this.facilityRepository = facilityRepository;
        this.hcfRepository = hcfRepository;
        this.pdfService = pdfService;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public HcfApprovalResponse approveHcf(UUID hcfId, HcfApprovalRequest request) {
        Hcf hcf = hcfRepository.findById(hcfId).orElseThrow();
        Facility facility = facilityRepository.findById(request.getFacilityId()).orElseThrow();

        String agreementNumber = generateAgreementNumber(facility.getCode());
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber(agreementNumber);
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setStartDate(request.getStartDate());
        agreement.setEndDate(request.getEndDate());
        agreement.setPerBedPerDayRate(request.getPerBedPerDayRate());
        agreement.setStatus("ACTIVE");
        agreementRepository.save(agreement);

        hcf.setStatus("ACTIVE");
        hcfRepository.save(hcf);

        String pdfPath = pdfService.generateAgreementPdf(agreement);
        agreement.setPdfUrl(pdfPath);
        agreementRepository.save(agreement);

        auditLogService.log("AGREEMENT", agreement.getId(), "APPROVE", null, null);

        emailService.sendEmail(hcf.getContactEmail(), "Agreement Approved", "Agreement " + agreementNumber + " approved.");
        if (facility.getContactEmail() != null) {
            emailService.sendEmail(facility.getContactEmail(), "Agreement Approved", "Agreement " + agreementNumber + " approved.");
        }

        return new HcfApprovalResponse(hcf.getId(), hcf.getStatus(), agreementNumber);
    }

    private String generateAgreementNumber(String facilityCode) {
        long seq = agreementRepository.count() + 1;
        return facilityCode + "-" + Year.now().getValue() + "-" + String.format("%05d", seq);
    }
}
