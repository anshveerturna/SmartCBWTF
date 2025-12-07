package com.smartcbwtf.controller;

import com.smartcbwtf.dto.BagEventSyncRequest;
import com.smartcbwtf.dto.BagEventSyncResponse;
import com.smartcbwtf.service.BagEventService;
import jakarta.validation.Valid;
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
}
