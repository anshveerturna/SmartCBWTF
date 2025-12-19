package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Hcf;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface HcfRepository extends JpaRepository<Hcf, UUID> {
    Optional<Hcf> findByCode(String code);

    // Duplicate detection queries
    Optional<Hcf> findByPanNo(String panNo);

    Optional<Hcf> findByGstNo(String gstNo);

    Optional<Hcf> findByAadharNo(String aadharNo);

    Optional<Hcf> findByContactPhone(String contactPhone);

    // Master Data queries for SuperAdmin
    Page<Hcf> findByStatus(String status, Pageable pageable);

    @Query("SELECT h FROM Hcf h WHERE LOWER(h.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(h.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Hcf> searchByNameOrCode(@Param("search") String search, Pageable pageable);
}
