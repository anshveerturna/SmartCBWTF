package com.smartcbwtf.service;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.Route;
import com.smartcbwtf.domain.RouteAlert;
import com.smartcbwtf.domain.RouteCycleHistory;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.RouteAlertRepository;
import com.smartcbwtf.repository.RouteAssignmentRepository;
import com.smartcbwtf.repository.RouteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RouteServiceSecurityTest {

    private final RouteRepository routeRepository = mock(RouteRepository.class);
    private final RouteAssignmentRepository assignmentRepository = mock(RouteAssignmentRepository.class);
    private final FacilityRepository facilityRepository = mock(FacilityRepository.class);
    private final RouteExecutionService routeExecutionService = mock(RouteExecutionService.class);
    private final RouteAlertRepository routeAlertRepository = mock(RouteAlertRepository.class);
    private final RouteService service = new RouteService(routeRepository, assignmentRepository, facilityRepository,
            routeExecutionService, routeAlertRepository);

    @Test
    void resolveAlertUsesFacilityScopedLookup() {
        UUID alertId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        RouteAlert alert = alert(alertId);
        when(routeAlertRepository.findByIdAndFacilityId(alertId, facilityId)).thenReturn(Optional.of(alert));
        when(routeAlertRepository.save(alert)).thenReturn(alert);

        service.resolveAlert(alertId, facilityId, "checked with driver");

        verify(routeAlertRepository).findByIdAndFacilityId(alertId, facilityId);
        verify(routeAlertRepository).save(alert);
    }

    @Test
    void resolveAlertDoesNotSaveCrossTenantAlert() {
        UUID alertId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        when(routeAlertRepository.findByIdAndFacilityId(alertId, facilityId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.resolveAlert(alertId, facilityId, "not ours"));

        verify(routeAlertRepository).findByIdAndFacilityId(alertId, facilityId);
        verify(routeAlertRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getUnresolvedAlertsUsesBoundedPageRequest() {
        UUID facilityId = UUID.randomUUID();
        when(routeExecutionService.getUnresolvedAlerts(
                org.mockito.ArgumentMatchers.eq(facilityId), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of());

        service.getUnresolvedAlerts(facilityId, 1000);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(routeExecutionService).getUnresolvedAlerts(
                org.mockito.ArgumentMatchers.eq(facilityId), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
    }

    private static RouteAlert alert(UUID alertId) {
        Route route = mock(Route.class);
        when(route.getId()).thenReturn(UUID.randomUUID());
        when(route.getName()).thenReturn("Route A");
        when(route.getColor()).thenReturn("#3366ff");

        RouteCycleHistory cycle = mock(RouteCycleHistory.class);
        when(cycle.getId()).thenReturn(UUID.randomUUID());
        when(cycle.getCycleNumber()).thenReturn(1);

        AppUser staff = mock(AppUser.class);
        when(staff.getId()).thenReturn(UUID.randomUUID());
        when(staff.getName()).thenReturn("Driver One");

        RouteAlert alert = new RouteAlert();
        alert.setId(alertId);
        alert.setRoute(route);
        alert.setCycle(cycle);
        alert.setStaff(staff);
        alert.setAlertType(RouteAlert.AlertType.WAYPOINT_MISSED);
        alert.setSeverity(RouteAlert.Severity.WARNING);
        alert.setTitle("Missed stop");
        alert.setMessage("A stop was missed");
        alert.setMissedHcfCount(1);
        alert.setIsResolved(false);
        alert.setResolvedAt(Instant.now());
        return alert;
    }
}
