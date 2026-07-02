package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.DuesClearanceRequest;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.DuesClearanceRequestRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.service.HcfAccessGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;

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
class HcfDuesClearanceControllerPaginationTest {

    @Mock
    private DuesClearanceRequestRepository clearanceRepo;
    @Mock
    private HcfRepository hcfRepo;
    @Mock
    private AgreementRepository agreementRepo;
    @Mock
    private HcfAccessGuard accessGuard;

    private HcfDuesClearanceController controller;
    private UUID hcfId;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        controller = new HcfDuesClearanceController(clearanceRepo, hcfRepo, agreementRepo, accessGuard);
        hcfId = UUID.randomUUID();
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void historyUsesBoundedHcfQuery() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(clearanceRepo.findByHcfIdAndFacilityIdOrderByRequestedAtDesc(eq(hcfId), eq(facilityId),
                pageable.capture())).thenReturn(List.of());

        var response = controller.getHistory(5000);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
        assertEquals(250, pageable.getValue().getPageSize());
        verify(clearanceRepo, never()).findByHcfIdOrderByRequestedAtDesc(hcfId);
        verify(clearanceRepo, never()).findByHcfIdOrderByRequestedAtDesc(eq(hcfId), any(Pageable.class));
    }

    @Test
    void requestReportAccessUsesTenantScopedHcfAndAgreement() {
        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        Facility facility = new Facility();
        facility.setId(facilityId);
        Agreement agreement = new Agreement();
        agreement.setId(UUID.randomUUID());
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setStatus(Agreement.Status.ACTIVE.name());

        when(hcfRepo.findByIdAndFacilityId(hcfId, facilityId)).thenReturn(Optional.of(hcf));
        when(agreementRepo.findActiveByHcfAndFacility(hcfId, facilityId)).thenReturn(Optional.of(agreement));
        when(clearanceRepo.save(any(DuesClearanceRequest.class))).thenAnswer(invocation -> {
            DuesClearanceRequest request = invocation.getArgument(0);
            request.setId(UUID.randomUUID());
            return request;
        });

        var response = controller.requestReportAccess(null);

        assertEquals(200, response.getStatusCode().value());
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
        verify(clearanceRepo).existsByHcfIdAndFacilityIdAndManagementStatusIn(
                hcfId, facilityId, List.of("PENDING", "SUBMITTED"));
        verify(hcfRepo).findByIdAndFacilityId(hcfId, facilityId);
        verify(agreementRepo).findActiveByHcfAndFacility(hcfId, facilityId);
        verify(hcfRepo, never()).findById(hcfId);
        verify(agreementRepo, never()).findByHcfIdAndStatus(hcfId, Agreement.Status.ACTIVE.name());
    }
}
