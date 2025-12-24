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
    Optional<Agreement> findActiveByHcfAndFacility(@Param("hcfId") UUID hcfId, @Param("facilityId") UUID facilityId);

    // Count active agreements for CBWTF (dashboard metric)
    @Query("SELECT COUNT(a) FROM Agreement a WHERE a.facility.id = :facilityId AND a.status = 'ACTIVE'")
    long countActiveByFacilityId(@Param("facilityId") UUID facilityId);
}
