package com.smartcbwtf.repository;

import com.smartcbwtf.domain.SystemError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SystemErrorRepository extends JpaRepository<SystemError, UUID> {

    // Count unresolved errors
    long countByStatus(String status);

    default long countOpen() {
        return countByStatus("OPEN");
    }

    default long countInProgress() {
        return countByStatus("IN_PROGRESS");
    }

    // Count by severity (for dashboard stats)
    long countByStatusAndSeverity(String status, String severity);

    default long countOpenCritical() {
        return countByStatusAndSeverity("OPEN", "CRITICAL");
    }

    default long countOpenErrors() {
        return countByStatusAndSeverity("OPEN", "ERROR");
    }

    default long countOpenWarnings() {
        return countByStatusAndSeverity("OPEN", "WARNING");
    }

    // Find errors for specific facility
    Page<SystemError> findByFacilityIdOrderByCreatedAtDesc(UUID facilityId, Pageable pageable);

    // Find by status (paginated)
    Page<SystemError> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    // Find by severity (paginated)
    Page<SystemError> findBySeverityOrderByCreatedAtDesc(String severity, Pageable pageable);

    // Find by status and severity
    Page<SystemError> findByStatusAndSeverityOrderByCreatedAtDesc(String status, String severity, Pageable pageable);

    // Find open errors (most recent first)
    @Query("SELECT e FROM SystemError e WHERE e.status IN ('OPEN', 'IN_PROGRESS') ORDER BY " +
            "CASE e.severity WHEN 'CRITICAL' THEN 1 WHEN 'ERROR' THEN 2 WHEN 'WARNING' THEN 3 ELSE 4 END, " +
            "e.createdAt DESC")
    Page<SystemError> findUnresolvedOrderedBySeverity(Pageable pageable);

    // Find recent open errors (for dashboard)
    @Query("SELECT e FROM SystemError e WHERE e.status = 'OPEN' ORDER BY " +
            "CASE e.severity WHEN 'CRITICAL' THEN 1 WHEN 'ERROR' THEN 2 WHEN 'WARNING' THEN 3 ELSE 4 END, " +
            "e.createdAt DESC")
    List<SystemError> findTop10OpenOrderedBySeverity();

    // Check for duplicate auto-detected errors (to avoid spamming)
    boolean existsByTitleAndSourceAndStatusIn(String title, String source, List<String> statuses);

    default boolean hasOpenAutoDetectedError(String title) {
        return existsByTitleAndSourceAndStatusIn(title, "AUTO_DETECTED", List.of("OPEN", "IN_PROGRESS"));
    }

    // Find all open auto-detected errors (for auto-resolution check)
    @Query("SELECT e FROM SystemError e WHERE e.source = 'AUTO_DETECTED' AND e.status = 'OPEN'")
    List<SystemError> findOpenAutoDetectedErrors();

    // Search errors
    @Query("SELECT e FROM SystemError e WHERE " +
            "(LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY e.createdAt DESC")
    Page<SystemError> searchByTitleOrDescription(@Param("search") String search, Pageable pageable);

    // All errors ordered by severity and date
    @Query("SELECT e FROM SystemError e ORDER BY " +
            "CASE e.status WHEN 'OPEN' THEN 1 WHEN 'IN_PROGRESS' THEN 2 ELSE 3 END, " +
            "CASE e.severity WHEN 'CRITICAL' THEN 1 WHEN 'ERROR' THEN 2 WHEN 'WARNING' THEN 3 ELSE 4 END, " +
            "e.createdAt DESC")
    Page<SystemError> findAllOrdered(Pageable pageable);
}
