package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.DuesClearanceRequest;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.DuesClearanceRequestRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CbwtfDuesClearanceControllerPaginationTest {

    @Mock
    private DuesClearanceRequestRepository requestRepository;
    @Mock
    private HcfRepository hcfRepository;

    private CbwtfDuesClearanceController controller;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        controller = new CbwtfDuesClearanceController(requestRepository, hcfRepository);
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listRequestsUsesBoundedFacilityStatusQuery() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(requestRepository.findByFacilityIdAndManagementStatusOrderByRequestedAtDesc(
                eq(facilityId), eq("PENDING"), pageable.capture())).thenReturn(List.of());

        controller.listRequests("PENDING", 5000);

        assertEquals(250, pageable.getValue().getPageSize());
        verify(requestRepository, never()).findByFacilityIdAndManagementStatusOrderByRequestedAtDesc(
                facilityId, "PENDING");
    }

    @Test
    void submitForApprovalStoresTypedAmountAndTrimmedNotes() {
        UUID requestId = UUID.randomUUID();
        DuesClearanceRequest request = duesRequest(facilityId);
        when(requestRepository.findByIdAndFacilityId(requestId, facilityId)).thenReturn(Optional.of(request));

        ResponseEntity<?> response = controller.submitForApproval(requestId,
                new CbwtfDuesClearanceController.SubmitForApprovalRequest(new BigDecimal("1250.50"),
                        "  ready for management  "));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(new BigDecimal("1250.50"), request.getAmountCleared());
        assertEquals("ready for management", request.getCbwtfNotes());
        assertEquals(DuesClearanceRequest.Status.SUBMITTED.name(), request.getManagementStatus());
        verify(requestRepository).save(request);
        verify(requestRepository, never()).findById(requestId);
    }

    @Test
    void submitForApprovalRejectsTooPreciseAmountBeforeSaving() {
        UUID requestId = UUID.randomUUID();
        DuesClearanceRequest request = duesRequest(facilityId);
        when(requestRepository.findByIdAndFacilityId(requestId, facilityId)).thenReturn(Optional.of(request));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller.submitForApproval(requestId,
                        new CbwtfDuesClearanceController.SubmitForApprovalRequest(new BigDecimal("10.999"), null)));

        verify(requestRepository, never()).save(any());
        verify(requestRepository, never()).findById(requestId);
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
        request.setManagementStatusEnum(DuesClearanceRequest.Status.PENDING);
        return request;
    }
}
