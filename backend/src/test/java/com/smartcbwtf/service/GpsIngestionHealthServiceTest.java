package com.smartcbwtf.service;

import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.GpsIngestionLog;
import com.smartcbwtf.repository.GpsIngestionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GpsIngestionHealthServiceTest {

    @Mock
    private GpsIngestionLogRepository repository;

    @Test
    void allHealthUsesBoundedFetchInsteadOfUnboundedFindAll() {
        GpsIngestionHealthService service = new GpsIngestionHealthService(repository);
        GpsIngestionLog log = healthyLog(UUID.randomUUID(), "CBWTF One", "WHEELSEYE");
        when(repository.findAllByOrderByUpdatedAtDesc(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(log));

        List<GpsIngestionHealthService.IngestionHealthDTO> result = service.getAllHealth();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByOrderByUpdatedAtDesc(pageableCaptor.capture());
        verify(repository, never()).findAll();
        assertEquals(500, pageableCaptor.getValue().getPageSize());
        assertEquals(1, result.size());
        assertEquals("CBWTF One", result.get(0).facilityName());
    }

    @Test
    void facilityHealthUsesFacilityScopedOrderedQuery() {
        GpsIngestionHealthService service = new GpsIngestionHealthService(repository);
        UUID facilityId = UUID.randomUUID();
        when(repository.findByFacilityIdOrderByVendorAsc(facilityId))
                .thenReturn(List.of(healthyLog(facilityId, "CBWTF Two", "GENERIC")));

        List<GpsIngestionHealthService.IngestionHealthDTO> result = service.getHealthByFacility(facilityId);

        verify(repository).findByFacilityIdOrderByVendorAsc(facilityId);
        verify(repository, never()).findByFacilityId(facilityId);
        assertEquals(1, result.size());
        assertEquals("GENERIC", result.get(0).vendor());
    }

    private GpsIngestionLog healthyLog(UUID facilityId, String facilityName, String vendor) {
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setName(facilityName);

        GpsIngestionLog log = new GpsIngestionLog();
        log.setFacility(facility);
        log.setVendor(vendor);
        log.recordSuccess(10);
        return log;
    }
}
