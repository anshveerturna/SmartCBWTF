package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.DuesClearanceRequest;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.DuesClearanceRequestRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.service.AuditLogService;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagementDuesApprovalControllerSecurityTest {

    @Mock
    private DuesClearanceRequestRepository clearanceRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private HcfRepository hcfRepository;

    private ManagementDuesApprovalController controller;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        controller = new ManagementDuesApprovalController(clearanceRepository, auditLogService, hcfRepository);
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "TOP_MANAGEMENT", "top"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listPendingUsesTenantFacilityForAllStatus() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(clearanceRepository.findByFacilityIdOrderByRequestedAtDesc(eq(facilityId), pageable.capture()))
                .thenReturn(List.of());

        controller.listPending("ALL", 0);

        assertEquals(100, pageable.getValue().getPageSize());
        verify(clearanceRepository).findByFacilityIdOrderByRequestedAtDesc(
                eq(facilityId), org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(clearanceRepository).countByFacilityId(facilityId);
        verify(clearanceRepository, never()).findAll();
    }

    @Test
    void approveMasksCrossTenantRequest() {
        UUID requestId = UUID.randomUUID();
        when(clearanceRepository.findByIdAndFacilityId(requestId, facilityId)).thenReturn(java.util.Optional.empty());

        ResponseEntity<?> response = controller.approve(requestId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(clearanceRepository).findByIdAndFacilityId(requestId, facilityId);
        verify(clearanceRepository, never()).findById(requestId);
        verify(clearanceRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(hcfRepository);
    }

    @Test
    void approvePeriodScopedRequestDoesNotSetGlobalHcfDuesStatus() {
        DuesClearanceRequest request = duesRequest(facilityId);
        request.setRequestMonth(6);
        request.setRequestYear(2026);
        UUID requestId = request.getId();
        when(clearanceRepository.findByIdAndFacilityId(requestId, facilityId)).thenReturn(java.util.Optional.of(request));

        ResponseEntity<?> response = controller.approve(requestId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(clearanceRepository).findByIdAndFacilityId(requestId, facilityId);
        verify(clearanceRepository, never()).findById(requestId);
        verify(clearanceRepository).save(request);
        verifyNoInteractions(hcfRepository);
    }

    @Test
    void detailsUsesTenantScopedLookup() {
        UUID requestId = UUID.randomUUID();
        when(clearanceRepository.findByIdAndFacilityId(requestId, facilityId)).thenReturn(java.util.Optional.empty());

        ResponseEntity<?> response = controller.getDetails(requestId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(clearanceRepository).findByIdAndFacilityId(requestId, facilityId);
        verify(clearanceRepository, never()).findById(requestId);
    }

    @Test
    void bulkApproveRejectsOversizedBatchBeforeLoadingRequests() {
        List<UUID> ids = IntStream.range(0, 101)
                .mapToObj(i -> UUID.randomUUID())
                .toList();

        ResponseEntity<?> response = controller.bulkApprove(
                new ManagementDuesApprovalController.BulkApproveRequest(ids));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(clearanceRepository, never()).findById(any(UUID.class));
        verify(clearanceRepository, never()).findByFacilityIdAndIdIn(
                eq(facilityId), org.mockito.ArgumentMatchers.anyList());
        verify(clearanceRepository, never()).save(any());
        verifyNoInteractions(hcfRepository);
    }

    @Test
    void bulkApproveLoadsOnlyTenantScopedRequests() {
        DuesClearanceRequest request = duesRequest(facilityId);
        UUID approvedId = request.getId();
        UUID missingOrForeignId = UUID.randomUUID();
        when(clearanceRepository.findByFacilityIdAndIdIn(facilityId, List.of(approvedId, missingOrForeignId)))
                .thenReturn(List.of(request));

        ResponseEntity<?> response = controller.bulkApprove(
                new ManagementDuesApprovalController.BulkApproveRequest(List.of(approvedId, missingOrForeignId)));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(1, body.get("approved"));
        assertEquals(1, body.get("failed"));
        verify(clearanceRepository).findByFacilityIdAndIdIn(facilityId, List.of(approvedId, missingOrForeignId));
        verify(clearanceRepository, never()).findById(any(UUID.class));
        verify(clearanceRepository).save(request);
    }

    private DuesClearanceRequest duesRequest(UUID ownerFacilityId) {
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
        agreement.setStatus("ACTIVE");

        DuesClearanceRequest request = new DuesClearanceRequest();
        request.setId(UUID.randomUUID());
        request.setFacility(facility);
        request.setHcf(hcf);
        request.setAgreement(agreement);
        request.setRequestedAt(Instant.now());
        request.setManagementStatusEnum(DuesClearanceRequest.Status.SUBMITTED);
        return request;
    }
}
