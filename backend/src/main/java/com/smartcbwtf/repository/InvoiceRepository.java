package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    // Master Data queries for SuperAdmin
    Page<Invoice> findByFacilityId(UUID facilityId, Pageable pageable);

    Page<Invoice> findByStatus(String status, Pageable pageable);

    Page<Invoice> findByFacilityIdAndStatus(UUID facilityId, String status, Pageable pageable);
}
