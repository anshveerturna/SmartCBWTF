package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Route;
import com.smartcbwtf.domain.RouteAssignment;
import com.smartcbwtf.dto.mobile.MobileRouteDTO;
import com.smartcbwtf.repository.AttendanceRepository;
import com.smartcbwtf.repository.RouteAssignmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileRouteControllerSecurityTest {

    @Mock
    private RouteAssignmentRepository assignmentRepository;
    @Mock
    private AttendanceRepository attendanceRepository;

    private MobileRouteController controller;
    private UUID staffId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        controller = new MobileRouteController(assignmentRepository, attendanceRepository);
        staffId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(staffId, tenantId, null, "DRIVER", "driver"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void myRouteMasksCrossTenantAssignment() {
        when(assignmentRepository.findActiveAssignmentByStaffIdWithRouteDetails(staffId))
                .thenReturn(Optional.of(assignment(UUID.randomUUID())));

        ResponseEntity<MobileRouteDTO> response = controller.getMyRoute();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(assignmentRepository).findActiveAssignmentByStaffIdWithRouteDetails(staffId);
        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void myRouteReturnsCurrentTenantAssignment() {
        when(assignmentRepository.findActiveAssignmentByStaffIdWithRouteDetails(staffId))
                .thenReturn(Optional.of(assignment(tenantId)));

        ResponseEntity<MobileRouteDTO> response = controller.getMyRoute();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        MobileRouteDTO body = response.getBody();
        assertNotNull(body);
        assertEquals("Assigned Route", body.routeName());
        assertEquals("Tenant Facility", body.facilityName());
        assertEquals(0, body.waypoints().size());
        verifyNoInteractions(attendanceRepository);
    }

    private RouteAssignment assignment(UUID facilityId) {
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setName("Tenant Facility");
        facility.setCode("FAC");
        facility.setAddress("Address");

        Route route = new Route();
        route.setId(UUID.randomUUID());
        route.setFacility(facility);
        route.setName("Assigned Route");
        route.setColor("#2563EB");
        route.setCompletionDays(2);

        RouteAssignment assignment = new RouteAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setRoute(route);
        return assignment;
    }
}
