package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CBWTFDashboardServiceTest {

    @Mock
    private TenantAssertionService tenantAssertion;
    @Mock
    private AgreementRepository agreementRepo;
    @Mock
    private BagEventRepository bagEventRepo;
    @Mock
    private BagLabelRepository bagLabelRepo;
    @Mock
    private InvoiceRepository invoiceRepo;
    @Mock
    private FacilityRepository facilityRepo;
    @Mock
    private AppUserRepository userRepo;

    @Test
    void dashboardLimitsDisplayedExpiringAgreementsButKeepsTotalCount() {
        UUID facilityId = UUID.randomUUID();
        when(tenantAssertion.getRequiredTenantId()).thenReturn(facilityId);
        when(facilityRepo.findById(facilityId)).thenReturn(java.util.Optional.of(facility(facilityId)));
        when(agreementRepo.countExpiringSoonByFacilityId(eq(facilityId), any(LocalDate.class))).thenReturn(42L);
        when(bagEventRepo.findRecentByFacilityId(facilityId, 10)).thenReturn(List.of());
        Agreement agreement = agreement("AGR-1", LocalDate.now().plusDays(5));
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(agreementRepo.findExpiringSoonByFacilityId(eq(facilityId), any(LocalDate.class), pageable.capture()))
                .thenReturn(List.of(agreement));

        var response = service().getDashboardMetrics();

        assertEquals(42L, response.getAgreementsExpiringSoon());
        assertEquals(1, response.getExpiringAgreements().size());
        assertEquals(10, pageable.getValue().getPageSize());
    }

    @Test
    void anomalyDetailsUseBoundedQueriesAndBatchStaffLookup() {
        UUID facilityId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        when(tenantAssertion.getRequiredTenantId()).thenReturn(facilityId);
        BagEvent anomaly = bagEvent(facilityId, staffId, "MISMATCH");
        BagEvent missing = bagEvent(facilityId, staffId, null);
        AppUser staff = new AppUser();
        staff.setId(staffId);
        staff.setUsername("driver-1");
        staff.setFullName("Driver One");
        ArgumentCaptor<Pageable> anomalyPageable = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Pageable> missingPageable = ArgumentCaptor.forClass(Pageable.class);
        when(bagEventRepo.findRecentAnomaliesByFacilityIdSince(eq(facilityId), any(Instant.class),
                anomalyPageable.capture())).thenReturn(List.of(anomaly));
        when(bagEventRepo.findMissingBags(eq(facilityId), any(Instant.class), missingPageable.capture()))
                .thenReturn(List.of(missing));
        when(userRepo.findAllById(any())).thenReturn(List.of(staff));

        var response = service().getAnomalyBags();

        assertEquals(2, response.size());
        assertEquals("Driver One", response.get(0).staffName());
        assertEquals("NOT_VERIFIED_AT_CBWTF", response.get(1).anomalyState());
        assertEquals(50, anomalyPageable.getValue().getPageSize());
        assertEquals(50, missingPageable.getValue().getPageSize());
        verify(bagEventRepo, never()).findByFacilityIdAndEventTsBetween(eq(facilityId), any(), any());
        verify(bagEventRepo, never()).findMissingBags(eq(facilityId), any(Instant.class));
        verify(userRepo, never()).findById(staffId);
    }

    private CBWTFDashboardService service() {
        return new CBWTFDashboardService(tenantAssertion, agreementRepo, bagEventRepo, bagLabelRepo,
                invoiceRepo, facilityRepo, userRepo);
    }

    private static Facility facility(UUID facilityId) {
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setCode("FAC");
        facility.setName("Facility");
        facility.setAddress("Address");
        return facility;
    }

    private static Agreement agreement(String number, LocalDate endDate) {
        Agreement agreement = new Agreement();
        agreement.setAgreementNumber(number);
        agreement.setStatusEnum(Agreement.Status.ACTIVE);
        agreement.setDuesStatusEnum(Agreement.DuesStatus.CLEAR);
        agreement.setEndDate(endDate);
        Hcf hcf = new Hcf();
        hcf.setName("HCF");
        agreement.setHcf(hcf);
        return agreement;
    }

    private static BagEvent bagEvent(UUID facilityId, UUID staffId, String anomalyState) {
        Facility facility = facility(facilityId);
        Hcf hcf = new Hcf();
        hcf.setName("HCF");
        BagLabel label = new BagLabel();
        label.setCategory("YELLOW");
        BagEvent event = new BagEvent();
        event.setId(UUID.randomUUID());
        event.setFacility(facility);
        event.setHcf(hcf);
        event.setBagLabel(label);
        event.setEventType("HCF_COLLECTION");
        event.setEventTs(Instant.now());
        event.setWeightKg(new BigDecimal("1.250"));
        event.setCollectedByUserId(staffId);
        event.setGpsLat(28.0);
        event.setGpsLon(77.0);
        event.setAnomalyState(anomalyState);
        return event;
    }
}
