package com.smartcbwtf.service;

import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.DailyWasteSnapshotRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.MonthlyWasteSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsAggregationServiceTest {

    @Mock
    private BagEventRepository bagEventRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private DailyWasteSnapshotRepository dailySnapshotRepository;
    @Mock
    private MonthlyWasteSnapshotRepository monthlySnapshotRepository;

    private AnalyticsAggregationService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsAggregationService(
                bagEventRepository,
                facilityRepository,
                dailySnapshotRepository,
                monthlySnapshotRepository);
    }

    @Test
    void dailyAggregationProcessesFacilitiesAcrossPages() {
        Facility first = facility();
        Facility second = facility();
        LocalDate date = LocalDate.of(2026, 7, 1);
        when(facilityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first), PageRequest.of(0, 1), 2))
                .thenReturn(new PageImpl<>(List.of(second), PageRequest.of(1, 1), 2));
        when(bagEventRepository.findByFacilityIdAndEventTsBetween(any(UUID.class), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        service.aggregateDailyForDate(date);

        verify(bagEventRepository).findByFacilityIdAndEventTsBetween(eq(first.getId()), any(), any());
        verify(bagEventRepository).findByFacilityIdAndEventTsBetween(eq(second.getId()), any(), any());
        verify(facilityRepository, times(2)).findAll(any(Pageable.class));
        verify(facilityRepository, never()).findAll();
    }

    @Test
    void monthlyAggregationUsesPagedFacilityTraversal() {
        Facility facility = facility();
        LocalDate month = LocalDate.of(2026, 6, 1);
        when(facilityRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(facility)));
        when(dailySnapshotRepository.findByFacilityIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                eq(facility.getId()), eq(month), eq(month.plusMonths(1).minusDays(1))))
                .thenReturn(List.of());

        service.aggregateMonthlyForMonth(month);

        verify(dailySnapshotRepository).findByFacilityIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                eq(facility.getId()), eq(month), eq(month.plusMonths(1).minusDays(1)));
        verify(facilityRepository).findAll(any(Pageable.class));
        verify(facilityRepository, never()).findAll();
    }

    private static Facility facility() {
        Facility facility = new Facility();
        facility.setId(UUID.randomUUID());
        facility.setName("Smart CBWTF");
        return facility;
    }
}
