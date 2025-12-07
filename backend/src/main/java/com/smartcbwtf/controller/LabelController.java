package com.smartcbwtf.controller;

import com.smartcbwtf.dto.LabelIssueRequest;
import com.smartcbwtf.dto.LabelIssueResponse;
import com.smartcbwtf.service.LabelService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @PostMapping("/issue")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public LabelIssueResponse issue(@Valid @RequestBody LabelIssueRequest request) {
        return labelService.issue(request);
    }
}
