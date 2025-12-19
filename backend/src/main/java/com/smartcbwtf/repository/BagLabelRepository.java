package com.smartcbwtf.repository;

import com.smartcbwtf.domain.BagLabel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
