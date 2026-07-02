package com.smartcbwtf.service;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Route;
import com.smartcbwtf.domain.RouteAssignment;
import com.smartcbwtf.domain.Vehicle;
import com.smartcbwtf.dto.route.AssignRouteRequest;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.RouteAssignmentRepository;
import com.smartcbwtf.repository.RouteRepository;
import com.smartcbwtf.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteAssignmentServiceSecurityTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private RouteAssignmentRepository assignmentRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private VehicleRepository vehicleRepository;

    private RouteAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new RouteAssignmentService(
                routeRepository,
                assignmentRepository,
                appUserRepository,
                vehicleRepository);
    }

    @Test
    void assignRouteRejectsStaffFromAnotherFacility() {
        UUID facilityId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        when(routeRepository.findByIdAndFacilityId(routeId, facilityId))
                .thenReturn(Optional.of(route(routeId, facilityId)));
        when(appUserRepository.findByIdAndFacilityIdAndRoleInAndActive(
                staffId, facilityId, assignableRoles(), true))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.assignRoute(routeId, facilityId, new AssignRouteRequest(staffId, null)));

        verify(appUserRepository).findByIdAndFacilityIdAndRoleInAndActive(
                staffId, facilityId, assignableRoles(), true);
        verify(appUserRepository, never()).findById(any(UUID.class));
        verify(assignmentRepository, never()).findByRouteIdAndIsActiveTrue(routeId);
        verify(assignmentRepository, never()).save(any(RouteAssignment.class));
    }

    @Test
    void assignRouteRejectsAdminUserEvenWithinFacility() {
        UUID facilityId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();

        when(routeRepository.findByIdAndFacilityId(routeId, facilityId))
                .thenReturn(Optional.of(route(routeId, facilityId)));
        when(appUserRepository.findByIdAndFacilityIdAndRoleInAndActive(
                staffId, facilityId, assignableRoles(), true))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.assignRoute(routeId, facilityId, new AssignRouteRequest(staffId, null)));

        verify(appUserRepository).findByIdAndFacilityIdAndRoleInAndActive(
                staffId, facilityId, assignableRoles(), true);
        verify(appUserRepository, never()).findById(any(UUID.class));
        verify(assignmentRepository, never()).findByRouteIdAndIsActiveTrue(routeId);
        verify(assignmentRepository, never()).save(any(RouteAssignment.class));
    }

    @Test
    void assignRouteRejectsVehicleFromAnotherFacility() {
        UUID facilityId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(routeRepository.findByIdAndFacilityId(routeId, facilityId))
                .thenReturn(Optional.of(route(routeId, facilityId)));
        when(appUserRepository.findByIdAndFacilityIdAndRoleInAndActive(
                staffId, facilityId, assignableRoles(), true))
                .thenReturn(Optional.of(staff(staffId, facilityId, StaffService.ROLE_DRIVER, true)));
        when(vehicleRepository.findByIdAndFacilityIdAndStatus(vehicleId, facilityId, "ACTIVE"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.assignRoute(routeId, facilityId, new AssignRouteRequest(staffId, vehicleId)));

        verify(appUserRepository, never()).findById(any(UUID.class));
        verify(vehicleRepository).findByIdAndFacilityIdAndStatus(vehicleId, facilityId, "ACTIVE");
        verify(vehicleRepository, never()).findById(any(UUID.class));
        verify(assignmentRepository, never()).findByRouteIdAndIsActiveTrue(routeId);
        verify(assignmentRepository, never()).save(any(RouteAssignment.class));
    }

    @Test
    void assignRouteUsesOnlyFacilityOwnedActiveStaffAndVehicle() {
        UUID facilityId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        Route route = route(routeId, facilityId);
        AppUser staff = staff(staffId, facilityId, StaffService.ROLE_DRIVER, true);
        Vehicle vehicle = vehicle(vehicleId, facilityId, "ACTIVE");

        when(routeRepository.findByIdAndFacilityId(routeId, facilityId)).thenReturn(Optional.of(route));
        when(appUserRepository.findByIdAndFacilityIdAndRoleInAndActive(staffId, facilityId, assignableRoles(), true))
                .thenReturn(Optional.of(staff));
        when(vehicleRepository.findByIdAndFacilityIdAndStatus(vehicleId, facilityId, "ACTIVE"))
                .thenReturn(Optional.of(vehicle));
        when(assignmentRepository.findByRouteIdAndIsActiveTrue(routeId)).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(RouteAssignment.class))).thenAnswer(invocation -> {
            RouteAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        service.assignRoute(routeId, facilityId, new AssignRouteRequest(staffId, vehicleId));

        ArgumentCaptor<RouteAssignment> assignment = ArgumentCaptor.forClass(RouteAssignment.class);
        verify(assignmentRepository).save(assignment.capture());
        assertEquals(route, assignment.getValue().getRoute());
        assertEquals(staff, assignment.getValue().getStaff());
        assertEquals(vehicle, assignment.getValue().getVehicle());
        verify(appUserRepository, never()).findById(any(UUID.class));
        verify(vehicleRepository, never()).findById(any(UUID.class));
    }

    private static java.util.Set<String> assignableRoles() {
        return java.util.Set.of(StaffService.ROLE_DRIVER, StaffService.ROLE_PLANT_OPERATOR);
    }

    private static Route route(UUID id, UUID facilityId) {
        Route route = new Route();
        route.setId(id);
        route.setName("North Route");
        route.setFacility(facility(facilityId));
        return route;
    }

    private static AppUser staff(UUID id, UUID facilityId, String role, boolean active) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setFacility(facility(facilityId));
        user.setRole(role);
        user.setActive(active);
        user.setName("Driver One");
        return user;
    }

    private static Vehicle vehicle(UUID id, UUID facilityId, String status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setFacility(facility(facilityId));
        vehicle.setRegistrationNumber("MH12AB1234");
        vehicle.setStatus(status);
        return vehicle;
    }

    private static Facility facility(UUID id) {
        Facility facility = new Facility();
        facility.setId(id);
        facility.setCode("CBWTF");
        facility.setName("CBWTF");
        facility.setAddress("Address");
        return facility;
    }
}
