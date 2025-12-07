package com.smartcbwtf.repository;

import com.smartcbwtf.domain.BagEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BagEventRepository extends JpaRepository<BagEvent, UUID> {

	List<BagEvent> findByHcfIdAndEventTsBetween(UUID hcfId, Instant start, Instant end);

	List<BagEvent> findByFacilityIdAndEventTsBetween(UUID facilityId, Instant start, Instant end);

	List<BagEvent> findByEventTypeAndAnomalyState(String eventType, String anomalyState);

	@Query("select e from BagEvent e where e.eventType = 'HCF_COLLECTION' and e.eventTs < :cutoff and not exists (select 1 from BagEvent v where v.bagLabel = e.bagLabel and v.eventType = 'CBWTF_VERIFICATION')")
	List<BagEvent> findMissingBags(@Param("cutoff") Instant cutoff);

	Optional<BagEvent> findFirstByBagLabelIdAndEventTypeOrderByEventTsDesc(UUID bagLabelId, String eventType);

	boolean existsByBagLabelIdAndEventTypeAndEventTs(UUID bagLabelId, String eventType, Instant eventTs);
}
