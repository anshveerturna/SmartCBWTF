package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.LabelIssueRequest;
import com.smartcbwtf.dto.LabelIssueResponse;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LabelService {

        private final HcfRepository hcfRepository;
        private final FacilityRepository facilityRepository;
        private final AgreementRepository agreementRepository;
        private final AgreementGuardService agreementGuard;
        private final PdfService pdfService;
        private final AuditLogService auditLogService;
        private final QrAuthorizationService qrAuthService;

        public LabelService(HcfRepository hcfRepository,
                        FacilityRepository facilityRepository,
                        AgreementRepository agreementRepository,
                        AgreementGuardService agreementGuard,
                        PdfService pdfService,
                        AuditLogService auditLogService,
                        QrAuthorizationService qrAuthService) {
                this.hcfRepository = hcfRepository;
                this.facilityRepository = facilityRepository;
                this.agreementRepository = agreementRepository;
                this.agreementGuard = agreementGuard;
                this.pdfService = pdfService;
                this.auditLogService = auditLogService;
                this.qrAuthService = qrAuthService;
        }

        @Transactional
        public LabelIssueResponse issue(LabelIssueRequest request) {
                Hcf hcf = hcfRepository.findById(request.getHcfId()).orElseThrow();
                Facility facility = facilityRepository.findById(request.getFacilityId()).orElseThrow();

                // *** CRITICAL: Agreement Guard Check ***
                Agreement agreement = agreementRepository.findActiveByHcfAndFacility(hcf.getId(), facility.getId())
                                .orElseThrow(() -> new IllegalStateException(
                                                "No active agreement for HCF under this facility"));
                agreementGuard.assertAgreementActive(agreement.getId(), "LABEL_ISSUE");

                int quantity = request.getQuantity();

                // Determine validity period from agreement
                Instant validFrom = Instant.now();
                Instant validTo;
                if (agreement.getEndDate() != null) {
                        validTo = agreement.getEndDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
                } else {
                        validTo = validFrom.plus(365, ChronoUnit.DAYS);
                }

                UUID createdBy = TenantContext.getUserId();

                // Generate signed QR codes via QrAuthorizationService
                // This creates proper QrAuthorization + BagLabel records for each label
                // QR payload includes: qrId, agreementId, hcfId, facilityId, wasteCategory,
                // validFrom, validTo, checksum
                List<QrAuthorizationService.QrGenerateResult> qrResults = qrAuthService.generateQrBulk(
                                request.getHcfId(), request.getCategory(),
                                quantity, validFrom, validTo, createdBy);

                List<String> qrCodes = qrResults.stream()
                                .map(QrAuthorizationService.QrGenerateResult::qrPayloadJson)
                                .toList();

                String pdfUrl = pdfService.generateLabelBatchPdf(hcf, facility, request.getCategory(),
                                qrCodes.toArray(new String[0]), request.getValidUntil());

                auditLogService.log("BAG_LABEL", null, "ISSUE", null,
                                "{\"quantity\":" + quantity + ",\"agreementId\":\"" + agreement.getId() + "\"}");

                return new LabelIssueResponse(hcf.getId(), facility.getId(), request.getCategory(), quantity, qrCodes,
                                pdfUrl);
        }

        /**
         * Issue labels for multiple waste categories and produce a single combined PDF.
         * Uses QrAuthorizationService to create proper signed QR codes with full
         * payload (qrId, agreementId, hcfId, facilityId, wasteCategory, validity,
         * checksum).
         */
        @Transactional
        public LabelIssueResponse issueMultiCategory(UUID hcfId, UUID facilityId,
                        Map<String, Integer> categoryQuantities, java.time.LocalDate validUntil) {
                Hcf hcf = hcfRepository.findById(hcfId).orElseThrow();
                Facility facility = facilityRepository.findById(facilityId).orElseThrow();

                Agreement agreement = agreementRepository.findActiveByHcfAndFacility(hcf.getId(), facility.getId())
                                .orElseThrow(() -> new IllegalStateException(
                                                "No active agreement for HCF under this facility"));
                agreementGuard.assertAgreementActive(agreement.getId(), "LABEL_ISSUE");

                // Determine validity period from agreement
                Instant validFrom = Instant.now();
                Instant validTo;
                if (agreement.getEndDate() != null) {
                        validTo = agreement.getEndDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
                } else {
                        validTo = validFrom.plus(365, ChronoUnit.DAYS);
                }

                UUID createdBy = TenantContext.getUserId();

                int totalQuantity = 0;
                List<String> allQrCodes = new ArrayList<>();
                Map<String, String[]> categoryQrCodes = new LinkedHashMap<>();

                for (var entry : categoryQuantities.entrySet()) {
                        String category = entry.getKey();
                        int qty = entry.getValue();
                        if (qty <= 0)
                                continue;

                        List<QrAuthorizationService.QrGenerateResult> qrResults = qrAuthService.generateQrBulk(hcfId,
                                        category, qty, validFrom, validTo, createdBy);

                        String[] payloads = qrResults.stream()
                                        .map(QrAuthorizationService.QrGenerateResult::qrPayloadJson)
                                        .toArray(String[]::new);

                        allQrCodes.addAll(Arrays.asList(payloads));
                        categoryQrCodes.put(category, payloads);
                        totalQuantity += qty;
                }

                String pdfUrl = pdfService.generateMultiCategoryLabelBatchPdf(hcf, facility, categoryQrCodes,
                                validUntil);

                auditLogService.log("BAG_LABEL", null, "ISSUE_MULTI", null,
                                "{\"totalQuantity\":" + totalQuantity + ",\"categories\":" + categoryQuantities.size()
                                                + ",\"agreementId\":\"" + agreement.getId() + "\"}");

                return new LabelIssueResponse(hcf.getId(), facility.getId(), "MULTI", totalQuantity, allQrCodes,
                                pdfUrl);
        }
}
