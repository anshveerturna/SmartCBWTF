package com.smartcbwtf.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Invoice Controller - DEPRECATED.
 * 
 * Invoice generation is now handled externally via Tally accounting software.
 * This controller returns 410 Gone for all endpoints and logs access attempts
 * to detect old clients or rogue integrations.
 * 
 * @deprecated Invoice generation moved to Tally. Use /api/cbwtf/billing/bills/*
 *             endpoints instead.
 */
@Deprecated(forRemoval = true)
@RestController
@RequestMapping("/api/invoices")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class InvoiceController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    private static final Map<String, Object> DEPRECATED_RESPONSE = Map.of(
            "error", "Invoice generation is handled externally via Tally.",
            "message", "This API is deprecated. Use /api/cbwtf/billing/bills/* endpoints for operational bills.",
            "replacement", "/api/cbwtf/billing/bills");

    @PostMapping("/generate")
    @Deprecated(forRemoval = true)
    public ResponseEntity<?> generate(@RequestBody(required = false) Object request) {
        log.warn("DEPRECATED API accessed: POST /api/invoices/generate - caller should use /api/cbwtf/billing/bills");
        return ResponseEntity.status(410).body(DEPRECATED_RESPONSE);
    }

    @GetMapping("/list")
    @Deprecated(forRemoval = true)
    public ResponseEntity<?> list() {
        log.warn("DEPRECATED API accessed: GET /api/invoices/list - caller should use /api/cbwtf/billing/bills");
        return ResponseEntity.status(410).body(DEPRECATED_RESPONSE);
    }

    @GetMapping("/{id}/pdf")
    @Deprecated(forRemoval = true)
    public ResponseEntity<?> pdf(@PathVariable String id) {
        log.warn(
                "DEPRECATED API accessed: GET /api/invoices/{}/pdf - caller should use /api/cbwtf/billing/bills/{}/pdf",
                id, id);
        return ResponseEntity.status(410).body(DEPRECATED_RESPONSE);
    }

    @GetMapping
    @Deprecated(forRemoval = true)
    public ResponseEntity<?> getAll() {
        log.warn("DEPRECATED API accessed: GET /api/invoices - caller should use /api/cbwtf/billing/bills");
        return ResponseEntity.status(410).body(DEPRECATED_RESPONSE);
    }

    @GetMapping("/{id}")
    @Deprecated(forRemoval = true)
    public ResponseEntity<?> getById(@PathVariable String id) {
        log.warn("DEPRECATED API accessed: GET /api/invoices/{} - caller should use /api/cbwtf/billing/bills", id);
        return ResponseEntity.status(410).body(DEPRECATED_RESPONSE);
    }
}
