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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CbwtfDuesClearanceControllerSecurityTest {

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
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "cbwtf"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void rejectPeriodScopedRequestDoesNotResetGlobalHcfDuesStatus() {
        DuesClearanceRequest request = duesRequest();
        request.setRequestMonth(6);
        request.setRequestYear(2026);
        UUID requestId = request.getId();
        when(requestRepository.findByIdAndFacilityId(requestId, facilityId)).thenReturn(Optional.of(request));

        ResponseEntity<?> response = controller.rejectRequest(requestId,
                new CbwtfDuesClearanceController.RejectClearanceRequest(
                        "Outstanding dues pending",
                        new BigDecimal("1500.00")));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("REJECTED", request.getManagementStatus());
        assertEquals("Outstanding dues pending", request.getRejectionReason());
        verify(requestRepository).save(request);
        verify(requestRepository, never()).findById(requestId);
        verifyNoInteractions(hcfRepository);
    }

    @Test
    void rejectUsesTenantScopedLookup() {
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findByIdAndFacilityId(requestId, facilityId)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> controller.rejectRequest(requestId,
                        new CbwtfDuesClearanceController.RejectClearanceRequest(
                                "Not cleared", new BigDecimal("100.00"))));

        verify(requestRepository).findByIdAndFacilityId(requestId, facilityId);
        verify(requestRepository, never()).findById(requestId);
        verify(requestRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(hcfRepository);
    }

    private DuesClearanceRequest duesRequest() {
        Facility facility = new Facility();
        facility.setId(facilityId);
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
