package com.smartcbwtf.dto.route;

import com.smartcbwtf.domain.RouteAssignment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Assignment DTO with staff and vehicle details.
 */
public record RouteAssignmentDTO(
        UUID id,
        UUID staffId,
        String staffName,
        String staffPhone,
        UUID vehicleId,
        String vehicleRegistration,
        LocalDate assignedFrom,
        LocalDate assignedTo,
        Boolean isActive,
        Instant createdAt) {
    public static RouteAssignmentDTO from(RouteAssignment assignment) {
        var staff = assignment.getStaff();
        var vehicle = assignment.getVehicle();
        return new RouteAssignmentDTO(
                assignment.getId(),
                staff != null ? staff.getId() : null,
                staff != null ? staff.getName() : null,
                staff != null ? staff.getPhone() : null,
                vehicle != null ? vehicle.getId() : null,
                vehicle != null ? vehicle.getRegistrationNumber() : null,
                assignment.getAssignedFrom(),
                assignment.getAssignedTo(),
                assignment.getIsActive(),
                assignment.getCreatedAt());
    }
}
