package com.smartcbwtf.service;

import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.FacilityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class BillingSchedulerTest {

    @Mock
    private BillGenerationService billGenerationService;

    @Mock
    private FacilityRepository facilityRepository;

    @Test
    void monthlyBillingProcessesFacilitiesAcrossPages() {
        Facility first = facility("First CBWTF");
        Facility second = facility("Second CBWTF");
        when(facilityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first), PageRequest.of(0, 1), 2))
                .thenReturn(new PageImpl<>(List.of(second), PageRequest.of(1, 1), 2));
        when(billGenerationService.generateBillsForMonth(eq(first.getId()), any(LocalDate.class), eq(null)))
                .thenReturn(2);
        when(billGenerationService.generateBillsForMonth(eq(second.getId()), any(LocalDate.class), eq(null)))
                .thenReturn(3);

        new BillingScheduler(billGenerationService, facilityRepository).generateMonthlyBills();

        verify(billGenerationService).generateBillsForMonth(eq(first.getId()), any(LocalDate.class), eq(null));
        verify(billGenerationService).generateBillsForMonth(eq(second.getId()), any(LocalDate.class), eq(null));
        verify(facilityRepository, times(2)).findAll(any(Pageable.class));
        verify(facilityRepository, never()).findAll();
    }

    @Test
    void monthlyBillingContinuesAfterFacilityFailure() {
        Facility failed = facility("Failing CBWTF");
        Facility successful = facility("Healthy CBWTF");
        when(facilityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(failed, successful)));
        when(billGenerationService.generateBillsForMonth(eq(failed.getId()), any(LocalDate.class), eq(null)))
                .thenThrow(new IllegalStateException("billing failed"));

        new BillingScheduler(billGenerationService, facilityRepository).generateMonthlyBills();

        verify(billGenerationService).generateBillsForMonth(eq(successful.getId()), any(LocalDate.class), eq(null));
        verify(facilityRepository, never()).findAll();
    }

    private static Facility facility(String name) {
        Facility facility = new Facility();
        facility.setId(UUID.randomUUID());
        facility.setName(name);
        return facility;
    }
}
