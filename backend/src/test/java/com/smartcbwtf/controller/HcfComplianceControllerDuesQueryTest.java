package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.DuesClearanceRequest;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.DuesClearanceRequestRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.service.PdfService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcfComplianceControllerDuesQueryTest {

    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private BagEventRepository bagEventRepository;
    @Mock
    private BagLabelRepository bagLabelRepository;
    @Mock
    private DuesClearanceRequestRepository duesRequestRepository;
    @Mock
    private HcfAccessGuard accessGuard;
    @Mock
    private PdfService pdfService;

    private HcfComplianceController controller;
    private UUID hcfId;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        controller = new HcfComplianceController(
                hcfRepository,
                agreementRepository,
                bagEventRepository,
                bagLabelRepository,
                duesRequestRepository,
                accessGuard,
                pdfService);
        hcfId = UUID.randomUUID();
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void requestDuesClearanceUsesPeriodScopedExistenceChecks() {
        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        Facility facility = new Facility();
        facility.setId(facilityId);
        Agreement agreement = new Agreement();
        agreement.setHcf(hcf);
        agreement.setFacility(facility);

        when(hcfRepository.findByIdAndFacilityId(hcfId, facilityId)).thenReturn(Optional.of(hcf));
        when(duesRequestRepository.existsByHcfIdAndFacilityIdAndRequestMonthAndRequestYearAndManagementStatus(
                hcfId, facilityId, 6, 2026, DuesClearanceRequest.Status.APPROVED.name())).thenReturn(false);
        when(duesRequestRepository.existsByHcfIdAndFacilityIdAndRequestMonthAndRequestYearAndManagementStatusIn(
                hcfId, facilityId, 6, 2026,
                List.of(DuesClearanceRequest.Status.PENDING.name(), DuesClearanceRequest.Status.SUBMITTED.name())))
                .thenReturn(false);
        when(agreementRepository.findActiveByHcfAndFacility(hcfId, facilityId)).thenReturn(Optional.of(agreement));

        controller.requestDuesClearance(new HcfComplianceController.DuesAccessRequest(6, 2026));

        verify(duesRequestRepository).save(any(DuesClearanceRequest.class));
        verify(hcfRepository).findByIdAndFacilityId(hcfId, facilityId);
        verify(agreementRepository).findActiveByHcfAndFacility(hcfId, facilityId);
        verify(hcfRepository, never()).findById(hcfId);
        verify(agreementRepository, never()).findByHcfIdAndStatus(hcfId, Agreement.Status.ACTIVE.name());
        verify(duesRequestRepository, never()).findByHcfIdOrderByRequestedAtDesc(hcfId);
    }

    @Test
    void requestDuesClearanceRejectsFuturePeriodBeforeRepositoryQueries() {
        YearMonth futurePeriod = futurePeriod();

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.requestDuesClearance(new HcfComplianceController.DuesAccessRequest(
                        futurePeriod.getMonthValue(),
                        futurePeriod.getYear())));

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
        verifyNoInteractions(hcfRepository, agreementRepository, bagEventRepository, bagLabelRepository,
                duesRequestRepository, pdfService);
    }

    @Test
    void monthlyDataUsesPeriodScopedLatestRequestLookup() {
        DuesClearanceRequest request = new DuesClearanceRequest();
        request.setManagementStatusEnum(DuesClearanceRequest.Status.APPROVED);
        when(bagEventRepository.sumWeightByFacilityIdAndHcfIdAndEventTsBetween(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("12.50"));
        when(duesRequestRepository.findTopByHcfIdAndFacilityIdAndRequestMonthAndRequestYearOrderByRequestedAtDesc(
                hcfId, facilityId, 6, 2026)).thenReturn(Optional.of(request));

        var response = controller.getMonthlyData(2026, 6);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("APPROVED", body.get("accessStatus"));
        verify(duesRequestRepository).findTopByHcfIdAndFacilityIdAndRequestMonthAndRequestYearOrderByRequestedAtDesc(
                hcfId, facilityId, 6, 2026);
        verify(bagEventRepository, never()).sumWeightByHcfIdAndEventTsBetween(
                eq(hcfId), any(Instant.class), any(Instant.class));
        verify(duesRequestRepository, never()).findByHcfIdOrderByRequestedAtDesc(hcfId);
    }

    @Test
    void monthlyDataRejectsInvalidMonthBeforeRepositoryQueries() {
        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.getMonthlyData(2026, 13));

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
        verifyNoInteractions(hcfRepository, agreementRepository, bagEventRepository, bagLabelRepository,
                duesRequestRepository, pdfService);
    }

    @Test
    void monthlyPdfDenialUsesPeriodScopedApprovalCheck() {
        when(duesRequestRepository.existsByHcfIdAndFacilityIdAndRequestMonthAndRequestYearAndManagementStatus(
                hcfId, facilityId, 6, 2026, DuesClearanceRequest.Status.APPROVED.name())).thenReturn(false);

        assertThrows(HcfComplianceController.AccessDeniedException.class,
                () -> controller.downloadMonthlyReportPdf(2026, 6));

        verify(duesRequestRepository).existsByHcfIdAndFacilityIdAndRequestMonthAndRequestYearAndManagementStatus(
                hcfId, facilityId, 6, 2026, DuesClearanceRequest.Status.APPROVED.name());
        verify(duesRequestRepository, never()).existsByHcfIdAndRequestMonthAndRequestYearAndManagementStatus(
                hcfId, 6, 2026, DuesClearanceRequest.Status.APPROVED.name());
        verifyNoInteractions(hcfRepository, agreementRepository, bagEventRepository, pdfService);
    }

    @Test
    void monthlyPdfRejectsFuturePeriodBeforeApprovalLookup() {
        YearMonth futurePeriod = futurePeriod();

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.downloadMonthlyReportPdf(futurePeriod.getYear(), futurePeriod.getMonthValue()));

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
        verifyNoInteractions(hcfRepository, agreementRepository, bagEventRepository, bagLabelRepository,
                duesRequestRepository, pdfService);
    }

    @Test
    void monthlyPdfDownloadUsesNoStoreCacheHeaderWhenApproved() {
        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        hcf.setCode("HCF-001");
        Facility facility = new Facility();
        facility.setId(facilityId);
        Agreement agreement = new Agreement();
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        byte[] pdf = new byte[] { 1, 2, 3 };
        when(duesRequestRepository.existsByHcfIdAndFacilityIdAndRequestMonthAndRequestYearAndManagementStatus(
                hcfId, facilityId, 6, 2026, DuesClearanceRequest.Status.APPROVED.name())).thenReturn(true);
        when(hcfRepository.findByIdAndFacilityId(hcfId, facilityId)).thenReturn(Optional.of(hcf));
        when(agreementRepository.findActiveByHcfAndFacility(hcfId, facilityId)).thenReturn(Optional.of(agreement));
        when(bagEventRepository.findByFacilityIdAndHcfIdAndEventTsBetween(
                eq(facilityId), eq(hcfId), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(pdfService.generateMonthlyCompliancePdf(eq(agreement), any(LocalDate.class), eq(List.of())))
                .thenReturn(pdf);

        var response = controller.downloadMonthlyReportPdf(2026, 6);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertArrayEquals(pdf, (byte[]) response.getBody());
    }

    @Test
    void cancelDuesRequestDoesNotResetGlobalHcfDuesStatus() {
        DuesClearanceRequest request = new DuesClearanceRequest();
        request.setRequestMonth(6);
        request.setRequestYear(2026);
        when(duesRequestRepository.findByHcfIdAndFacilityIdAndManagementStatusIn(
                hcfId,
                facilityId,
                List.of(DuesClearanceRequest.Status.PENDING.name(), DuesClearanceRequest.Status.SUBMITTED.name())))
                .thenReturn(List.of(request));

        controller.cancelDuesRequest();

        verify(duesRequestRepository).deleteAll(List.of(request));
        verifyNoInteractions(hcfRepository);
    }

    @Test
    void yearlyDataRejectsFutureYearBeforeAccessAndRepositoryQueries() {
        int futureYear = YearMonth.now(ZoneId.of("Asia/Kolkata")).plusYears(1).getYear();

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.getYearlyData(futureYear));

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
        verifyNoInteractions(accessGuard, hcfRepository, agreementRepository, bagEventRepository, bagLabelRepository,
                duesRequestRepository, pdfService);
    }

    private static YearMonth futurePeriod() {
        return YearMonth.now(ZoneId.of("Asia/Kolkata")).plusMonths(1);
    }
}
