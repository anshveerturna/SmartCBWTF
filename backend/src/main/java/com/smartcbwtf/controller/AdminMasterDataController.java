package com.smartcbwtf.controller;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.smartcbwtf.util.PaginationUtils.pageRequest;

/**
 * Read-only Master Data API for SuperAdmin.
 * Provides global visibility across all CBWTFs.
 * All endpoints require SUPER_ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/master-data")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminMasterDataController {
    private static final int MAX_FILTER_LENGTH = 80;
    private static final int MAX_SEARCH_LENGTH = 120;

    private final HcfRepository hcfRepository;
    private final AttendanceRepository attendanceRepository;
    private final BagLabelRepository bagLabelRepository;
    private final BagEventRepository bagEventRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditLogRepository auditLogRepository;
    private final SubscriptionAuditRepository subscriptionAuditRepository;
    private final AppUserRepository userRepository;
    private final FacilityRepository facilityRepository;
    private final VehicleRepository vehicleRepository;
    private final PaymentRepository paymentRepository;

    public AdminMasterDataController(
            HcfRepository hcfRepository,
            AttendanceRepository attendanceRepository,
            BagLabelRepository bagLabelRepository,
            BagEventRepository bagEventRepository,
            InvoiceRepository invoiceRepository,
            AuditLogRepository auditLogRepository,
            SubscriptionAuditRepository subscriptionAuditRepository,
            AppUserRepository userRepository,
            FacilityRepository facilityRepository,
            VehicleRepository vehicleRepository,
            PaymentRepository paymentRepository) {
        this.hcfRepository = hcfRepository;
        this.attendanceRepository = attendanceRepository;
        this.bagLabelRepository = bagLabelRepository;
        this.bagEventRepository = bagEventRepository;
        this.invoiceRepository = invoiceRepository;
        this.auditLogRepository = auditLogRepository;
        this.subscriptionAuditRepository = subscriptionAuditRepository;
        this.userRepository = userRepository;
        this.facilityRepository = facilityRepository;
        this.vehicleRepository = vehicleRepository;
        this.paymentRepository = paymentRepository;
    }

    // ========== HCFs ==========

    @GetMapping("/hcfs")
    public ResponseEntity<Page<Map<String, Object>>> listHcfs(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = pageRequest(page, size, 20, Sort.by("createdAt").descending());
        Page<Hcf> hcfs;
        String normalizedStatus = normalizeFilter(status, "status");
        String normalizedSearch = normalizeSearch(search);

        if (cbwtfId != null) {
            hcfs = hcfRepository.findByFacilityIdWithFilters(cbwtfId, normalizedStatus, normalizedSearch, pageable);
        } else if (normalizedSearch != null) {
            hcfs = hcfRepository.searchByNameOrCode(normalizedSearch, pageable);
        } else if (normalizedStatus != null) {
            hcfs = hcfRepository.findByStatus(normalizedStatus, pageable);
        } else {
            hcfs = hcfRepository.findAll(pageable);
        }

        Page<Map<String, Object>> result = hcfs.map(this::mapHcf);
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> mapHcf(Hcf hcf) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", hcf.getId());
        map.put("code", hcf.getCode());
        map.put("name", hcf.getName());
        map.put("address", hcf.getAddress());
        map.put("contactEmail", hcf.getContactEmail());
        map.put("contactPhone", hcf.getContactPhone());
        map.put("numberOfBeds", hcf.getNumberOfBeds());
        map.put("status", hcf.getStatus());
        map.put("createdAt", hcf.getCreatedAt());
        return map;
    }

    // ========== WASTE PICKUPS (BagEvents) ==========

    @GetMapping("/pickups")
    public ResponseEntity<Page<Map<String, Object>>> listPickups(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = pageRequest(page, size, 20, Sort.by("eventTs").descending());
        Page<BagEvent> events;
        String normalizedEventType = normalizeFilter(eventType, "eventType");
        DateRange dateRange = parseOptionalDateRange(from, to);

        if (cbwtfId != null) {
            events = bagEventRepository.findByFacilityId(cbwtfId, pageable);
        } else if (normalizedEventType != null) {
            events = bagEventRepository.findByEventType(normalizedEventType, pageable);
        } else if (dateRange != null) {
            events = bagEventRepository.findByEventTsBetween(dateRange.fromInclusive(), dateRange.toExclusive(),
                    pageable);
        } else {
            events = bagEventRepository.findAll(pageable);
        }

        Page<Map<String, Object>> result = events.map(this::mapBagEvent);
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> mapBagEvent(BagEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        BagLabel label = event.getBagLabel();
        Hcf hcf = event.getHcf();
        Facility facility = event.getFacility();
        map.put("id", event.getId());
        map.put("bagLabelId", label != null ? label.getId() : null);
        map.put("qrCode", label != null ? label.getQrCode() : null);
        map.put("hcfId", hcf != null ? hcf.getId() : null);
        map.put("hcfName", hcf != null ? hcf.getName() : null);
        map.put("cbwtfId", facility != null ? facility.getId() : null);
        map.put("cbwtfName", facility != null ? facility.getName() : null);
        map.put("eventType", event.getEventType());
        map.put("eventTs", event.getEventTs());
        map.put("weightKg", event.getWeightKg());
        map.put("anomalyState", event.getAnomalyState());
        map.put("collectedByUserId", event.getCollectedByUserId());
        map.put("createdAt", event.getCreatedAt());
        return map;
    }

    // ========== WASTE BAGS (BagLabels) ==========

    @GetMapping("/bags")
    public ResponseEntity<Page<Map<String, Object>>> listBags(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = pageRequest(page, size, 20, Sort.by("issuedAt").descending());
        Page<BagLabel> bags;
        String normalizedStatus = normalizeFilter(status, "status");
        String normalizedCategory = normalizeFilter(category, "category");

        if (cbwtfId != null) {
            bags = bagLabelRepository.findByFacilityId(cbwtfId, pageable);
        } else if (normalizedStatus != null) {
            bags = bagLabelRepository.findByStatus(normalizedStatus, pageable);
        } else if (normalizedCategory != null) {
            bags = bagLabelRepository.findByCategory(normalizedCategory, pageable);
        } else {
            bags = bagLabelRepository.findAll(pageable);
        }

        Page<Map<String, Object>> result = bags.map(this::mapBagLabel);
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> mapBagLabel(BagLabel bag) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", bag.getId());
        map.put("serialNo", bag.getSerialNo());
        map.put("qrCode", bag.getQrCode());
        map.put("category", bag.getCategory());
        map.put("status", bag.getStatus());
        map.put("hcfId", bag.getHcf().getId());
        map.put("hcfName", bag.getHcf().getName());
        map.put("cbwtfId", bag.getFacility().getId());
        map.put("cbwtfName", bag.getFacility().getName());
        map.put("issuedAt", bag.getIssuedAt());
        map.put("usedAt", bag.getUsedAt());
        return map;
    }

    // ========== QR LABELS (same as bags, different view) ==========

    @GetMapping("/qr-labels")
    public ResponseEntity<Page<Map<String, Object>>> listQrLabels(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = pageRequest(page, size, 20, Sort.by("issuedAt").descending());
        Page<BagLabel> labels;
        String normalizedStatus = normalizeFilter(status, "status");
        String normalizedSearch = normalizeSearch(search);

        if (normalizedSearch != null) {
            labels = bagLabelRepository.searchByQrCodeOrSerial(normalizedSearch, pageable);
        } else if (cbwtfId != null) {
            labels = bagLabelRepository.findByFacilityId(cbwtfId, pageable);
        } else if (normalizedStatus != null) {
            labels = bagLabelRepository.findByStatus(normalizedStatus, pageable);
        } else {
            labels = bagLabelRepository.findAll(pageable);
        }

        Page<Map<String, Object>> result = labels.map(this::mapQrLabel);
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> mapQrLabel(BagLabel label) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", label.getId());
        map.put("qrCode", label.getQrCode());
        map.put("serialNo", label.getSerialNo());
        map.put("category", label.getCategory());
        map.put("status", label.getStatus());
        map.put("hcfName", label.getHcf().getName());
        map.put("cbwtfName", label.getFacility().getName());
        map.put("issuedAt", label.getIssuedAt());
        return map;
    }

    // ========== ATTENDANCE ==========

    @GetMapping("/attendance")
    public ResponseEntity<Page<Map<String, Object>>> listAttendance(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = pageRequest(page, size, 20, Sort.by("eventTs").descending());
        Page<Attendance> records;
        DateRange dateRange = parseOptionalDateRange(from, to);

        if (cbwtfId != null) {
            records = attendanceRepository.findByFacilityId(cbwtfId, pageable);
        } else if (dateRange != null) {
            records = attendanceRepository.findByEventTsBetween(dateRange.fromInclusive(), dateRange.toExclusive(),
                    pageable);
        } else {
            records = attendanceRepository.findAll(pageable);
        }

        Page<Map<String, Object>> result = records.map(this::mapAttendance);
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> mapAttendance(Attendance att) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", att.getId());
        map.put("driverId", att.getDriver().getId());
        map.put("driverName", att.getDriver().getFullName());
        map.put("hcfId", att.getHcf().getId());
        map.put("hcfName", att.getHcf().getName());
        map.put("eventTs", att.getEventTs());
        map.put("gpsLat", att.getGpsLat());
        map.put("gpsLon", att.getGpsLon());
        map.put("distanceFromHcfM", att.getDistanceFromHcfM());
        map.put("createdAt", att.getCreatedAt());
        return map;
    }

    // ========== VEHICLES ==========

    @GetMapping("/vehicles")
    public ResponseEntity<Page<Map<String, Object>>> listVehicles(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = pageRequest(page, size, 20, Sort.by("createdAt").descending());
        Page<Vehicle> vehicles = cbwtfId != null
                ? vehicleRepository.findByFacilityId(cbwtfId, pageable)
                : vehicleRepository.findAll(pageable);

        return ResponseEntity.ok(vehicles.map(this::mapVehicle));
    }

    private Map<String, Object> mapVehicle(Vehicle vehicle) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", vehicle.getId());
        map.put("registrationNo", vehicle.getRegistrationNumber());
        map.put("vehicleType", vehicle.getVehicleType());
        map.put("cbwtfId", vehicle.getFacility().getId());
        map.put("cbwtfName", vehicle.getFacility().getName());
        map.put("driverId", vehicle.getAssignedDriver() != null ? vehicle.getAssignedDriver().getId() : null);
        map.put("driverName", vehicle.getAssignedDriver() != null ? vehicle.getAssignedDriver().getFullName() : null);
        map.put("status", vehicle.getStatus());
        map.put("gpsStatus", vehicle.getGpsStatus());
        map.put("gpsVendor", vehicle.getGpsVendor());
        map.put("lastGpsAt", vehicle.getLastGpsAt());
        map.put("createdAt", vehicle.getCreatedAt());
        return map;
    }

    // ========== INVOICES ==========

    @GetMapping("/invoices")
    public ResponseEntity<Page<Map<String, Object>>> listInvoices(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = pageRequest(page, size, 20, Sort.by("createdAt").descending());
        Page<Invoice> invoices;
        String normalizedStatus = normalizeFilter(status, "status");

        if (cbwtfId != null) {
            invoices = invoiceRepository.findByFacilityId(cbwtfId, pageable);
        } else if (normalizedStatus != null) {
            invoices = invoiceRepository.findByStatus(normalizedStatus, pageable);
        } else {
            invoices = invoiceRepository.findAll(pageable);
        }

        Page<Map<String, Object>> result = invoices.map(this::mapInvoice);
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> mapInvoice(Invoice inv) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", inv.getId());
        map.put("invoiceNumber", inv.getInvoiceNumber());
        map.put("hcfId", inv.getHcf().getId());
        map.put("hcfName", inv.getHcf().getName());
        map.put("cbwtfId", inv.getFacility().getId());
        map.put("cbwtfName", inv.getFacility().getName());
        map.put("periodStart", inv.getPeriodStart());
        map.put("periodEnd", inv.getPeriodEnd());
        map.put("totalAmount", inv.getTotalAmount());
        map.put("status", inv.getStatus());
        map.put("createdAt", inv.getCreatedAt());
        return map;
    }

    // ========== PAYMENTS ==========

    @GetMapping("/payments")
    public ResponseEntity<Page<Map<String, Object>>> listPayments(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = pageRequest(page, size, 20,
                Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("createdAt")));
        Page<Payment> payments = cbwtfId != null
                ? paymentRepository.findByFacilityId(cbwtfId, pageable)
                : paymentRepository.findAll(pageable);

        return ResponseEntity.ok(payments.map(this::mapPayment));
    }

    private Map<String, Object> mapPayment(Payment payment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", payment.getId());
        map.put("paymentId", payment.getId());
        map.put("referenceNumber", payment.getReferenceNumber());
        map.put("hcfId", payment.getHcf().getId());
        map.put("hcfName", payment.getHcf().getName());
        map.put("cbwtfId", payment.getFacility().getId());
        map.put("cbwtfName", payment.getFacility().getName());
        map.put("amount", payment.getAmount());
        map.put("paymentMethod", payment.getMode() != null ? payment.getMode().name() : null);
        map.put("status", "RECORDED");
        map.put("paidAt", payment.getPaymentDate());
        map.put("createdAt", payment.getCreatedAt());
        return map;
    }

    // ========== AUDIT LOGS ==========

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<Map<String, Object>>> listAuditLogs(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = pageRequest(page, size, 20, Sort.by("createdAt").descending());
        Page<SubscriptionAudit> audits;
        String normalizedAction = normalizeFilter(action, "action");

        if (cbwtfId != null) {
            audits = subscriptionAuditRepository.findByFacilityId(cbwtfId, pageable);
        } else if (normalizedAction != null) {
            audits = subscriptionAuditRepository.findByAction(normalizedAction, pageable);
        } else {
            audits = subscriptionAuditRepository.findAll(pageable);
        }

        Page<Map<String, Object>> result = audits.map(this::mapAudit);
        return privateResponse(result);
    }

    private static <T> ResponseEntity<T> privateResponse(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    private Map<String, Object> mapAudit(SubscriptionAudit audit) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", audit.getId());
        map.put("entityType", audit.getEntityType());
        map.put("entityId", audit.getEntityId());
        map.put("action", audit.getAction());
        map.put("oldValue", audit.getOldValue());
        map.put("newValue", audit.getNewValue());
        map.put("performedByUsername", audit.getPerformedByUsername());
        map.put("performedByRole", audit.getPerformedByRole());
        map.put("notes", audit.getNotes());
        map.put("createdAt", audit.getCreatedAt());
        return map;
    }

    private static String normalizeSearch(String search) {
        return normalizeText(search, MAX_SEARCH_LENGTH, "search");
    }

    private static String normalizeFilter(String value, String label) {
        return normalizeText(value, MAX_FILTER_LENGTH, label);
    }

    private static String normalizeText(String value, int maxLength, String label) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " must be " + maxLength + " characters or fewer");
        }
        for (int i = 0; i < normalized.length(); i++) {
            if (Character.isISOControl(normalized.charAt(i))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        label + " contains unsupported control characters");
            }
        }
        return normalized;
    }

    private static DateRange parseOptionalDateRange(String from, String to) {
        String normalizedFrom = normalizeFilter(from, "from");
        String normalizedTo = normalizeFilter(to, "to");
        if (normalizedFrom == null && normalizedTo == null) {
            return null;
        }
        if (normalizedFrom == null || normalizedTo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both from and to dates are required");
        }
        LocalDate fromDate = parseIsoDate(normalizedFrom, "from");
        LocalDate toDate = parseIsoDate(normalizedTo, "to");
        if (toDate.isBefore(fromDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to date must be on or after from date");
        }
        ZoneId zone = ZoneId.systemDefault();
        return new DateRange(
                fromDate.atStartOfDay(zone).toInstant(),
                toDate.plusDays(1).atStartOfDay(zone).toInstant());
    }

    private static LocalDate parseIsoDate(String value, String label) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " date must use ISO format YYYY-MM-DD");
        }
    }

    private record DateRange(Instant fromInclusive, Instant toExclusive) {
    }
}
