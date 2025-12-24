package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    // Sum of all paid invoice amounts for revenue calculation
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status = 'PAID'")
    Optional<BigDecimal> sumPaidAmount();

    // Master Data queries for SuperAdmin
    Page<Invoice> findByFacilityId(UUID facilityId, Pageable pageable);

    Page<Invoice> findByStatus(String status, Pageable pageable);

    Page<Invoice> findByFacilityIdAndStatus(UUID facilityId, String status, Pageable pageable);

    // Tenant-scoped queries (non-paginated)
    List<Invoice> findByFacilityId(UUID facilityId);

    List<Invoice> findByFacilityIdAndStatus(UUID facilityId, String status);
}
