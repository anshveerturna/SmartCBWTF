package com.smartcbwtf.service;

import com.smartcbwtf.domain.DailyComplianceReport;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.DailyComplianceReportRepository;
import com.smartcbwtf.repository.FacilityRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyReportGenerationServiceTest {

    @Mock
    DailyComplianceReportRepository reportRepository;
    @Mock
    ReportGenerationLockService lockService;
    @Mock
    FacilityRepository facilityRepository;
    @Mock
    ComplianceDataAggregator aggregator;
    @Mock
    ComplianceReportExportService reportExportService;
    @Mock
    AuditLogService auditLogService;
    @Mock
    TransactionTemplate transactionTemplate;

    DailyReportGenerationService service;

    @BeforeEach
    void setUp() {
        service = new DailyReportGenerationService(
                reportRepository,
                lockService,
                facilityRepository,
                aggregator,
                reportExportService,
                auditLogService,
                transactionTemplate);
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Boolean> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void generateReportStoresPdfBytesAtCreation() {
        UUID facilityId = UUID.randomUUID();
        LocalDate reportDate = LocalDate.of(2026, 6, 30);
        Facility facility = new Facility();
        facility.setId(facilityId);
        byte[] pdfBytes = new byte[] { 1, 2, 3, 4 };

        ComplianceDataAggregator.DailyAggregation aggregation = new ComplianceDataAggregator.DailyAggregation(
                reportDate,
                Instant.parse("2026-06-29T18:30:00Z"),
                Instant.parse("2026-06-30T18:29:59Z"),
                BigDecimal.ZERO,
                Map.of("YELLOW", BigDecimal.ZERO),
                0,
                0,
                0,
                0,
                List.of(),
                false);

        when(lockService.acquire("DAILY", reportDate.toString(), facilityId)).thenReturn(true);
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(aggregator.aggregateDaily(eq(facilityId), eq(reportDate), any(Instant.class), any(Instant.class)))
                .thenReturn(aggregation);
        when(aggregator.toJson(aggregation)).thenReturn("{\"totalWasteKg\":0}");
        when(reportExportService.dailyPdf(any(DailyComplianceReport.class))).thenReturn(pdfBytes);

        boolean generated = service.generateReport(facilityId, reportDate);

        assertEquals(true, generated);
        ArgumentCaptor<DailyComplianceReport> reportCaptor = ArgumentCaptor.forClass(DailyComplianceReport.class);
        verify(reportRepository).save(reportCaptor.capture());
        DailyComplianceReport saved = reportCaptor.getValue();
        assertArrayEquals(pdfBytes, saved.getPdfBytes());
        assertEquals(DailyComplianceReport.Status.READY, saved.getStatus());
        assertEquals(DailyComplianceReport.DataCompleteness.COMPLETE, saved.getDataCompleteness());
        assertNotNull(saved.getChecksum());
        assertEquals(64, saved.getChecksum().length());
        verify(reportExportService).dailyPdf(saved);
        verify(lockService).release("DAILY", reportDate.toString(), facilityId);
    }

    @Test
    void generateReportsForAllFacilitiesUsesPagedFacilityTraversal() {
        LocalDate reportDate = LocalDate.of(2026, 6, 30);
        Facility first = facility();
        Facility second = facility();
        when(facilityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first), PageRequest.of(0, 1), 2))
                .thenReturn(new PageImpl<>(List.of(second), PageRequest.of(1, 1), 2));
        when(lockService.acquire("DAILY", reportDate.toString(), first.getId())).thenReturn(false);
        when(lockService.acquire("DAILY", reportDate.toString(), second.getId())).thenReturn(false);

        int generated = service.generateReportsForAllFacilities(reportDate);

        assertEquals(0, generated);
        verify(lockService).acquire("DAILY", reportDate.toString(), first.getId());
        verify(lockService).acquire("DAILY", reportDate.toString(), second.getId());
        verify(facilityRepository, times(2)).findAll(any(Pageable.class));
        verify(facilityRepository, never()).findAll();
    }

    private static Facility facility() {
        Facility facility = new Facility();
        facility.setId(UUID.randomUUID());
        return facility;
    }
}
