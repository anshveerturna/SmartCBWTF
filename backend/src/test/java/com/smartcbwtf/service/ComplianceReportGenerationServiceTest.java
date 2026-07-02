package com.smartcbwtf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcbwtf.domain.AnnualComplianceReport;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.AnnualComplianceReportRepository;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.BarcodeComplianceReportRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.MonthlyComplianceReportRepository;
import com.smartcbwtf.repository.ViolationReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceReportGenerationServiceTest {

    @Mock
    private MonthlyComplianceReportRepository monthlyReportRepository;
    @Mock
    private AnnualComplianceReportRepository annualReportRepository;
    @Mock
    private BarcodeComplianceReportRepository barcodeReportRepository;
    @Mock
    private ViolationReportRepository violationReportRepository;
    @Mock
    private ReportGenerationLockService lockService;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private BagEventRepository bagEventRepository;
    @Mock
    private BagLabelRepository bagLabelRepository;
    @Mock
    private ComplianceDataAggregator aggregator;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ComplianceReportExportService exportService;
    @Mock
    private TransactionTemplate transactionTemplate;

    private ComplianceReportGenerationService service;

    @BeforeEach
    void setUp() {
        service = new ComplianceReportGenerationService(
                monthlyReportRepository,
                annualReportRepository,
                barcodeReportRepository,
                violationReportRepository,
                lockService,
                facilityRepository,
                bagEventRepository,
                bagLabelRepository,
                aggregator,
                auditLogService,
                exportService,
                new ObjectMapper(),
                transactionTemplate);
    }

    @Test
    void monthlyReportGenerationProcessesFacilitiesAcrossPages() {
        Facility first = facility();
        Facility second = facility();
        LocalDate month = LocalDate.of(2026, 6, 1);
        when(facilityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first), PageRequest.of(0, 1), 2))
                .thenReturn(new PageImpl<>(List.of(second), PageRequest.of(1, 1), 2));
        when(lockService.acquire(eq("MONTHLY"), eq(month.toString()), any(UUID.class))).thenReturn(false);

        int generated = service.generateMonthlyReportsForAllFacilities(month);

        assertEquals(0, generated);
        verify(lockService).acquire("MONTHLY", month.toString(), first.getId());
        verify(lockService).acquire("MONTHLY", month.toString(), second.getId());
        verify(facilityRepository, times(2)).findAll(any(Pageable.class));
        verify(facilityRepository, never()).findAll();
    }

    @Test
    void barcodeAndViolationReportGenerationUsePagedFacilityTraversal() {
        Facility facility = facility();
        LocalDate reportDate = LocalDate.of(2026, 7, 1);
        when(facilityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(facility)))
                .thenReturn(new PageImpl<>(List.of(facility)));
        when(lockService.acquire(any(), eq(reportDate.toString()), eq(facility.getId()))).thenReturn(false);

        assertEquals(0, service.generateBarcodeReportsForAllFacilities(reportDate));
        assertEquals(0, service.generateViolationReportsForAllFacilities(reportDate));

        verify(lockService).acquire("BARCODE_DAILY", reportDate.toString(), facility.getId());
        verify(lockService).acquire("VIOLATION_DAILY", reportDate.toString(), facility.getId());
        verify(facilityRepository, times(2)).findAll(any(Pageable.class));
        verify(facilityRepository, never()).findAll();
    }

    @Test
    void annualReportGenerationStoresExcelBytes() {
        Facility facility = facility();
        LocalDate fyStart = LocalDate.of(2025, 4, 1);
        byte[] excelBytes = new byte[] { 1, 2, 3 };
        when(facilityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(facility)));
        when(lockService.acquire("ANNUAL", "2025-26", facility.getId())).thenReturn(true);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(aggregator.aggregateMonthly(eq(facility.getId()), eq(fyStart), any(), any()))
                .thenReturn(new ComplianceDataAggregator.MonthlyAggregation(
                        fyStart,
                        java.time.Instant.parse("2025-03-31T18:30:00Z"),
                        java.time.Instant.parse("2026-03-31T18:30:00Z"),
                        new BigDecimal("12.50"),
                        Map.of("YELLOW", new BigDecimal("12.50")),
                        Map.of(UUID.randomUUID(), new BigDecimal("12.50")),
                        1));
        when(exportService.annualExcel(any(AnnualComplianceReport.class))).thenReturn(excelBytes);

        int generated = service.generateAnnualReportsForAllFacilities(2025);

        ArgumentCaptor<AnnualComplianceReport> reportCaptor = ArgumentCaptor.forClass(AnnualComplianceReport.class);
        assertEquals(1, generated);
        verify(annualReportRepository).save(reportCaptor.capture());
        assertEquals("2025-26", reportCaptor.getValue().getFinancialYear());
        assertArrayEquals(excelBytes, reportCaptor.getValue().getExcelBytes());
        verify(exportService).annualExcel(reportCaptor.getValue());
    }

    private static Facility facility() {
        Facility facility = new Facility();
        facility.setId(UUID.randomUUID());
        facility.setName("Smart CBWTF");
        return facility;
    }
}
