package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.AgreementCorrectionRequest;
import com.smartcbwtf.domain.DuesClearanceRequest;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.HcfDetailDTO;
import com.smartcbwtf.repository.AgreementCorrectionRequestRepository;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.DuesClearanceRequestRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.service.CbwtfHcfService;
import com.smartcbwtf.service.UploadFileValidator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopManagementControllerSecurityTest {

    @Mock
    private DuesClearanceRequestRepository requestRepository;
    @Mock
    private AgreementCorrectionRequestRepository correctionRequestRepository;
    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private CbwtfHcfService hcfService;

    private TopManagementController controller;
    private UUID facilityId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        controller = new TopManagementController(
                requestRepository,
                correctionRequestRepository,
                hcfRepository,
                agreementRepository,
                hcfService);
        facilityId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, facilityId, null, "TOP_MANAGEMENT", "top"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listDuesApprovalsUsesTenantFacility() {
        DuesClearanceRequest request = duesRequest(facilityId);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(requestRepository.findByFacilityIdAndManagementStatusOrderByRequestedAtDesc(
                eq(facilityId), eq("SUBMITTED"), pageable.capture()))
                .thenReturn(List.of(request));

        ResponseEntity<List<Map<String, Object>>> response = controller.listPendingApprovals(5000);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(250, pageable.getValue().getPageSize());
        verify(requestRepository).findByFacilityIdAndManagementStatusOrderByRequestedAtDesc(
                eq(facilityId), eq("SUBMITTED"), org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(requestRepository, never()).findByManagementStatusOrderByRequestedAtDesc("SUBMITTED");
    }

    @Test
    void approveDuesMasksCrossTenantRequest() {
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findByIdAndFacilityId(requestId, facilityId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.approveRequest(requestId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(requestRepository).findByIdAndFacilityId(requestId, facilityId);
        verify(requestRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(hcfRepository);
    }

    @Test
    void approvePeriodScopedDuesDoesNotSetGlobalHcfDuesStatus() {
        DuesClearanceRequest request = duesRequest(facilityId);
        request.setRequestMonth(6);
        request.setRequestYear(2026);
        UUID requestId = request.getId();
        when(requestRepository.findByIdAndFacilityId(requestId, facilityId)).thenReturn(Optional.of(request));

        ResponseEntity<?> response = controller.approveRequest(requestId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(requestRepository).save(request);
        verifyNoInteractions(hcfRepository);
    }

    @Test
    void rejectDuesNormalizesReasonBeforeSaving() {
        DuesClearanceRequest request = duesRequest(facilityId);
        UUID requestId = request.getId();
        when(requestRepository.findByIdAndFacilityId(requestId, facilityId)).thenReturn(Optional.of(request));

        ResponseEntity<?> response = controller.rejectRequest(requestId,
                new TopManagementController.RejectRequest("  Missing\nreceipt\tcopy  "));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Missing receipt copy", request.getRejectionReason());
        verify(requestRepository).save(request);
        verify(hcfRepository).save(request.getHcf());
    }

    @Test
    void rejectPeriodScopedDuesDoesNotResetGlobalHcfDuesStatus() {
        DuesClearanceRequest request = duesRequest(facilityId);
        request.setRequestMonth(6);
        request.setRequestYear(2026);
        UUID requestId = request.getId();
        when(requestRepository.findByIdAndFacilityId(requestId, facilityId)).thenReturn(Optional.of(request));

        ResponseEntity<?> response = controller.rejectRequest(requestId,
                new TopManagementController.RejectRequest("Missing receipt"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Missing receipt", request.getRejectionReason());
        verify(requestRepository).save(request);
        verifyNoInteractions(hcfRepository);
    }

    @Test
    void listHcfApprovalsUsesTenantAgreements() {
        Agreement agreement = agreement(facilityId, "PENDING_APPROVAL");
        agreement.getHcf().setStatus("PENDING_APPROVAL");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(agreementRepository.findPendingApprovalAgreementsByFacilityId(eq(facilityId), pageable.capture()))
                .thenReturn(List.of(agreement));

        ResponseEntity<List<Map<String, Object>>> response = controller.listPendingHcfApprovals(5000);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(250, pageable.getValue().getPageSize());
        verify(agreementRepository).findPendingApprovalAgreementsByFacilityId(
                eq(facilityId), org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(agreementRepository, never()).findLatestAgreementsByFacilityId(facilityId);
    }

    @Test
    void correctionApprovalPassesTenantToService() {
        UUID correctionId = UUID.randomUUID();

        controller.approveCorrection(correctionId);

        verify(hcfService).approveCorrectionRequest(correctionId, facilityId, userId);
    }

    @Test
    void hcfRejectionPassesNormalizedReasonToService() {
        UUID hcfId = UUID.randomUUID();

        controller.rejectHcfRegistration(hcfId, new TopManagementController.RejectRequest("  Missing\nKYC\t "));

        verify(hcfService).rejectHcfByTopManagement(hcfId, facilityId, "Missing KYC");
    }

    @Test
    void rentAgreementDownloadUsesNoStoreCacheHeader() throws Exception {
        UUID hcfId = UUID.randomUUID();
        String publicUrl = "/uploads/rent-agreements/" + facilityId + "_" + UUID.randomUUID() + ".pdf";
        Path agreementPath = UploadFileValidator.uploadedAssetPath(publicUrl, "/uploads/rent-agreements/");
        HcfDetailDTO detail = new HcfDetailDTO();
        detail.setRentAgreementUrl(publicUrl);
        when(hcfService.getHcfDetailForTopManagement(hcfId, facilityId)).thenReturn(detail);

        try {
            Files.createDirectories(agreementPath.getParent());
            Files.write(agreementPath, "%PDF-1.4".getBytes(StandardCharsets.US_ASCII));

            ResponseEntity<org.springframework.core.io.Resource> response = controller.downloadRentAgreement(hcfId);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("no-store", response.getHeaders().getCacheControl());
        } finally {
            Files.deleteIfExists(agreementPath);
        }
    }

    @Test
    void correctionRejectionPassesNormalizedReasonToService() {
        UUID correctionId = UUID.randomUUID();

        controller.rejectCorrection(correctionId, new TopManagementController.RejectRequest("  Invalid\nproof\t "));

        verify(hcfService).rejectCorrectionRequest(correctionId, facilityId, "Invalid proof", userId);
    }

    @Test
    void rejectRequestValidationRequiresBoundedReason() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        var blankViolations = validator.validate(new TopManagementController.RejectRequest(""));
        var longViolations = validator.validate(new TopManagementController.RejectRequest("x".repeat(501)));

        assertTrue(blankViolations.stream().anyMatch(v -> "reason".contentEquals(v.getPropertyPath().toString())));
        assertTrue(longViolations.stream().anyMatch(v -> "reason".contentEquals(v.getPropertyPath().toString())));
    }

    @Test
    void listCorrectionsUsesTenantFacility() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(correctionRequestRepository.findByFacilityIdAndStatusOrderByRequestedAtDesc(
                eq(facilityId), eq(AgreementCorrectionRequest.Status.PENDING), pageable.capture()))
                .thenReturn(List.of());

        controller.listPendingCorrections(5000);

        assertEquals(250, pageable.getValue().getPageSize());
        verify(correctionRequestRepository).findByFacilityIdAndStatusOrderByRequestedAtDesc(
                eq(facilityId), eq(AgreementCorrectionRequest.Status.PENDING),
                org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(correctionRequestRepository, never()).findByStatusOrderByRequestedAtDesc(
                AgreementCorrectionRequest.Status.PENDING);
    }

    private DuesClearanceRequest duesRequest(UUID ownerFacilityId) {
        Agreement agreement = agreement(ownerFacilityId, "ACTIVE");
        DuesClearanceRequest request = new DuesClearanceRequest();
        request.setId(UUID.randomUUID());
        request.setFacility(agreement.getFacility());
        request.setHcf(agreement.getHcf());
        request.setAgreement(agreement);
        request.setRequestedAt(Instant.now());
        request.setManagementStatusEnum(DuesClearanceRequest.Status.SUBMITTED);
        return request;
    }

    private Agreement agreement(UUID ownerFacilityId, String status) {
        Facility facility = new Facility();
        facility.setId(ownerFacilityId);
        facility.setName("Facility");
        facility.setCode("FAC");
        facility.setAddress("Address");

        Hcf hcf = new Hcf();
        hcf.setId(UUID.randomUUID());
        hcf.setName("HCF");
        hcf.setCode("HCF-1");

        Agreement agreement = new Agreement();
        agreement.setId(UUID.randomUUID());
        agreement.setAgreementNumber("AGR-1");
        agreement.setFacility(facility);
        agreement.setHcf(hcf);
        agreement.setStatus(status);
        agreement.setCreatedAt(Instant.now());
        return agreement;
    }
}
