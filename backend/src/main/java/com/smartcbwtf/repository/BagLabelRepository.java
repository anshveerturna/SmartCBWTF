package com.smartcbwtf.repository;

import com.smartcbwtf.domain.BagLabel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BagLabelRepository extends JpaRepository<BagLabel, UUID> {
    Optional<BagLabel> findByQrCode(String qrCode);

    // Master Data queries for SuperAdmin
    Page<BagLabel> findByFacilityId(UUID facilityId, Pageable pageable);

    Page<BagLabel> findByStatus(String status, Pageable pageable);

    Page<BagLabel> findByCategory(String category, Pageable pageable);

    @Query("SELECT b FROM BagLabel b WHERE LOWER(b.qrCode) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.serialNo) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<BagLabel> searchByQrCodeOrSerial(@Param("search") String search, Pageable pageable);

    // Tenant-scoped queries (non-paginated)
    List<BagLabel> findByFacilityId(UUID facilityId);

    List<BagLabel> findByFacilityIdAndStatus(UUID facilityId, String status);

    long countByFacilityId(UUID facilityId);

    long countByFacilityIdAndStatus(UUID facilityId, String status);

    long countByFacilityIdAndIssuedAtBetween(UUID facilityId, java.time.Instant start, java.time.Instant end);

    // HCF Portal queries
    long countByHcfIdAndIssuedAtBetween(UUID hcfId, java.time.Instant start, java.time.Instant end);

    long countByFacilityIdAndHcfIdAndIssuedAtBetween(UUID facilityId, UUID hcfId, java.time.Instant start,
            java.time.Instant end);
}
