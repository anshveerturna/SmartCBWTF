package com.smartcbwtf.controller;

import com.smartcbwtf.dto.BagEventSyncRequest;
import com.smartcbwtf.dto.BagEventSyncResponse;
import com.smartcbwtf.dto.BagVerifyRequest;
import com.smartcbwtf.dto.BagVerifyResponse;
import com.smartcbwtf.service.BagEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bags")
public class BagEventController {

    private final BagEventService bagEventService;

    public BagEventController(BagEventService bagEventService) {
        this.bagEventService = bagEventService;
    }

    @PostMapping("/events/sync")
    @PreAuthorize("hasRole('DRIVER')")
    public BagEventSyncResponse sync(@Valid @RequestBody BagEventSyncRequest request) {
        return bagEventService.sync(request);
    }

    /**
     * Dedicated verification endpoint for CBWTF facility.
     * Returns:
     * - 200 OK with verification result
     * - 403 Forbidden if device is outside facility geofence
     * - 404 Not Found if bag label doesn't exist
     * - 409 Conflict if bag was already verified (idempotency)
     */
    @PostMapping("/verify")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<BagVerifyResponse> verify(@Valid @RequestBody BagVerifyRequest request) {
        BagEventService.VerifyResult result = bagEventService.verifyBag(request);
        return ResponseEntity.status(result.getHttpStatus()).body(result.getResponse());
    }
}
