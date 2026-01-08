package com.smartcbwtf.service;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.Route;
import com.smartcbwtf.domain.RouteAssignment;
import com.smartcbwtf.domain.Vehicle;
import com.smartcbwtf.dto.route.AssignRouteRequest;
import com.smartcbwtf.dto.route.RouteAssignmentDTO;
import com.smartcbwtf.repository.RouteAssignmentRepository;
import com.smartcbwtf.repository.RouteRepository;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing route assignments.
 * Assignments are detachable and replaceable. History is preserved.
 */
@Service
public class RouteAssignmentService {

        private static final Logger log = LoggerFactory.getLogger(RouteAssignmentService.class);

        private final RouteRepository routeRepository;
        private final RouteAssignmentRepository assignmentRepository;
        private final AppUserRepository appUserRepository;
        private final VehicleRepository vehicleRepository;

        public RouteAssignmentService(
                        RouteRepository routeRepository,
                        RouteAssignmentRepository assignmentRepository,
                        AppUserRepository appUserRepository,
                        VehicleRepository vehicleRepository) {
                this.routeRepository = routeRepository;
                this.assignmentRepository = assignmentRepository;
                this.appUserRepository = appUserRepository;
                this.vehicleRepository = vehicleRepository;
        }

        /**
         * Assign or reassign a route to staff.
         * If there's an existing active assignment, it will be ended.
         */
        @Transactional
        public RouteAssignmentDTO assignRoute(UUID routeId, UUID facilityId, AssignRouteRequest request) {
                Route route = routeRepository.findByIdAndFacilityId(routeId, facilityId)
                                .orElseThrow(() -> new EntityNotFoundException("Route not found: " + routeId));

                AppUser staff = appUserRepository.findById(request.staffId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Staff not found: " + request.staffId()));

                Vehicle vehicle = null;
                if (request.vehicleId() != null) {
                        vehicle = vehicleRepository.findById(request.vehicleId())
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                        "Vehicle not found: " + request.vehicleId()));
                }

                // End existing active assignment if any
                assignmentRepository.findByRouteIdAndIsActiveTrue(routeId)
                                .ifPresent(existing -> {
                                        existing.endAssignment();
                                        assignmentRepository.saveAndFlush(existing);
                                        log.info("Ended previous assignment for route '{}' (staff: {})",
                                                        route.getName(), existing.getStaff().getName());
                                });

                // Create new assignment
                RouteAssignment assignment = new RouteAssignment();
                assignment.setRoute(route);
                assignment.setStaff(staff);
                assignment.setVehicle(vehicle);
                assignment.setIsActive(true);

                assignment = assignmentRepository.save(assignment);
                log.info("Assigned route '{}' to staff '{}'{}",
                                route.getName(),
                                staff.getName(),
                                vehicle != null ? " with vehicle " + vehicle.getRegistrationNumber() : "");

                return RouteAssignmentDTO.from(assignment);
        }

        /**
         * Unassign a route (end current assignment).
         */
        @Transactional
        public void unassignRoute(UUID routeId, UUID facilityId) {
                Route route = routeRepository.findByIdAndFacilityId(routeId, facilityId)
                                .orElseThrow(() -> new EntityNotFoundException("Route not found: " + routeId));

                assignmentRepository.findByRouteIdAndIsActiveTrue(routeId)
                                .ifPresent(existing -> {
                                        existing.endAssignment();
                                        assignmentRepository.save(existing);
                                        log.info("Unassigned route '{}' (previous staff: {})",
                                                        route.getName(), existing.getStaff().getName());
                                });
        }

        /**
         * Get current active assignment for a route.
         */
        @Transactional(readOnly = true)
        public RouteAssignmentDTO getCurrentAssignment(UUID routeId) {
                return assignmentRepository.findActiveAssignmentWithDetails(routeId)
                                .map(RouteAssignmentDTO::from)
                                .orElse(null);
        }

        /**
         * Get assignment history for a route.
         */
        @Transactional(readOnly = true)
        public List<RouteAssignmentDTO> getAssignmentHistory(UUID routeId) {
                return assignmentRepository.findAssignmentHistoryWithDetails(routeId)
                                .stream()
                                .map(RouteAssignmentDTO::from)
                                .toList();
        }
}
