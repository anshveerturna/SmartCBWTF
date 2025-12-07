package com.smartcbwtf.controller;

import com.smartcbwtf.dto.InvoiceGenerateRequest;
import com.smartcbwtf.dto.InvoiceResponse;
import com.smartcbwtf.domain.Invoice;
import com.smartcbwtf.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public InvoiceResponse generate(@Valid @RequestBody InvoiceGenerateRequest request) {
        return new InvoiceResponse(invoiceService.generate(request));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public List<InvoiceResponse> list() {
        return invoiceService.listAll().stream().map(InvoiceResponse::new).toList();
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public ResponseEntity<FileSystemResource> pdf(@PathVariable UUID id) {
        Invoice invoice = invoiceService.get(id);
        File file = new File(invoice.getPdfUrl());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .body(resource);
    }
}
