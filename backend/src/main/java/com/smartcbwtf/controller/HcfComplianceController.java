package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.util.PaginationUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/hcf/compliance")
@PreAuthorize("hasRole('HCF_ADMIN')")
public class HcfComplianceController {
        private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Kolkata");
        private static final String UNKNOWN_CATEGORY = "UNKNOWN";
        private static final int DEFAULT_DAILY_PICKUP_LIMIT = 200;
        private static final int MAX_DAILY_PICKUP_LIMIT = 500;

        private final HcfRepository hcfRepository;
        private final AgreementRepository agreementRepository;
        private final BagEventRepository bagEventRepository;
        private final BagLabelRepository bagLabelRepository;
        private final DuesClearanceRequestRepository duesRequestRepository;
        private final HcfAccessGuard accessGuard;

        public HcfComplianceController(
                        HcfRepository hcfRepository,
                        AgreementRepository agreementRepository,
                        BagEventRepository bagEventRepository,
                        BagLabelRepository bagLabelRepository,
                        DuesClearanceRequestRepository duesRequestRepository,
                        HcfAccessGuard accessGuard,
                        com.smartcbwtf.service.PdfService pdfService) {
                this.hcfRepository = hcfRepository;
                this.agreementRepository = agreementRepository;
                this.bagEventRepository = bagEventRepository;
                this.bagLabelRepository = bagLabelRepository;
                this.duesRequestRepository = duesRequestRepository;
                this.accessGuard = accessGuard;
                this.pdfService = pdfService;
        }

        private final com.smartcbwtf.service.PdfService pdfService;

        // ==========================================
        // DAILY DATA (Always Accessible)
        // ==========================================

        @GetMapping("/daily")
        public ResponseEntity<Map<String, Object>> getDailyData(
                        @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(name = "pickupLimit", defaultValue = "200") int pickupLimit) {

                PortalHcfIds ids = requirePortalHcfIds();
                UUID hcfId = ids.hcfId();
                UUID facilityId = ids.facilityId();

                LocalDate targetDate = date != null ? date : LocalDate.now(REPORT_ZONE);
                Instant startOfDay = targetDate.atStartOfDay(REPORT_ZONE).toInstant();
                Instant endOfDay = targetDate.plusDays(1).atStartOfDay(REPORT_ZONE).toInstant();

                int safePickupLimit = PaginationUtils.normalizeSize(
                                pickupLimit, DEFAULT_DAILY_PICKUP_LIMIT, MAX_DAILY_PICKUP_LIMIT);

                Map<String, BigDecimal> categoryWeights = new HashMap<>();
                for (Object[] row : bagEventRepository
                                .sumWeightGroupedByCategoryForFacilityAndHcfBetweenIncludingUnknown(
                                                facilityId, hcfId, startOfDay, endOfDay)) {
                        String category = row[0] != null ? row[0].toString() : UNKNOWN_CATEGORY;
                        BigDecimal weight = row[1] instanceof BigDecimal decimal ? decimal : BigDecimal.ZERO;
                        categoryWeights.put(category, weight);
                }
                BigDecimal totalWeight = bagEventRepository.sumWeightByFacilityIdAndHcfIdAndEventTsBetween(
                                facilityId, hcfId, startOfDay, endOfDay);
                long pickupCount = bagEventRepository.countByFacilityIdAndHcfIdAndEventTsBetween(
                                facilityId, hcfId, startOfDay, endOfDay);

                // 2. QR Labels Generated (Issued)
                long qrGenerated = bagLabelRepository.countByFacilityIdAndHcfIdAndIssuedAtBetween(
                                facilityId, hcfId, startOfDay, endOfDay);

                // 3. Bounded recent pickup history
                List<BagEvent> dailyEvents = bagEventRepository
                                .findByFacilityIdAndHcfIdAndEventTsBetweenOrderByEventTsDesc(
                                                facilityId, hcfId, startOfDay, endOfDay,
                                                PageRequest.of(0, safePickupLimit));
                List<Map<String, Object>> pickupHistory = dailyEvents.stream()
                                .map(e -> {
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("timestamp", e.getEventTs());
                                        map.put("category", categoryOf(e));
                                        map.put("weight", weightOf(e));
                                        map.put("bagSerial", serialOf(e));
                                        return map;
                                })
                                .toList();

                return ResponseEntity.ok(Map.of(
                                "date", targetDate,
                                "totalWeight", totalWeight,
                                "categoryWeights", categoryWeights,
                                "qrGenerated", qrGenerated,
                                "pickupCount", pickupCount,
                                "pickupLimit", safePickupLimit,
                                "pickups", pickupHistory));
        }

        // ==========================================
        // DUES CLEARANCE STATUS & REQUEST
        // ==========================================

        @GetMapping("/status")
        public ResponseEntity<Map<String, Object>> getDuesStatus() {
                PortalHcfIds ids = requirePortalHcfIds();
                UUID hcfId = ids.hcfId();
                UUID facilityId = ids.facilityId();
                Hcf hcf = hcfRepository.findByIdAndFacilityId(hcfId, facilityId).orElseThrow();

                Map<String, Object> response = new HashMap<>();
                response.put("status", hcf.getDuesClearStatus());

                // Helper to check if pending request exists
                Optional<DuesClearanceRequest> lastRequest = duesRequestRepository
                                .findTopByHcfIdAndFacilityIdOrderByRequestedAtDesc(hcfId, facilityId);

                lastRequest.ifPresent(req -> {
                        response.put("lastRequestStatus", req.getManagementStatus());
                        response.put("lastRequestDate", req.getRequestedAt());
                        response.put("rejectionReason", req.getRejectionReason());
                        response.put("outstandingDues", req.getOutstandingDues());
                });

                return ResponseEntity.ok(response);
        }

        @PostMapping("/request-access")
        @Transactional
        public ResponseEntity<?> requestDuesClearance(@Valid @RequestBody DuesAccessRequest payload) {
                PortalHcfIds ids = requirePortalHcfIds();
                UUID hcfId = ids.hcfId();
                UUID facilityId = ids.facilityId();

                Integer month = payload != null ? payload.month() : null;
                Integer year = payload != null ? payload.year() : null;

                if (month == null || year == null) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", "MISSING_PERIOD", "message",
                                                        "Month and Year are required."));
                }
                YearMonth requestedPeriod = requireReportMonth(year, month);
                month = requestedPeriod.getMonthValue();
                year = requestedPeriod.getYear();

                // Check if report access is already granted (via approved request)
                boolean isApproved = duesRequestRepository
                                .existsByHcfIdAndFacilityIdAndRequestMonthAndRequestYearAndManagementStatus(
                                                hcfId, facilityId, month, year,
                                                DuesClearanceRequest.Status.APPROVED.name());

                if (isApproved) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", "ALREADY_APPROVED", "message",
                                                        "Access already granted for this period."));
                }

                // Check for pending request for THIS period
                boolean hasPending = duesRequestRepository
                                .existsByHcfIdAndFacilityIdAndRequestMonthAndRequestYearAndManagementStatusIn(
                                                hcfId, facilityId, month, year,
                                                List.of(DuesClearanceRequest.Status.PENDING.name(),
                                                                DuesClearanceRequest.Status.SUBMITTED.name()));

                if (hasPending) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", "REQUEST_PENDING",
                                        "message", "A request for this month is already pending."));
                }

                Hcf hcf = hcfRepository.findByIdAndFacilityId(hcfId, facilityId).orElseThrow();
                UUID userId = TenantContext.get().userId();

                // Find Active Agreement
                Agreement agreement = agreementRepository.findActiveByHcfAndFacility(hcfId, facilityId)
                                .orElseThrow(() -> new IllegalStateException("No active agreement found."));

                // Create Request
                DuesClearanceRequest request = new DuesClearanceRequest();
                request.setHcf(hcf);
                request.setFacility(agreement.getFacility());
                request.setAgreement(agreement);
                request.setRequestedBy(userId);
                request.setManagementStatusEnum(DuesClearanceRequest.Status.PENDING);
                request.setRequestMonth(month);
                request.setRequestYear(year);

                duesRequestRepository.save(request);

                // We do NOT update HCF global status anymore for granular requests
                // hcf.setDuesClearStatus(DuesClearStatus.REQUESTED);
                // hcfRepository.save(hcf);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Request submitted for " + month + "/" + year));
        }

        public record DuesAccessRequest(
                        @NotNull(message = "Month is required") @Min(value = 1, message = "Month must be between 1 and 12") @Max(value = 12, message = "Month must be between 1 and 12") Integer month,
                        @NotNull(message = "Year is required") @Min(value = 2000, message = "Year must be 2000 or later") @Max(value = 9999, message = "Year is invalid") Integer year) {
        }

        @PostMapping("/cancel-request")
        @Transactional
        public ResponseEntity<?> cancelDuesRequest() {
                PortalHcfIds ids = requirePortalHcfIds();
                UUID hcfId = ids.hcfId();
                UUID facilityId = ids.facilityId();

                // Find and delete any pending requests without loading full request history.
                List<DuesClearanceRequest> pendingRequests = duesRequestRepository
                                .findByHcfIdAndFacilityIdAndManagementStatusIn(
                                                hcfId,
                                                facilityId,
                                List.of(DuesClearanceRequest.Status.PENDING.name(),
                                                DuesClearanceRequest.Status.SUBMITTED.name()));

                if (!pendingRequests.isEmpty()) {
                        duesRequestRepository.deleteAll(pendingRequests);
                }

                return ResponseEntity.ok(Map.of("message", "Request cancelled successfully."));
        }

        @GetMapping("/monthly")
        public ResponseEntity<?> getMonthlyData(
                        @RequestParam(name = "year") int year,
                        @RequestParam(name = "month") int month) {

                // checkAccess(); // Removed global check to allow granular status check
                PortalHcfIds ids = requirePortalHcfIds();
                UUID hcfId = ids.hcfId();
                UUID facilityId = ids.facilityId();

                YearMonth ym = requireReportMonth(year, month);
                month = ym.getMonthValue();
                year = ym.getYear();
                Instant start = ym.atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
                Instant end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();

                BigDecimal totalWeight = bagEventRepository.sumWeightByFacilityIdAndHcfIdAndEventTsBetween(
                                facilityId, hcfId, start, end);

                // Breakdown logic similar to daily can be added here

                // Check Access Status for this specific month
                Optional<DuesClearanceRequest> req = duesRequestRepository
                                .findTopByHcfIdAndFacilityIdAndRequestMonthAndRequestYearOrderByRequestedAtDesc(
                                                hcfId, facilityId, month, year);

                String accessStatus = req.map(DuesClearanceRequest::getManagementStatus).orElse("NONE");

                return ResponseEntity.ok(Map.of(
                                "period", ym.toString(),
                                "totalWeight", totalWeight,
                                "accessStatus", accessStatus));
        }

        @GetMapping("/monthly-report/pdf")
        public ResponseEntity<?> downloadMonthlyReportPdf(
                        @RequestParam(name = "year") int year,
                        @RequestParam(name = "month") int month) {

                // granular check
                PortalHcfIds ids = requirePortalHcfIds();
                UUID hcfId = ids.hcfId();
                UUID facilityId = ids.facilityId();
                YearMonth ym = requireReportMonth(year, month);
                month = ym.getMonthValue();
                year = ym.getYear();

                boolean isApproved = duesRequestRepository
                                .existsByHcfIdAndFacilityIdAndRequestMonthAndRequestYearAndManagementStatus(
                                                hcfId, facilityId, month, year,
                                                DuesClearanceRequest.Status.APPROVED.name());

                if (!isApproved) {
                        // Fallback: Check global Access?
                        // User requested explicit restriction. So NO fallback for now.
                        throw new AccessDeniedException("Access not granted for " + month + "/" + year);
                }

                Hcf hcf = hcfRepository.findByIdAndFacilityId(hcfId, facilityId).orElseThrow();

                // Find Active Agreement for Facility details
                // Find Active Agreement for Facility details
                Agreement agreement = agreementRepository.findActiveByHcfAndFacility(hcfId, facilityId)
                                .orElseThrow(() -> new IllegalStateException(
                                                "No active agreement found. Cannot generate report."));

                Instant start = ym.atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
                Instant end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();

                // Fetch detailed events
                List<BagEvent> events = bagEventRepository.findByFacilityIdAndHcfIdAndEventTsBetween(
                                facilityId, hcfId, start, end);

                // Generate PDF
                byte[] pdfBytes = pdfService.generateMonthlyCompliancePdf(agreement, ym.atDay(1), events);

                String filename = "Monthly_Compliance_Report_" + hcf.getCode() + "_" + ym + ".pdf";

                return ResponseEntity.ok()
                                .cacheControl(CacheControl.noStore())
                                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + filename + "\"")
                                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                                .body(pdfBytes);
        }

        @GetMapping("/yearly")
        public ResponseEntity<?> getYearlyData(@RequestParam(name = "year") int year) {
                int reportYear = requireReportYear(year);
                PortalHcfIds ids = checkAccess();
                UUID hcfId = ids.hcfId();
                UUID facilityId = ids.facilityId();

                YearMonth ym = YearMonth.of(reportYear, 1);
                Instant start = ym.atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
                Instant end = ym.plusYears(1).atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();

                BigDecimal totalWeight = bagEventRepository.sumWeightByFacilityIdAndHcfIdAndEventTsBetween(
                                facilityId, hcfId, start, end);

                return ResponseEntity.ok(Map.of(
                                "period", String.valueOf(reportYear),
                                "totalWeight", totalWeight));
        }

        private PortalHcfIds checkAccess() {
                PortalHcfIds ids = requirePortalHcfIds();
                Hcf hcf = hcfRepository.findByIdAndFacilityId(ids.hcfId(), ids.facilityId()).orElseThrow();
                if (hcf.getDuesClearStatus() != DuesClearStatus.CLEARED) {
                        throw new AccessDeniedException("Dues not cleared. Please request access.");
                }
                return ids;
        }

        private PortalHcfIds requirePortalHcfIds() {
                UUID hcfId = TenantContext.getHcfId();
                UUID facilityId = TenantContext.getTenantId();
                accessGuard.assertPortalAccess(hcfId, facilityId);
                return new PortalHcfIds(hcfId, facilityId);
        }

        private record PortalHcfIds(UUID hcfId, UUID facilityId) {
        }

        private YearMonth requireReportMonth(int year, int month) {
                YearMonth period;
                try {
                        period = YearMonth.of(year, month);
                } catch (DateTimeException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report month");
                }
                if (period.isAfter(YearMonth.now(REPORT_ZONE))) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Future compliance report periods are not available");
                }
                return period;
        }

        private int requireReportYear(int year) {
                int currentYear = Year.now(REPORT_ZONE).getValue();
                if (year < 2000 || year > currentYear) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report year");
                }
                return year;
        }

        // Custom Exception for cleaner response
        @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
        public static class AccessDeniedException extends RuntimeException {
                public AccessDeniedException(String message) {
                        super(message);
                }
        }

        private static String categoryOf(BagEvent event) {
                if (event.getBagLabel() == null || event.getBagLabel().getCategory() == null
                                || event.getBagLabel().getCategory().isBlank()) {
                        return UNKNOWN_CATEGORY;
                }
                return event.getBagLabel().getCategory();
        }

        private static String serialOf(BagEvent event) {
                if (event.getBagLabel() == null || event.getBagLabel().getSerialNo() == null) {
                        return "";
                }
                return event.getBagLabel().getSerialNo();
        }

        private static BigDecimal weightOf(BagEvent event) {
                return event.getWeightKg() != null ? event.getWeightKg() : BigDecimal.ZERO;
        }

}
