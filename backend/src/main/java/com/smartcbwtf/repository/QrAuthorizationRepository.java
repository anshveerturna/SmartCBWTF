package com.smartcbwtf.repository;

import com.smartcbwtf.domain.QrAuthorization;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QrAuthorizationRepository extends JpaRepository<QrAuthorization, UUID> {

        // Find by facility (tenant-scoped)
        List<QrAuthorization> findByFacilityIdOrderByCreatedAtDesc(UUID facilityId);

        List<QrAuthorization> findByFacilityIdOrderByCreatedAtDesc(UUID facilityId, Pageable pageable);

        // Find by HCF within facility
        List<QrAuthorization> findByFacilityIdAndHcfIdOrderByCreatedAtDesc(UUID facilityId, UUID hcfId);

        List<QrAuthorization> findByFacilityIdAndHcfIdOrderByCreatedAtDesc(UUID facilityId, UUID hcfId,
                        Pageable pageable);

        List<QrAuthorization> findByFacilityIdAndHcfIdAndStatusOrderByCreatedAtDesc(
                        UUID facilityId, UUID hcfId, String status, Pageable pageable);

        // Find by HCF only (for HCF admin portal)
        @Query("SELECT q FROM QrAuthorization q WHERE q.hcf.id = :hcfId ORDER BY q.createdAt DESC")
        List<QrAuthorization> findByHcfIdOrderByCreatedAtDesc(@Param("hcfId") UUID hcfId);

        @Query("SELECT q FROM QrAuthorization q WHERE q.hcf.id = :hcfId ORDER BY q.createdAt DESC")
        List<QrAuthorization> findByHcfIdOrderByCreatedAtDesc(@Param("hcfId") UUID hcfId, Pageable pageable);

        // Find by HCF and status (for HCF admin portal)
        @Query("SELECT q FROM QrAuthorization q WHERE q.hcf.id = :hcfId AND q.status = :status ORDER BY q.createdAt DESC")
        List<QrAuthorization> findByHcfIdAndStatusOrderByCreatedAtDesc(@Param("hcfId") UUID hcfId,
                        @Param("status") String status);

        @Query("SELECT q FROM QrAuthorization q WHERE q.hcf.id = :hcfId AND q.status = :status ORDER BY q.createdAt DESC")
        List<QrAuthorization> findByHcfIdAndStatusOrderByCreatedAtDesc(@Param("hcfId") UUID hcfId,
                        @Param("status") String status, Pageable pageable);

        // Find by status within facility
        List<QrAuthorization> findByFacilityIdAndStatusOrderByCreatedAtDesc(UUID facilityId, String status);

        List<QrAuthorization> findByFacilityIdAndStatusOrderByCreatedAtDesc(UUID facilityId, String status,
                        Pageable pageable);

        // Find by agreement
        List<QrAuthorization> findByAgreementIdOrderByCreatedAtDesc(UUID agreementId);

        // Find ACTIVE QRs that have expired (for scheduled job)
        @Query("SELECT q FROM QrAuthorization q WHERE q.status = 'ACTIVE' AND q.validTo < :now")
        List<QrAuthorization> findExpiredActiveQrs(@Param("now") Instant now);

        // Find USED QRs older than SLA threshold (for verification SLA breach
        // detection)
        @Query("SELECT q FROM QrAuthorization q WHERE q.status = 'USED' AND q.usedAt < :threshold")
        List<QrAuthorization> findUsedQrsBeyondSla(@Param("threshold") Instant threshold);

        // Find QRs for agreement status change (revoke/block)
        @Query("SELECT q FROM QrAuthorization q WHERE q.agreement.id = :agreementId AND q.status IN ('ACTIVE', 'USED')")
        List<QrAuthorization> findActiveOrUsedByAgreement(@Param("agreementId") UUID agreementId);

        // Bulk update QR status by agreement
        @Modifying
        @Query("UPDATE QrAuthorization q SET q.status = :newStatus WHERE q.agreement.id = :agreementId AND q.status IN ('ACTIVE', 'USED')")
        int updateStatusByAgreement(@Param("agreementId") UUID agreementId, @Param("newStatus") String newStatus);

        // Check for overlapping QRs (same agreement + category + overlapping period)
        @Query("SELECT COUNT(q) > 0 FROM QrAuthorization q WHERE q.agreement.id = :agreementId " +
                        "AND q.wasteCategory = :category AND q.status = 'ACTIVE' " +
                        "AND q.validFrom < :validTo AND q.validTo > :validFrom")
        boolean existsOverlapping(@Param("agreementId") UUID agreementId,
                        @Param("category") String category,
                        @Param("validFrom") Instant validFrom,
                        @Param("validTo") Instant validTo);

        // Find QR by ID with facility check (tenant isolation)
        Optional<QrAuthorization> findByIdAndFacilityId(UUID id, UUID facilityId);

        // Resolve QR authorization record by signed payload and facility.
        Optional<QrAuthorization> findFirstByQrPayloadAndFacilityId(String qrPayload, UUID facilityId);
}
