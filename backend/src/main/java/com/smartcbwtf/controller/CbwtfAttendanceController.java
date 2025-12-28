package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Attendance;
import com.smartcbwtf.repository.AttendanceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller for CBWTF Attendance Management.
 * Lists attendance records for the facility.
 */
@RestController
@RequestMapping("/api/cbwtf/attendance")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfAttendanceController {

        private final AttendanceRepository attendanceRepository;

        public CbwtfAttendanceController(AttendanceRepository attendanceRepository) {
                this.attendanceRepository = attendanceRepository;
        }

        /**
         * List attendance records for the current CBWTF.
         * Returns: staff name, HCF name, HCF address, timestamp
         */
        @GetMapping
        public ResponseEntity<AttendanceListResponse> listAttendance(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "50") int size) {

                UUID facilityId = TenantContext.getTenantId();
                if (facilityId == null) {
                        throw new IllegalStateException("Tenant ID not found in context");
                }

                Pageable pageable = PageRequest.of(page, Math.min(size, 100));

                // Query attendance by driver's facility (more reliable than
                // attendance.facility)
                Page<Attendance> attendancePage = attendanceRepository.findByDriverFacilityId(facilityId, pageable);

                List<AttendanceDTO> records = attendancePage.getContent().stream()
                                .map(a -> {
                                        String hcfAddress = "";
                                        if (a.getHcf() != null && a.getHcf().getAddress() != null) {
                                                hcfAddress = a.getHcf().getAddress();
                                        }
                                        return new AttendanceDTO(
                                                        a.getId().toString(),
                                                        a.getDriver() != null ? a.getDriver().getFullName() : "Unknown",
                                                        a.getDriver() != null ? a.getDriver().getRole() : null,
                                                        a.getHcf() != null ? a.getHcf().getName() : "Unknown",
                                                        a.getHcf() != null ? a.getHcf().getId().toString() : null,
                                                        hcfAddress,
                                                        a.getEventTs(),
                                                        a.getGpsLat(),
                                                        a.getGpsLon());
                                })
                                .toList();

                return ResponseEntity.ok(new AttendanceListResponse(
                                records,
                                attendancePage.getTotalElements(),
                                attendancePage.getTotalPages(),
                                page));
        }

        public record AttendanceDTO(
                        String id,
                        String staffName,
                        String staffRole,
                        String hcfName,
                        String hcfId,
                        String hcfAddress,
                        Instant eventTs,
                        Double gpsLat,
                        Double gpsLon) {
        }

        public record AttendanceListResponse(
                        List<AttendanceDTO> records,
                        long totalRecords,
                        int totalPages,
                        int currentPage) {
        }
}
