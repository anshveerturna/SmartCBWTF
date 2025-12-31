package com.smartcbwtf.repository;

import com.smartcbwtf.domain.FacilityEmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacilityEmailTemplateRepository extends JpaRepository<FacilityEmailTemplate, UUID> {

    List<FacilityEmailTemplate> findByFacilityIdOrderByTemplateCodeAscVersionDesc(UUID facilityId);

    @Query("SELECT t FROM FacilityEmailTemplate t WHERE t.facilityId = :facilityId AND t.templateCode = :templateCode AND t.isActive = true")
    Optional<FacilityEmailTemplate> findActiveTemplate(UUID facilityId, String templateCode);

    Optional<FacilityEmailTemplate> findByFacilityIdAndTemplateCodeAndIsActiveTrue(UUID facilityId,
            String templateCode);

    @Query("SELECT MAX(t.version) FROM FacilityEmailTemplate t WHERE t.facilityId = :facilityId AND t.templateCode = :templateCode")
    Optional<Integer> findMaxVersion(UUID facilityId, String templateCode);

    List<FacilityEmailTemplate> findByFacilityIdAndTemplateCode(UUID facilityId, String templateCode);
}
