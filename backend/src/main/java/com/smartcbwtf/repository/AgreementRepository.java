package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Hcf;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, UUID> {

        Optional<Agreement> findByAgreementNumber(String agreementNumber);

        @Query("SELECT a.agreementNumber FROM Agreement a WHERE a.facility.id = :facilityId")
        List<String> findAgreementNumbersByFacilityId(@Param("facilityId") UUID facilityId);

        Optional<Agreement> findFirstByHcfIdAndStatusOrderByStartDateDesc(UUID hcfId, String status);

        // Find agreements with pending dues for HCF
        @Query("SELECT a FROM Agreement a WHERE a.hcf.id = :hcfId AND a.duesStatus = :duesStatus")
        List<Agreement> findByHcfIdAndDuesStatus(@Param("hcfId") UUID hcfId, @Param("duesStatus") String duesStatus);

        // Find agreements with specific status for HCF
        @Query("SELECT a FROM Agreement a WHERE a.hcf.id = :hcfId AND a.status = :status")
        List<Agreement> findByHcfIdAndStatus(@Param("hcfId") UUID hcfId, @Param("status") String status);

        @Query("""
                        SELECT a.hcf.id, a.agreementNumber
                        FROM Agreement a
                        WHERE a.facility.id = :facilityId
                          AND a.hcf.id IN :hcfIds
                          AND a.status = 'ACTIVE'
                        ORDER BY a.createdAt DESC
                        """)
        List<Object[]> findActiveAgreementNumbersByFacilityAndHcfIds(
                        @Param("facilityId") UUID facilityId,
                        @Param("hcfIds") List<UUID> hcfIds);

        // Find active agreements under a CBWTF
        @Query("SELECT a FROM Agreement a WHERE a.facility.id = :facilityId AND a.status = 'ACTIVE'")
        List<Agreement> findActiveByFacilityId(@Param("facilityId") UUID facilityId);

        // Find HCFs under a CBWTF (through active agreements)
        @Query("SELECT a.hcf FROM Agreement a WHERE a.facility.id = :facilityId AND a.status = 'ACTIVE'")
        List<Hcf> findHcfsByFacilityId(@Param("facilityId") UUID facilityId);

        @Query("""
                        SELECT DISTINCT h FROM Agreement a
                        JOIN a.hcf h
                        WHERE a.facility.id = :facilityId
                          AND a.status = 'ACTIVE'
                          AND h.gpsLat IS NOT NULL
                          AND h.gpsLon IS NOT NULL
                        ORDER BY h.name ASC
                        """)
        List<Hcf> findMobileActiveHcfsByFacilityId(@Param("facilityId") UUID facilityId);

        // Find active agreement for HCF under specific CBWTF
        @Query("SELECT a FROM Agreement a WHERE a.hcf.id = :hcfId AND a.facility.id = :facilityId AND a.status = 'ACTIVE'")
        Optional<Agreement> findActiveByHcfAndFacility(@Param("hcfId") UUID hcfId,
                        @Param("facilityId") UUID facilityId);

        // Find ACTIVE or UPCOMING agreement for HCF under specific CBWTF (for PDF download — agreements should be downloadable before start date)
        @Query("SELECT a FROM Agreement a WHERE a.hcf.id = :hcfId AND a.facility.id = :facilityId AND a.status IN ('ACTIVE', 'UPCOMING') ORDER BY a.createdAt DESC")
        Optional<Agreement> findActiveOrUpcomingByHcfAndFacility(@Param("hcfId") UUID hcfId,
                        @Param("facilityId") UUID facilityId);

        // Count active agreements for CBWTF (dashboard metric)
        @Query("SELECT COUNT(a) FROM Agreement a WHERE a.facility.id = :facilityId AND a.status = 'ACTIVE'")
        long countActiveByFacilityId(@Param("facilityId") UUID facilityId);

        @Query("SELECT COUNT(DISTINCT a.hcf.id) FROM Agreement a WHERE a.facility.id = :facilityId AND a.status = 'ACTIVE'")
        long countDistinctActiveHcfsByFacilityId(@Param("facilityId") UUID facilityId);

        // Count all agreements for facility
        long countByFacilityId(UUID facilityId);

        // Count by status
        @Query("SELECT COUNT(a) FROM Agreement a WHERE a.facility.id = :facilityId AND a.status = :status")
        long countByFacilityIdAndStatus(@Param("facilityId") UUID facilityId, @Param("status") String status);

        // Count by dues status
        @Query("SELECT COUNT(a) FROM Agreement a WHERE a.facility.id = :facilityId AND a.duesStatus = :duesStatus")
        long countByFacilityIdAndDuesStatus(@Param("facilityId") UUID facilityId,
                        @Param("duesStatus") String duesStatus);

        // Count expiring soon (within date)
        @Query("SELECT COUNT(a) FROM Agreement a WHERE a.facility.id = :facilityId AND a.status = 'ACTIVE' AND a.endDate <= :endDate")
        long countExpiringSoonByFacilityId(@Param("facilityId") UUID facilityId,
                        @Param("endDate") java.time.LocalDate endDate);

        // Find expiring agreements for dashboard
        @Query("SELECT a FROM Agreement a WHERE a.facility.id = :facilityId AND a.status = 'ACTIVE' AND a.endDate <= :endDate ORDER BY a.endDate ASC")
        List<Agreement> findExpiringSoonByFacilityId(@Param("facilityId") UUID facilityId,
                        @Param("endDate") java.time.LocalDate endDate);

        @Query("SELECT a FROM Agreement a WHERE a.facility.id = :facilityId AND a.status = 'ACTIVE' AND a.endDate <= :endDate ORDER BY a.endDate ASC")
        List<Agreement> findExpiringSoonByFacilityId(@Param("facilityId") UUID facilityId,
                        @Param("endDate") java.time.LocalDate endDate, Pageable pageable);

        @Query("""
                        SELECT a FROM Agreement a
                        JOIN FETCH a.hcf
                        WHERE a.facility.id = :facilityId
                          AND a.status = 'ACTIVE'
                          AND a.endDate IS NOT NULL
                          AND a.endDate BETWEEN :fromDate AND :toDate
                        ORDER BY a.endDate ASC
                        """)
        List<Agreement> findActiveExpiringBetweenByFacilityId(
                        @Param("facilityId") UUID facilityId,
                        @Param("fromDate") java.time.LocalDate fromDate,
                        @Param("toDate") java.time.LocalDate toDate);

        // Find latest agreement for each HCF under a CBWTF (includes all statuses)
        @Query("""
                        SELECT a FROM Agreement a
                        JOIN FETCH a.hcf h
                        WHERE a.facility.id = :facilityId
                          AND a.createdAt = (
                              SELECT MAX(a2.createdAt)
                              FROM Agreement a2
                              WHERE a2.hcf.id = a.hcf.id
                                AND a2.facility.id = :facilityId
                          )
                        ORDER BY h.name ASC
                        """)
        List<Agreement> findLatestAgreementsByFacilityId(@Param("facilityId") UUID facilityId);

        @Query("""
                        SELECT a FROM Agreement a
                        JOIN FETCH a.hcf h
                        WHERE a.facility.id = :facilityId
                          AND a.createdAt = (
                              SELECT MAX(a2.createdAt)
                              FROM Agreement a2
                              WHERE a2.hcf.id = a.hcf.id
                                AND a2.facility.id = :facilityId
                          )
                        ORDER BY h.name ASC
                        """)
        List<Agreement> findLatestAgreementsByFacilityId(
                        @Param("facilityId") UUID facilityId,
                        Pageable pageable);

        @Query("""
                        SELECT a FROM Agreement a
                        JOIN FETCH a.hcf h
                        WHERE a.facility.id = :facilityId
                          AND a.status = 'PENDING_APPROVAL'
                          AND h.status = 'PENDING_APPROVAL'
                          AND a.createdAt = (
                              SELECT MAX(a2.createdAt)
                              FROM Agreement a2
                              WHERE a2.hcf.id = a.hcf.id
                                AND a2.facility.id = :facilityId
                          )
                        ORDER BY h.createdAt DESC
                        """)
        List<Agreement> findPendingApprovalAgreementsByFacilityId(
                        @Param("facilityId") UUID facilityId,
                        Pageable pageable);

        @Query("""
                        SELECT a FROM Agreement a
                        JOIN FETCH a.hcf h
                        WHERE a.facility.id = :facilityId
                          AND h.status = 'PENDING_APPROVAL'
                          AND a.createdAt = (
                              SELECT MAX(a2.createdAt)
                              FROM Agreement a2
                              WHERE a2.hcf.id = a.hcf.id
                                AND a2.facility.id = :facilityId
                          )
                        ORDER BY h.createdAt DESC
                        """)
        List<Agreement> findLatestPendingHcfAgreementsByFacilityId(
                        @Param("facilityId") UUID facilityId,
                        Pageable pageable);

        @Query("""
                        SELECT a FROM Agreement a
                        JOIN FETCH a.hcf h
                        WHERE a.facility.id = :facilityId
                          AND (
                              h.status = 'PENDING_APPROVAL'
                              OR a.status = 'PENDING_APPROVAL'
                              OR (h.status = 'REJECTED' AND h.rejectionCount = 1)
                          )
                          AND a.createdAt = (
                              SELECT MAX(a2.createdAt)
                              FROM Agreement a2
                              WHERE a2.hcf.id = a.hcf.id
                                AND a2.facility.id = :facilityId
                          )
                        ORDER BY h.createdAt DESC
                        """)
        List<Agreement> findLatestPendingOrResubmittableAgreementsByFacilityId(
                        @Param("facilityId") UUID facilityId,
                        Pageable pageable);

        @Query("""
                        SELECT a FROM Agreement a
                        JOIN FETCH a.hcf
                        WHERE a.facility.id = :facilityId
                          AND a.status = 'DRAFT'
                          AND a.createdAt = (
                              SELECT MAX(a2.createdAt)
                              FROM Agreement a2
                              WHERE a2.hcf.id = a.hcf.id
                                AND a2.facility.id = :facilityId
                          )
                        ORDER BY a.createdAt DESC
                        """)
        List<Agreement> findLatestDraftAgreementsByFacilityId(
                        @Param("facilityId") UUID facilityId,
                        Pageable pageable);

        // Find all agreements for HCF under facility, latest first
        @Query("SELECT a FROM Agreement a WHERE a.hcf.id = :hcfId AND a.facility.id = :facilityId ORDER BY a.createdAt DESC")
        List<Agreement> findAllByHcfIdAndFacilityId(@Param("hcfId") UUID hcfId, @Param("facilityId") UUID facilityId);

        @Query("""
                        SELECT a FROM Agreement a
                        JOIN FETCH a.hcf
                        WHERE a.hcf.id = :hcfId
                          AND a.facility.id = :facilityId
                        ORDER BY a.createdAt DESC
                        """)
        List<Agreement> findLatestByHcfIdAndFacilityId(
                        @Param("hcfId") UUID hcfId,
                        @Param("facilityId") UUID facilityId,
                        Pageable pageable);

        @Query("SELECT a FROM Agreement a WHERE a.hcf.id = :hcfId ORDER BY a.createdAt DESC")
        List<Agreement> findAllByHcfId(@Param("hcfId") UUID hcfId);

        long countByHcfId(UUID hcfId);

        // Find agreements by status and end date (for expiration)
        List<Agreement> findByStatusAndEndDateBefore(String status, java.time.LocalDate endDate);

        // Find agreements by status and start date (for activation of upcoming)
        List<Agreement> findByStatusAndStartDateLessThanEqual(String status, java.time.LocalDate startDate);
}
