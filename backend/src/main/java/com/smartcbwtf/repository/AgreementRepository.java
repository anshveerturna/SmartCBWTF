package com.smartcbwtf.repository;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Hcf;
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

        Optional<Agreement> findFirstByHcfIdAndStatusOrderByStartDateDesc(UUID hcfId, String status);

        // Find ACTIVE agreement for HCF (globally, any CBWTF)
        @Query("SELECT a FROM Agreement a WHERE a.hcf.id = :hcfId AND a.status = 'ACTIVE'")
        Optional<Agreement> findActiveByHcfId(@Param("hcfId") UUID hcfId);

        // Find agreements with pending dues for HCF
        @Query("SELECT a FROM Agreement a WHERE a.hcf.id = :hcfId AND a.duesStatus = :duesStatus")
        List<Agreement> findByHcfIdAndDuesStatus(@Param("hcfId") UUID hcfId, @Param("duesStatus") String duesStatus);

        // Find agreements with specific status for HCF
        @Query("SELECT a FROM Agreement a WHERE a.hcf.id = :hcfId AND a.status = :status")
        List<Agreement> findByHcfIdAndStatus(@Param("hcfId") UUID hcfId, @Param("status") String status);

        // Find active agreements under a CBWTF
        @Query("SELECT a FROM Agreement a WHERE a.facility.id = :facilityId AND a.status = 'ACTIVE'")
        List<Agreement> findActiveByFacilityId(@Param("facilityId") UUID facilityId);

        // Find HCFs under a CBWTF (through active agreements)
        @Query("SELECT a.hcf FROM Agreement a WHERE a.facility.id = :facilityId AND a.status = 'ACTIVE'")
        List<Hcf> findHcfsByFacilityId(@Param("facilityId") UUID facilityId);

        // Find active agreement for HCF under specific CBWTF
        @Query("SELECT a FROM Agreement a WHERE a.hcf.id = :hcfId AND a.facility.id = :facilityId AND a.status = 'ACTIVE'")
        Optional<Agreement> findActiveByHcfAndFacility(@Param("hcfId") UUID hcfId,
                        @Param("facilityId") UUID facilityId);

        // Count active agreements for CBWTF (dashboard metric)
        @Query("SELECT COUNT(a) FROM Agreement a WHERE a.facility.id = :facilityId AND a.status = 'ACTIVE'")
        long countActiveByFacilityId(@Param("facilityId") UUID facilityId);

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

        // Find latest agreement for each HCF under a CBWTF (includes all statuses)
        @Query("SELECT a FROM Agreement a WHERE a.facility.id = :facilityId AND a.createdAt = (SELECT MAX(a2.createdAt) FROM Agreement a2 WHERE a2.hcf.id = a.hcf.id AND a2.facility.id = :facilityId)")
        List<Agreement> findLatestAgreementsByFacilityId(@Param("facilityId") UUID facilityId);

        // Find all agreements for HCF under facility, latest first
        @Query("SELECT a FROM Agreement a WHERE a.hcf.id = :hcfId AND a.facility.id = :facilityId ORDER BY a.createdAt DESC")
        List<Agreement> findAllByHcfIdAndFacilityId(@Param("hcfId") UUID hcfId, @Param("facilityId") UUID facilityId);
}
