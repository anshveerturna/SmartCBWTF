package com.smartcbwtf.controller;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Read-only Master Data API for SuperAdmin.
 * Provides global visibility across all CBWTFs.
 * All endpoints require SUPER_ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/master-data")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminMasterDataController {

    private final HcfRepository hcfRepository;
    private final AttendanceRepository attendanceRepository;
    private final BagLabelRepository bagLabelRepository;
    private final BagEventRepository bagEventRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditLogRepository auditLogRepository;
    private final SubscriptionAuditRepository subscriptionAuditRepository;
    private final AppUserRepository userRepository;
    private final FacilityRepository facilityRepository;

    public AdminMasterDataController(
            HcfRepository hcfRepository,
            AttendanceRepository attendanceRepository,
            BagLabelRepository bagLabelRepository,
            BagEventRepository bagEventRepository,
            InvoiceRepository invoiceRepository,
            AuditLogRepository auditLogRepository,
            SubscriptionAuditRepository subscriptionAuditRepository,
            AppUserRepository userRepository,
            FacilityRepository facilityRepository) {
        this.hcfRepository = hcfRepository;
        this.attendanceRepository = attendanceRepository;
        this.bagLabelRepository = bagLabelRepository;
        this.bagEventRepository = bagEventRepository;
        this.invoiceRepository = invoiceRepository;
        this.auditLogRepository = auditLogRepository;
        this.subscriptionAuditRepository = subscriptionAuditRepository;
        this.userRepository = userRepository;
        this.facilityRepository = facilityRepository;
    }

    // ========== HCFs ==========

    @GetMapping("/hcfs")
    public ResponseEntity<Page<Map<String, Object>>> listHcfs(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Hcf> hcfs;

        if (search != null && !search.isBlank()) {
            hcfs = hcfRepository.searchByNameOrCode(search, pageable);
        } else if (status != null && !status.isBlank()) {
            hcfs = hcfRepository.findByStatus(status, pageable);
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

        Pageable pageable = PageRequest.of(page, size, Sort.by("eventTs").descending());
        Page<BagEvent> events;

        if (cbwtfId != null) {
            events = bagEventRepository.findByFacilityId(cbwtfId, pageable);
        } else if (eventType != null && !eventType.isBlank()) {
            events = bagEventRepository.findByEventType(eventType, pageable);
        } else {
            events = bagEventRepository.findAll(pageable);
        }

        Page<Map<String, Object>> result = events.map(this::mapBagEvent);
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> mapBagEvent(BagEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", event.getId());
        map.put("bagLabelId", event.getBagLabel().getId());
        map.put("qrCode", event.getBagLabel().getQrCode());
        map.put("hcfId", event.getHcf().getId());
        map.put("hcfName", event.getHcf().getName());
        map.put("cbwtfId", event.getFacility().getId());
        map.put("cbwtfName", event.getFacility().getName());
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

        Pageable pageable = PageRequest.of(page, size, Sort.by("issuedAt").descending());
        Page<BagLabel> bags;

        if (cbwtfId != null) {
            bags = bagLabelRepository.findByFacilityId(cbwtfId, pageable);
        } else if (status != null && !status.isBlank()) {
            bags = bagLabelRepository.findByStatus(status, pageable);
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

        Pageable pageable = PageRequest.of(page, size, Sort.by("issuedAt").descending());
        Page<BagLabel> labels;

        if (search != null && !search.isBlank()) {
            labels = bagLabelRepository.searchByQrCodeOrSerial(search, pageable);
        } else if (cbwtfId != null) {
            labels = bagLabelRepository.findByFacilityId(cbwtfId, pageable);
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

        Pageable pageable = PageRequest.of(page, size, Sort.by("eventTs").descending());
        Page<Attendance> records;

        if (from != null && to != null) {
            Instant fromTs = LocalDate.parse(from).atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant toTs = LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            records = attendanceRepository.findByEventTsBetween(fromTs, toTs, pageable);
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
    // Note: No Vehicle entity exists yet, return empty for now
    // This will be implemented when Vehicle entity is added

    @GetMapping("/vehicles")
    public ResponseEntity<Page<Map<String, Object>>> listVehicles(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        // Vehicles not yet implemented in domain model
        // Return empty page with proper structure
        return ResponseEntity.ok(Page.empty());
    }

    // ========== INVOICES ==========

    @GetMapping("/invoices")
    public ResponseEntity<Page<Map<String, Object>>> listInvoices(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Invoice> invoices;

        if (cbwtfId != null) {
            invoices = invoiceRepository.findByFacilityId(cbwtfId, pageable);
        } else if (status != null && !status.isBlank()) {
            invoices = invoiceRepository.findByStatus(status, pageable);
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
    // Note: No Payment entity exists yet, return empty for now

    @GetMapping("/payments")
    public ResponseEntity<Page<Map<String, Object>>> listPayments(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        // Payments not yet implemented in domain model
        return ResponseEntity.ok(Page.empty());
    }

    // ========== AUDIT LOGS ==========

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<Map<String, Object>>> listAuditLogs(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SubscriptionAudit> audits;

        if (cbwtfId != null) {
            audits = subscriptionAuditRepository.findByFacilityId(cbwtfId, pageable);
        } else if (action != null && !action.isBlank()) {
            audits = subscriptionAuditRepository.findByAction(action, pageable);
        } else {
            audits = subscriptionAuditRepository.findAll(pageable);
        }

        Page<Map<String, Object>> result = audits.map(this::mapAudit);
        return ResponseEntity.ok(result);
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
}
