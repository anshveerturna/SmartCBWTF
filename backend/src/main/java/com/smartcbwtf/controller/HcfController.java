package com.smartcbwtf.controller;

import com.smartcbwtf.dto.HcfApprovalRequest;
import com.smartcbwtf.dto.HcfApprovalResponse;
import com.smartcbwtf.dto.HcfRegistrationRequest;
import com.smartcbwtf.dto.HcfRegistrationResponse;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.service.AgreementService;
import com.smartcbwtf.service.HcfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hcfs")
public class HcfController {

    private final HcfService hcfService;
    private final AgreementService agreementService;

    public HcfController(HcfService hcfService, AgreementService agreementService) {
        this.hcfService = hcfService;
        this.agreementService = agreementService;
    }

    @PostMapping("/register")
    public ResponseEntity<HcfRegistrationResponse> register(@Valid @RequestBody HcfRegistrationRequest request) {
        HcfRegistrationResponse response = hcfService.register(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public List<Hcf> pending() {
        return hcfService.listPending();
    }

    @PostMapping("/{hcfId}/approve")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public HcfApprovalResponse approve(@PathVariable UUID hcfId, @Valid @RequestBody HcfApprovalRequest request) {
        return agreementService.approveHcf(hcfId, request);
    }
}
