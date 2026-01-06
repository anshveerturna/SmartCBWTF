package com.smartcbwtf.repository;

import com.smartcbwtf.domain.BillVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for BillVersion audit records.
 */
@Repository
public interface BillVersionRepository extends JpaRepository<BillVersion, UUID> {

    /**
     * Find all versions for a bill, ordered by version number descending.
     */
    List<BillVersion> findByBillIdOrderByVersionDesc(UUID billId);

    /**
     * Find the latest version for a bill.
     */
    @Query("SELECT bv FROM BillVersion bv WHERE bv.bill.id = :billId ORDER BY bv.version DESC LIMIT 1")
    BillVersion findLatestByBillId(UUID billId);

    /**
     * Count versions for a bill.
     */
    long countByBillId(UUID billId);
}
