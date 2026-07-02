package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Attendance;
import com.smartcbwtf.repository.AttendanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.smartcbwtf.util.PaginationUtils.pageRequest;

/**
 * Controller for CBWTF Attendance Management.
 * Lists attendance records for the facility.
 */
@RestController
@RequestMapping("/api/cbwtf/attendance")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfAttendanceController {

        private static final Logger log = LoggerFactory.getLogger(CbwtfAttendanceController.class);
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
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "50") int size) {

                try {
                        UUID facilityId = TenantContext.getTenantId();
                        if (facilityId == null) {
                                // Return empty list instead of throwing exception
                                return ResponseEntity.ok(new AttendanceListResponse(
                                                List.of(),
                                                0,
                                                0,
                                                page));
                        }

                        Pageable pageable = pageRequest(page, size, 50);

                        // Query attendance by driver's facility (more reliable than
                        // attendance.facility)
                        Page<Attendance> attendancePage = attendanceRepository.findByDriverFacilityId(facilityId,
                                        pageable);

                        List<AttendanceDTO> records = attendancePage.getContent().stream()
                                        .map(a -> {
                                                String hcfAddress = "";
                                                if (a.getHcf() != null && a.getHcf().getAddress() != null) {
                                                        hcfAddress = a.getHcf().getAddress();
                                                }
                                                return new AttendanceDTO(
                                                                a.getId().toString(),
                                                                a.getDriver() != null ? a.getDriver().getFullName()
                                                                                : "Unknown",
                                                                a.getDriver() != null ? a.getDriver().getRole() : null,
                                                                a.getHcf() != null ? a.getHcf().getName() : "Unknown",
                                                                a.getHcf() != null ? a.getHcf().getId().toString()
                                                                                : null,
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
                } catch (Exception e) {
                        log.error("Failed to list attendance records", e);
                        return ResponseEntity.ok(new AttendanceListResponse(
                                        List.of(),
                                        0,
                                        0,
                                        page));
                }
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
