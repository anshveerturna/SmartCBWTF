package com.smartcbwtf.repository;

import com.smartcbwtf.domain.GlobalEmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for global email templates.
 * SuperAdmin-only access.
 */
@Repository
public interface GlobalEmailTemplateRepository extends JpaRepository<GlobalEmailTemplate, UUID> {

    /**
     * Find the active template for a given code.
     * Only one template per code should be active at a time.
     */
    Optional<GlobalEmailTemplate> findByTemplateCodeAndIsActiveTrue(String templateCode);

    /**
     * Find all versions of a template by code, ordered by version descending.
     */
    List<GlobalEmailTemplate> findByTemplateCodeOrderByVersionDesc(String templateCode);

    /**
     * Get all active templates (one per code).
     */
    @Query("SELECT t FROM GlobalEmailTemplate t WHERE t.isActive = true ORDER BY t.templateCode")
    List<GlobalEmailTemplate> findAllActiveTemplates();

    /**
     * Find the latest version number for a template code.
     */
    @Query("SELECT MAX(t.version) FROM GlobalEmailTemplate t WHERE t.templateCode = :templateCode")
    Optional<Integer> findLatestVersion(String templateCode);

    /**
     * Find a specific version of a template.
     */
    Optional<GlobalEmailTemplate> findByTemplateCodeAndVersion(String templateCode, Integer version);

    /**
     * Check if a template code exists.
     */
    boolean existsByTemplateCode(String templateCode);
}
