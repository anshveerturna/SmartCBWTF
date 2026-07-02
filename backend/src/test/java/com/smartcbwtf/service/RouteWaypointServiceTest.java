package com.smartcbwtf.service;

import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.Route;
import com.smartcbwtf.domain.RouteWaypoint;
import com.smartcbwtf.dto.route.SetWaypointsRequest;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.RouteRepository;
import com.smartcbwtf.repository.RouteWaypointRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteWaypointServiceTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private RouteWaypointRepository waypointRepository;
    @Mock
    private HcfRepository hcfRepository;

    private RouteWaypointService service;
    private UUID routeId;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        service = new RouteWaypointService(routeRepository, waypointRepository, hcfRepository);
        routeId = UUID.randomUUID();
        facilityId = UUID.randomUUID();
    }

    @Test
    void setWaypointsLoadsOnlyActiveHcfsForRouteFacility() {
        UUID firstHcfId = UUID.randomUUID();
        UUID secondHcfId = UUID.randomUUID();
        Route route = route(routeId, "North Route");
        Hcf first = hcf(firstHcfId, "HCF-001", "Clinic One");
        Hcf second = hcf(secondHcfId, "HCF-002", "Clinic Two");

        when(routeRepository.findByIdAndFacilityId(routeId, facilityId)).thenReturn(Optional.of(route));
        when(hcfRepository.findActiveByFacilityIdAndIdIn(facilityId, List.of(firstHcfId, secondHcfId)))
                .thenReturn(List.of(second, first));
        when(waypointRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.setWaypoints(routeId, facilityId, new SetWaypointsRequest(List.of(firstHcfId, secondHcfId)));

        assertEquals(2, result.size());
        assertEquals(firstHcfId, result.get(0).hcfId());
        assertEquals(1, result.get(0).sequenceOrder());
        assertEquals(secondHcfId, result.get(1).hcfId());
        assertEquals(2, result.get(1).sequenceOrder());
        verify(hcfRepository).findActiveByFacilityIdAndIdIn(facilityId, List.of(firstHcfId, secondHcfId));
        verify(hcfRepository, never()).findAllById(any());
        verify(waypointRepository).deleteAllByRouteId(routeId);
    }

    @Test
    void setWaypointsRejectsForeignHcfIdsBeforeChangingRoute() {
        UUID localHcfId = UUID.randomUUID();
        UUID foreignHcfId = UUID.randomUUID();
        Route route = route(routeId, "North Route");

        when(routeRepository.findByIdAndFacilityId(routeId, facilityId)).thenReturn(Optional.of(route));
        when(hcfRepository.findActiveByFacilityIdAndIdIn(facilityId, List.of(localHcfId, foreignHcfId)))
                .thenReturn(List.of(hcf(localHcfId, "HCF-001", "Clinic One")));

        assertThrows(EntityNotFoundException.class,
                () -> service.setWaypoints(routeId, facilityId,
                        new SetWaypointsRequest(List.of(localHcfId, foreignHcfId))));

        verify(hcfRepository).findActiveByFacilityIdAndIdIn(facilityId, List.of(localHcfId, foreignHcfId));
        verify(hcfRepository, never()).findAllById(any());
        verify(waypointRepository, never()).deleteAllByRouteId(any());
        verify(waypointRepository, never()).saveAll(any());
    }

    private static Route route(UUID id, String name) {
        Route route = new Route();
        route.setId(id);
        route.setName(name);
        return route;
    }

    private static Hcf hcf(UUID id, String code, String name) {
        Hcf hcf = new Hcf();
        hcf.setId(id);
        hcf.setCode(code);
        hcf.setName(name);
        hcf.setAddress("123 Main Road");
        hcf.setGpsLat(28.6139);
        hcf.setGpsLon(77.2090);
        hcf.setStatus("ACTIVE");
        return hcf;
    }
}
