package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.HcfAccessGuard;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/hcf/compliance")
@PreAuthorize("hasRole('HCF_ADMIN')")
public class HcfComplianceController {

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
                        @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

                UUID hcfId = TenantContext.getHcfId();
                accessGuard.assertPortalAccess(hcfId);

                LocalDate targetDate = date != null ? date : LocalDate.now();
                Instant startOfDay = targetDate.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
                Instant endOfDay = targetDate.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();

                // 1. Waste Collected by Category
                List<BagEvent> dailyEvents = bagEventRepository.findByHcfIdAndEventTsBetween(hcfId, startOfDay,
                                endOfDay);

                Map<String, BigDecimal> categoryWeights = new HashMap<>();
                BigDecimal totalWeight = BigDecimal.ZERO;

                for (BagEvent event : dailyEvents) {
                        String category = event.getBagLabel().getCategory();
                        BigDecimal weight = event.getWeightKg();

                        categoryWeights.merge(category, weight, BigDecimal::add);
                        totalWeight = totalWeight.add(weight);
                }

                // 2. QR Labels Generated (Issued)
                // Assuming "issued_at" tracks generation. This might need BagLabelRepository
                // method.
                // For now, returning placeholder or simple count if method exists.
                long qrGenerated = bagLabelRepository.countByHcfIdAndIssuedAtBetween(hcfId, startOfDay, endOfDay);

                // 3. Pickup History (Events)
                List<Map<String, Object>> pickupHistory = dailyEvents.stream()
                                .map(e -> {
                                        Map<String, Object> map = new HashMap<>();
                                        map.put("timestamp", e.getEventTs());
                                        map.put("category", e.getBagLabel().getCategory());
                                        map.put("weight", e.getWeightKg());
                                        map.put("bagSerial", e.getBagLabel().getSerialNo());
                                        return map;
                                })
                                .sorted((a, b) -> ((Instant) b.get("timestamp"))
                                                .compareTo((Instant) a.get("timestamp")))
                                .toList();

                return ResponseEntity.ok(Map.of(
                                "date", targetDate,
                                "totalWeight", totalWeight,
                                "categoryWeights", categoryWeights,
                                "qrGenerated", qrGenerated,
                                "pickups", pickupHistory));
        }

        // ==========================================
        // DUES CLEARANCE STATUS & REQUEST
        // ==========================================

        @GetMapping("/status")
        public ResponseEntity<Map<String, Object>> getDuesStatus() {
                UUID hcfId = TenantContext.getHcfId();
                Hcf hcf = hcfRepository.findById(hcfId).orElseThrow();

                Map<String, Object> response = new HashMap<>();
                response.put("status", hcf.getDuesClearStatus());

                // Helper to check if pending request exists
                Optional<DuesClearanceRequest> lastRequest = duesRequestRepository
                                .findTopByHcfIdOrderByRequestedAtDesc(hcfId);

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
        public ResponseEntity<?> requestDuesClearance(@RequestBody Map<String, Integer> payload) {
                UUID hcfId = TenantContext.getHcfId();
                Hcf hcf = hcfRepository.findById(hcfId).orElseThrow();
                UUID userId = TenantContext.get().userId();

                Integer month = payload.get("month");
                Integer year = payload.get("year");

                if (month == null || year == null) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", "MISSING_PERIOD", "message",
                                                        "Month and Year are required."));
                }

                // Check if report access is already granted (via approved request)
                boolean isApproved = duesRequestRepository.findByHcfIdOrderByRequestedAtDesc(hcfId).stream()
                                .anyMatch(r -> "APPROVED".equals(r.getManagementStatus())
                                                && Objects.equals(r.getRequestMonth(), month)
                                                && Objects.equals(r.getRequestYear(), year));

                if (isApproved) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", "ALREADY_APPROVED", "message",
                                                        "Access already granted for this period."));
                }

                // Check for pending request for THIS period
                boolean hasPending = duesRequestRepository.findByHcfIdOrderByRequestedAtDesc(hcfId).stream()
                                .anyMatch(
                                                r -> ("PENDING".equals(r.getManagementStatus())
                                                                || "SUBMITTED".equals(r.getManagementStatus()))
                                                                && Objects.equals(r.getRequestMonth(), month)
                                                                && Objects.equals(r.getRequestYear(), year));

                if (hasPending) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", "REQUEST_PENDING",
                                        "message", "A request for this month is already pending."));
                }

                // Find Active Agreement
                Agreement agreement = agreementRepository.findByHcfIdAndStatus(hcfId, "ACTIVE").stream()
                                .findFirst()
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

        @PostMapping("/cancel-request")
        @Transactional
        public ResponseEntity<?> cancelDuesRequest() {
                UUID hcfId = TenantContext.getHcfId();
                Hcf hcf = hcfRepository.findById(hcfId).orElseThrow();

                // Find and delete any pending requests to be safe
                // Use existing safe method and filter in memory
                List<DuesClearanceRequest> allRequests = duesRequestRepository.findByHcfIdOrderByRequestedAtDesc(hcfId);

                List<DuesClearanceRequest> pendingRequests = allRequests.stream()
                                .filter(req -> "PENDING".equals(req.getManagementStatus())
                                                || "SUBMITTED".equals(req.getManagementStatus()))
                                .toList();

                if (!pendingRequests.isEmpty()) {
                        duesRequestRepository.deleteAll(pendingRequests);
                }

                // Reset HCF Status
                hcf.setDuesClearStatus(DuesClearStatus.PENDING);
                hcfRepository.save(hcf);

                return ResponseEntity.ok(Map.of("message", "Request cancelled successfully."));
        }

        @GetMapping("/monthly")
        public ResponseEntity<?> getMonthlyData(
                        @RequestParam(name = "year") int year,
                        @RequestParam(name = "month") int month) {

                // checkAccess(); // Removed global check to allow granular status check
                UUID hcfId = TenantContext.getHcfId();

                YearMonth ym = YearMonth.of(year, month);
                Instant start = ym.atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
                Instant end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();

                BigDecimal totalWeight = bagEventRepository.sumWeightByHcfIdAndEventTsBetween(hcfId, start, end);

                // Breakdown logic similar to daily can be added here

                // Check Access Status for this specific month
                Optional<DuesClearanceRequest> req = duesRequestRepository.findByHcfIdOrderByRequestedAtDesc(hcfId)
                                .stream()
                                .filter(r -> Objects.equals(r.getRequestMonth(), month)
                                                && Objects.equals(r.getRequestYear(), year))
                                .findFirst();

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
                UUID hcfId = TenantContext.getHcfId();
                boolean isApproved = duesRequestRepository.findByHcfIdOrderByRequestedAtDesc(hcfId).stream()
                                .anyMatch(r -> "APPROVED".equals(r.getManagementStatus())
                                                && Objects.equals(r.getRequestMonth(), month)
                                                && Objects.equals(r.getRequestYear(), year));

                if (!isApproved) {
                        // Fallback: Check global Access?
                        // User requested explicit restriction. So NO fallback for now.
                        throw new AccessDeniedException("Access not granted for " + month + "/" + year);
                }

                Hcf hcf = hcfRepository.findById(hcfId).orElseThrow();

                // Find Active Agreement for Facility details
                // Find Active Agreement for Facility details
                Agreement agreement = agreementRepository.findByHcfIdAndStatus(hcfId, "ACTIVE").stream()
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                                "No active agreement found. Cannot generate report."));

                YearMonth ym = YearMonth.of(year, month);
                Instant start = ym.atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
                Instant end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();

                // Fetch detailed events
                List<BagEvent> events = bagEventRepository.findByHcfIdAndEventTsBetween(hcfId, start, end);

                // Generate PDF
                byte[] pdfBytes = pdfService.generateMonthlyCompliancePdf(agreement, ym.atDay(1), events);

                String filename = "Monthly_Compliance_Report_" + hcf.getCode() + "_" + ym + ".pdf";

                return ResponseEntity.ok()
                                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"" + filename + "\"")
                                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                                .body(pdfBytes);
        }

        @GetMapping("/yearly")
        public ResponseEntity<?> getYearlyData(@RequestParam(name = "year") int year) {
                checkAccess();
                UUID hcfId = TenantContext.getHcfId();

                YearMonth ym = YearMonth.of(year, 1);
                Instant start = ym.atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
                Instant end = ym.plusYears(1).atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();

                BigDecimal totalWeight = bagEventRepository.sumWeightByHcfIdAndEventTsBetween(hcfId, start, end);

                return ResponseEntity.ok(Map.of(
                                "period", String.valueOf(year),
                                "totalWeight", totalWeight));
        }

        private void checkAccess() {
                UUID hcfId = TenantContext.getHcfId();
                Hcf hcf = hcfRepository.findById(hcfId).orElseThrow();
                if (hcf.getDuesClearStatus() != DuesClearStatus.CLEARED) {
                        throw new AccessDeniedException("Dues not cleared. Please request access.");
                }
        }

        // Custom Exception for cleaner response
        @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
        public static class AccessDeniedException extends RuntimeException {
                public AccessDeniedException(String message) {
                        super(message);
                }
        }
}
