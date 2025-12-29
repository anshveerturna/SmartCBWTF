package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByBillId(UUID billId);

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

    // Dashboard metrics - count by status for facility
    long countByFacilityIdAndStatus(UUID facilityId, String status);

    // Sum amount by status for facility
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.facility.id = :facilityId AND i.status = :status")
    BigDecimal sumAmountByFacilityIdAndStatus(@Param("facilityId") UUID facilityId, @Param("status") String status);

    // Sum paid amount since date for facility
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.facility.id = :facilityId AND i.status = 'PAID' AND i.updatedAt >= :since")
    BigDecimal sumPaidAmountByFacilityIdSince(@Param("facilityId") UUID facilityId, @Param("since") Instant since);

    // Count paid since date for facility
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.facility.id = :facilityId AND i.status = 'PAID' AND i.updatedAt >= :since")
    long countPaidByFacilityIdSince(@Param("facilityId") UUID facilityId, @Param("since") Instant since);

    // Sum all paid amount for facility (total revenue)
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.facility.id = :facilityId AND i.status = 'PAID'")
    BigDecimal sumPaidAmountByFacilityId(@Param("facilityId") UUID facilityId);
}
