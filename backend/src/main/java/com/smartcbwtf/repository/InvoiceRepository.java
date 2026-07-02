package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @Query("SELECT i FROM Invoice i WHERE i.id = :id AND i.facility.id = :facilityId")
    @EntityGraph(attributePaths = { "bill", "bill.agreement", "bill.agreement.hcf" })
    Optional<Invoice> findByIdAndFacilityId(@Param("id") UUID id, @Param("facilityId") UUID facilityId);

    @Query("SELECT i FROM Invoice i WHERE i.bill.id = :billId AND i.facility.id = :facilityId")
    @EntityGraph(attributePaths = { "bill", "bill.agreement", "bill.agreement.hcf" })
    Optional<Invoice> findByBillIdAndFacilityId(@Param("billId") UUID billId, @Param("facilityId") UUID facilityId);

    @EntityGraph(attributePaths = { "bill" })
    List<Invoice> findByBillIdIn(List<UUID> billIds);

    // Sum of all paid invoice amounts for revenue calculation
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status = 'PAID'")
    Optional<BigDecimal> sumPaidAmount();

    // Master Data queries for SuperAdmin
    @EntityGraph(attributePaths = { "bill", "bill.agreement", "bill.agreement.hcf" })
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

    // Sum all invoice amounts for facility. Payment allocation is tracked in invoice_payment.
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.facility.id = :facilityId")
    BigDecimal sumTotalAmountByFacilityId(@Param("facilityId") UUID facilityId);

    // Find unpaid invoices for HCF sorted by date (FIFO for payment allocation)
    @Query("SELECT i FROM Invoice i WHERE i.hcf.id = :hcfId AND i.status != 'PAID' ORDER BY i.invoiceDate ASC")
    List<Invoice> findUnpaidByHcfIdOrderByDateAsc(@Param("hcfId") UUID hcfId);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.hcf.id = :hcfId AND (i.status IS NULL OR i.status <> 'PAID')")
    long countUnpaidByHcfId(@Param("hcfId") UUID hcfId);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.facility.id = :facilityId AND i.hcf.id = :hcfId AND (i.status IS NULL OR i.status <> 'PAID')")
    long countUnpaidByFacilityIdAndHcfId(@Param("facilityId") UUID facilityId, @Param("hcfId") UUID hcfId);

    @Query("SELECT i FROM Invoice i WHERE i.facility.id = :facilityId AND i.hcf.id = :hcfId AND i.status != 'PAID' ORDER BY i.invoiceDate ASC")
    List<Invoice> findUnpaidByFacilityAndHcfOrderByDateAsc(@Param("facilityId") UUID facilityId,
            @Param("hcfId") UUID hcfId);

    @Query("""
            SELECT i FROM Invoice i
            LEFT JOIN FETCH i.hcf
            WHERE i.facility.id = :facilityId
              AND (i.status IS NULL OR i.status <> 'PAID')
            ORDER BY i.invoiceDate ASC
            """)
    List<Invoice> findUnpaidByFacilityIdWithHcf(@Param("facilityId") UUID facilityId);
}
