package com.smartcbwtf.repository;

import com.smartcbwtf.domain.BagEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BagEventRepository extends JpaRepository<BagEvent, UUID> {

	List<BagEvent> findByHcfIdAndEventTsBetween(UUID hcfId, Instant start, Instant end);

	List<BagEvent> findByHcfIdAndEventTsBetweenOrderByEventTsDesc(UUID hcfId, Instant start, Instant end,
			Pageable pageable);

	List<BagEvent> findByFacilityIdAndEventTsBetween(UUID facilityId, Instant start, Instant end);

	List<BagEvent> findByFacilityIdAndHcfIdAndEventTsBetween(UUID facilityId, UUID hcfId, Instant start, Instant end);

	List<BagEvent> findByFacilityIdAndHcfIdAndEventTsBetweenOrderByEventTsDesc(UUID facilityId, UUID hcfId,
			Instant start, Instant end, Pageable pageable);

	List<BagEvent> findByFacilityIdAndEventTypeAndEventTsBetween(UUID facilityId, String eventType, Instant start,
			Instant end);

	List<BagEvent> findByFacilityIdAndEventTypeAndAnomalyState(UUID facilityId, String eventType, String anomalyState);

	@EntityGraph(attributePaths = { "bagLabel", "hcf" })
	List<BagEvent> findByFacilityIdAndEventTypeAndAnomalyStateOrderByEventTsDesc(
			UUID facilityId, String eventType, String anomalyState, Pageable pageable);

	@Query("select e from BagEvent e where e.facility.id = :facilityId and e.eventType = 'HCF_COLLECTION' and e.eventTs < :cutoff and not exists (select 1 from BagEvent v where v.bagLabel = e.bagLabel and v.eventType = 'CBWTF_VERIFICATION')")
	List<BagEvent> findMissingBags(@Param("facilityId") UUID facilityId, @Param("cutoff") Instant cutoff);

	@EntityGraph(attributePaths = { "bagLabel", "hcf" })
	@Query("select e from BagEvent e where e.facility.id = :facilityId and e.eventType = 'HCF_COLLECTION' and e.eventTs < :cutoff and not exists (select 1 from BagEvent v where v.bagLabel = e.bagLabel and v.eventType = 'CBWTF_VERIFICATION') order by e.eventTs desc")
	List<BagEvent> findMissingBags(@Param("facilityId") UUID facilityId, @Param("cutoff") Instant cutoff,
			Pageable pageable);

	Optional<BagEvent> findFirstByBagLabelIdAndEventTypeOrderByEventTsDesc(UUID bagLabelId, String eventType);

	boolean existsByBagLabelIdAndEventTypeAndEventTs(UUID bagLabelId, String eventType, Instant eventTs);

	boolean existsByBagLabelIdAndEventType(UUID bagLabelId, String eventType);

	// Master Data queries for SuperAdmin
	Page<BagEvent> findByFacilityId(UUID facilityId, Pageable pageable);

	Page<BagEvent> findByEventType(String eventType, Pageable pageable);

	long countByEventType(String eventType);

	Page<BagEvent> findByEventTsBetween(Instant start, Instant end, Pageable pageable);

	// Dashboard metrics queries
	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.facility.id = :facilityId AND e.eventType = :eventType AND e.eventTs >= :since")
	long countByFacilityIdAndEventTypeAndEventTsAfter(
			@Param("facilityId") UUID facilityId,
			@Param("eventType") String eventType,
			@Param("since") Instant since);

	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.facility.id = :facilityId AND e.anomalyState != 'OK' AND e.eventTs >= :since")
	long countAnomaliesByFacilityIdSince(@Param("facilityId") UUID facilityId, @Param("since") Instant since);

	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.facility.id = :facilityId AND e.eventType = 'HCF_COLLECTION' AND e.eventTs >= :since AND NOT EXISTS (SELECT 1 FROM BagEvent v WHERE v.bagLabel = e.bagLabel AND v.eventType = 'CBWTF_VERIFICATION')")
	long countMissingVerificationsByFacilitySince(@Param("facilityId") UUID facilityId, @Param("since") Instant since);

	@Query("SELECT e FROM BagEvent e WHERE e.facility.id = :facilityId ORDER BY e.eventTs DESC LIMIT :limit")
	List<BagEvent> findRecentByFacilityId(@Param("facilityId") UUID facilityId, @Param("limit") int limit);

	@EntityGraph(attributePaths = { "bagLabel", "hcf" })
	@Query("""
			SELECT e FROM BagEvent e
			WHERE e.facility.id = :facilityId
			  AND e.eventTs >= :since
			  AND e.anomalyState IS NOT NULL
			  AND e.anomalyState <> 'OK'
			ORDER BY e.eventTs DESC
			""")
	List<BagEvent> findRecentAnomaliesByFacilityIdSince(
			@Param("facilityId") UUID facilityId,
			@Param("since") Instant since,
			Pageable pageable);

	// Count by waste category for dashboard charts
	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.facility.id = :facilityId AND e.bagLabel IS NOT NULL AND e.bagLabel.category = :category AND e.eventTs >= :since")
	long countByFacilityIdAndWasteCategoryAndEventTsAfter(
			@Param("facilityId") UUID facilityId,
			@Param("category") String category,
			@Param("since") Instant since);

	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.facility.id = :facilityId AND e.bagLabel IS NOT NULL AND e.bagLabel.category = :category AND e.eventTs >= :start AND e.eventTs < :end")
	long countByFacilityIdAndWasteCategoryBetween(
			@Param("facilityId") UUID facilityId,
			@Param("category") String category,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.facility.id = :facilityId AND e.eventType = :eventType AND e.eventTs >= :start AND e.eventTs < :end")
	long countByFacilityIdAndEventTypeBetween(
			@Param("facilityId") UUID facilityId,
			@Param("eventType") String eventType,
			@Param("start") Instant start,
			@Param("end") Instant end);

	// HCF Dashboard Queries
	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.hcf.id = :hcfId AND e.eventTs >= :start AND e.eventTs < :end")
	long countByHcfIdAndEventTsBetween(
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("""
			SELECT COUNT(e)
			FROM BagEvent e
			WHERE e.facility.id = :facilityId
			  AND e.hcf.id = :hcfId
			  AND e.eventTs >= :start
			  AND e.eventTs < :end
			""")
	long countByFacilityIdAndHcfIdAndEventTsBetween(
			@Param("facilityId") UUID facilityId,
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	List<BagEvent> findTop10ByHcfIdOrderByEventTsDesc(UUID hcfId);

	@Query("SELECT COALESCE(SUM(e.weightKg), 0) FROM BagEvent e WHERE e.hcf.id = :hcfId AND e.eventTs >= :start AND e.eventTs < :end")
	java.math.BigDecimal sumWeightByHcfIdAndEventTsBetween(
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("""
			SELECT COALESCE(SUM(e.weightKg), 0)
			FROM BagEvent e
			WHERE e.facility.id = :facilityId
			  AND e.hcf.id = :hcfId
			  AND e.eventTs >= :start
			  AND e.eventTs < :end
			""")
	java.math.BigDecimal sumWeightByFacilityIdAndHcfIdAndEventTsBetween(
			@Param("facilityId") UUID facilityId,
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.hcf.id = :hcfId AND e.eventTs >= :start AND e.eventTs < :end AND (e.anomalyState IS NULL OR e.anomalyState = 'OK')")
	long countOkByHcfIdAndEventTsBetween(
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("""
			SELECT COUNT(e)
			FROM BagEvent e
			WHERE e.facility.id = :facilityId
			  AND e.hcf.id = :hcfId
			  AND e.eventTs >= :start
			  AND e.eventTs < :end
			  AND (e.anomalyState IS NULL OR e.anomalyState = 'OK')
			""")
	long countOkByFacilityIdAndHcfIdAndEventTsBetween(
			@Param("facilityId") UUID facilityId,
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("""
			SELECT e.bagLabel.category, COALESCE(SUM(e.weightKg), 0)
			FROM BagEvent e
			WHERE e.hcf.id = :hcfId
			  AND e.eventTs >= :start
			  AND e.eventTs < :end
			GROUP BY e.bagLabel.category
			ORDER BY e.bagLabel.category
			""")
	List<Object[]> sumWeightGroupedByCategoryForHcfBetween(
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("""
			SELECT e.bagLabel.category, COALESCE(SUM(e.weightKg), 0)
			FROM BagEvent e
			WHERE e.facility.id = :facilityId
			  AND e.hcf.id = :hcfId
			  AND e.eventTs >= :start
			  AND e.eventTs < :end
			GROUP BY e.bagLabel.category
			ORDER BY e.bagLabel.category
			""")
	List<Object[]> sumWeightGroupedByCategoryForFacilityAndHcfBetween(
			@Param("facilityId") UUID facilityId,
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("""
			SELECT COALESCE(label.category, 'UNKNOWN'), COALESCE(SUM(e.weightKg), 0)
			FROM BagEvent e
			LEFT JOIN e.bagLabel label
			WHERE e.hcf.id = :hcfId
			  AND e.eventTs >= :start
			  AND e.eventTs < :end
			GROUP BY COALESCE(label.category, 'UNKNOWN')
			ORDER BY COALESCE(label.category, 'UNKNOWN')
			""")
	List<Object[]> sumWeightGroupedByCategoryForHcfBetweenIncludingUnknown(
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("""
			SELECT COALESCE(label.category, 'UNKNOWN'), COALESCE(SUM(e.weightKg), 0)
			FROM BagEvent e
			LEFT JOIN e.bagLabel label
			WHERE e.facility.id = :facilityId
			  AND e.hcf.id = :hcfId
			  AND e.eventTs >= :start
			  AND e.eventTs < :end
			GROUP BY COALESCE(label.category, 'UNKNOWN')
			ORDER BY COALESCE(label.category, 'UNKNOWN')
			""")
	List<Object[]> sumWeightGroupedByCategoryForFacilityAndHcfBetweenIncludingUnknown(
			@Param("facilityId") UUID facilityId,
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("""
			SELECT COALESCE(label.category, 'UNKNOWN'), COUNT(e), COALESCE(SUM(e.weightKg), 0)
			FROM BagEvent e
			LEFT JOIN e.bagLabel label
			WHERE e.hcf.id = :hcfId
			  AND e.eventTs >= :start
			  AND e.eventTs < :end
			GROUP BY COALESCE(label.category, 'UNKNOWN')
			ORDER BY COALESCE(label.category, 'UNKNOWN')
			""")
	List<Object[]> countAndSumWeightGroupedByCategoryForHcfBetweenIncludingUnknown(
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("""
			SELECT COALESCE(label.category, 'UNKNOWN'), COUNT(e), COALESCE(SUM(e.weightKg), 0)
			FROM BagEvent e
			LEFT JOIN e.bagLabel label
			WHERE e.facility.id = :facilityId
			  AND e.hcf.id = :hcfId
			  AND e.eventTs >= :start
			  AND e.eventTs < :end
			GROUP BY COALESCE(label.category, 'UNKNOWN')
			ORDER BY COALESCE(label.category, 'UNKNOWN')
			""")
	List<Object[]> countAndSumWeightGroupedByCategoryForFacilityAndHcfBetweenIncludingUnknown(
			@Param("facilityId") UUID facilityId,
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("""
			SELECT DATE(e.eventTs), e.bagLabel.category, COALESCE(SUM(e.weightKg), 0)
			FROM BagEvent e
			WHERE e.hcf.id = :hcfId
			  AND e.eventTs >= :start
			  AND e.eventTs < :end
			GROUP BY DATE(e.eventTs), e.bagLabel.category
			ORDER BY DATE(e.eventTs)
			""")
	List<Object[]> sumWeightGroupedByDayAndCategoryForHcf(
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("""
			SELECT DATE(e.eventTs), e.bagLabel.category, COALESCE(SUM(e.weightKg), 0)
			FROM BagEvent e
			WHERE e.facility.id = :facilityId
			  AND e.hcf.id = :hcfId
			  AND e.eventTs >= :start
			  AND e.eventTs < :end
			GROUP BY DATE(e.eventTs), e.bagLabel.category
			ORDER BY DATE(e.eventTs)
			""")
	List<Object[]> sumWeightGroupedByDayAndCategoryForFacilityAndHcf(
			@Param("facilityId") UUID facilityId,
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("""
			SELECT DATE(e.eventTs), COUNT(e), COALESCE(SUM(e.weightKg), 0),
			       SUM(CASE WHEN e.anomalyState IS NULL OR e.anomalyState = 'OK' THEN 0 ELSE 1 END)
			FROM BagEvent e
			WHERE e.hcf.id = :hcfId
			  AND e.eventTs >= :start
			  AND e.eventTs < :end
			GROUP BY DATE(e.eventTs)
			ORDER BY DATE(e.eventTs) DESC
			""")
	List<Object[]> summarizePickupsByDayForHcf(
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("""
			SELECT DATE(e.eventTs), COUNT(e), COALESCE(SUM(e.weightKg), 0),
			       SUM(CASE WHEN e.anomalyState IS NULL OR e.anomalyState = 'OK' THEN 0 ELSE 1 END)
			FROM BagEvent e
			WHERE e.facility.id = :facilityId
			  AND e.hcf.id = :hcfId
			  AND e.eventTs >= :start
			  AND e.eventTs < :end
			GROUP BY DATE(e.eventTs)
			ORDER BY DATE(e.eventTs) DESC
			""")
	List<Object[]> summarizePickupsByDayForFacilityAndHcf(
			@Param("facilityId") UUID facilityId,
			@Param("hcfId") UUID hcfId,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.hcf.id = :hcfId AND e.bagLabel.category = :category AND e.eventTs >= :start AND e.eventTs < :end")
	long countByHcfIdAndCategoryAndEventTsBetween(
			@Param("hcfId") UUID hcfId,
			@Param("category") String category,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.facility.id = :facilityId AND e.hcf.id = :hcfId AND e.bagLabel.category = :category AND e.eventTs >= :start AND e.eventTs < :end")
	long countByFacilityIdAndHcfIdAndCategoryAndEventTsBetween(
			@Param("facilityId") UUID facilityId,
			@Param("hcfId") UUID hcfId,
			@Param("category") String category,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.hcf.id = :hcfId AND e.bagLabel.category = :category AND e.eventTs >= :start AND e.eventTs < :end AND (e.anomalyState IS NULL OR e.anomalyState = 'OK')")
	long countOkByHcfIdAndCategoryAndEventTsBetween(
			@Param("hcfId") UUID hcfId,
			@Param("category") String category,
			@Param("start") Instant start,
			@Param("end") Instant end);

	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.facility.id = :facilityId AND e.hcf.id = :hcfId AND e.bagLabel.category = :category AND e.eventTs >= :start AND e.eventTs < :end AND (e.anomalyState IS NULL OR e.anomalyState = 'OK')")
	long countOkByFacilityIdAndHcfIdAndCategoryAndEventTsBetween(
			@Param("facilityId") UUID facilityId,
			@Param("hcfId") UUID hcfId,
			@Param("category") String category,
			@Param("start") Instant start,
			@Param("end") Instant end);

	List<BagEvent> findByHcfIdAndEventTsAfter(UUID hcfId, Instant since);

	// =====================================================
	// ANALYTICS PAGE QUERIES - Scoped by ACTIVE agreements
	// =====================================================

	/**
	 * Sum total weight for facility within date range.
	 * Only includes events from HCFs with ACTIVE agreements.
	 */
	@Query("""
			SELECT COALESCE(SUM(e.weightKg), 0)
			FROM BagEvent e
			WHERE e.facility.id = :facilityId
			AND e.eventTs >= :fromInstant
			AND e.eventTs < :toInstant
			AND EXISTS (
				SELECT 1 FROM Agreement a
				WHERE a.hcf.id = e.hcf.id
				AND a.facility.id = :facilityId
				AND a.status = 'ACTIVE'
			)
			""")
	java.math.BigDecimal sumWeightByFacilityAndDateRange(
			@Param("facilityId") UUID facilityId,
			@Param("fromInstant") Instant fromInstant,
			@Param("toInstant") Instant toInstant);

	/**
	 * Sum total weight for specific HCF within date range.
	 * Verifies HCF has ACTIVE agreement with facility.
	 */
	@Query("""
			SELECT COALESCE(SUM(e.weightKg), 0)
			FROM BagEvent e
			WHERE e.facility.id = :facilityId
			AND e.hcf.id = :hcfId
			AND e.eventTs >= :fromInstant
			AND e.eventTs < :toInstant
			AND EXISTS (
				SELECT 1 FROM Agreement a
				WHERE a.hcf.id = :hcfId
				AND a.facility.id = :facilityId
				AND a.status = 'ACTIVE'
			)
			""")
	java.math.BigDecimal sumWeightByFacilityAndHcfAndDateRange(
			@Param("facilityId") UUID facilityId,
			@Param("hcfId") UUID hcfId,
			@Param("fromInstant") Instant fromInstant,
			@Param("toInstant") Instant toInstant);

	/**
	 * Count events for facility within date range (ACTIVE agreements only).
	 */
	@Query("""
			SELECT COUNT(e)
			FROM BagEvent e
			WHERE e.facility.id = :facilityId
			AND e.eventTs >= :fromInstant
			AND e.eventTs < :toInstant
			AND EXISTS (
				SELECT 1 FROM Agreement a
				WHERE a.hcf.id = e.hcf.id
				AND a.facility.id = :facilityId
				AND a.status = 'ACTIVE'
			)
			""")
	long countEventsByFacilityAndDateRange(
			@Param("facilityId") UUID facilityId,
			@Param("fromInstant") Instant fromInstant,
			@Param("toInstant") Instant toInstant);

	/**
	 * Sum weight grouped by category for facility (ACTIVE agreements only).
	 * Returns Object[] with [category, sumWeight].
	 */
	@Query("""
			SELECT e.bagLabel.category, COALESCE(SUM(e.weightKg), 0)
			FROM BagEvent e
			WHERE e.facility.id = :facilityId
			AND e.eventTs >= :fromInstant
			AND e.eventTs < :toInstant
			AND EXISTS (
				SELECT 1 FROM Agreement a
				WHERE a.hcf.id = e.hcf.id
				AND a.facility.id = :facilityId
				AND a.status = 'ACTIVE'
			)
			GROUP BY e.bagLabel.category
			ORDER BY e.bagLabel.category
			""")
	List<Object[]> sumWeightGroupedByCategoryForFacility(
			@Param("facilityId") UUID facilityId,
			@Param("fromInstant") Instant fromInstant,
			@Param("toInstant") Instant toInstant);

	/**
	 * Sum weight grouped by category for specific HCF (ACTIVE agreement only).
	 */
	@Query("""
			SELECT e.bagLabel.category, COALESCE(SUM(e.weightKg), 0)
			FROM BagEvent e
			WHERE e.facility.id = :facilityId
			AND e.hcf.id = :hcfId
			AND e.eventTs >= :fromInstant
			AND e.eventTs < :toInstant
			AND EXISTS (
				SELECT 1 FROM Agreement a
				WHERE a.hcf.id = :hcfId
				AND a.facility.id = :facilityId
				AND a.status = 'ACTIVE'
			)
			GROUP BY e.bagLabel.category
			ORDER BY e.bagLabel.category
			""")
	List<Object[]> sumWeightGroupedByCategoryForHcf(
			@Param("facilityId") UUID facilityId,
			@Param("hcfId") UUID hcfId,
			@Param("fromInstant") Instant fromInstant,
			@Param("toInstant") Instant toInstant);

	/**
	 * Fetch paginated processed bags list for facility within date range.
	 * Only includes events from HCFs with ACTIVE agreements.
	 */
	@Query("""
			SELECT e
			FROM BagEvent e
			JOIN FETCH e.bagLabel bl
			JOIN FETCH e.hcf h
			WHERE e.facility.id = :facilityId
			AND e.eventTs >= :fromInstant
			AND e.eventTs < :toInstant
			AND EXISTS (
				SELECT 1 FROM Agreement a
				WHERE a.hcf.id = e.hcf.id
				AND a.facility.id = :facilityId
				AND a.status = 'ACTIVE'
			)
			ORDER BY e.eventTs DESC
			""")
	Page<BagEvent> findProcessedBagsForFacility(
			@Param("facilityId") UUID facilityId,
			@Param("fromInstant") Instant fromInstant,
			@Param("toInstant") Instant toInstant,
			Pageable pageable);

	/**
	 * Fetch paginated processed bags list for specific HCF within date range.
	 * Verifies HCF has ACTIVE agreement with facility.
	 */
	@Query("""
			SELECT e
			FROM BagEvent e
			JOIN FETCH e.bagLabel bl
			JOIN FETCH e.hcf h
			WHERE e.facility.id = :facilityId
			AND e.hcf.id = :hcfId
			AND e.eventTs >= :fromInstant
			AND e.eventTs < :toInstant
			AND EXISTS (
				SELECT 1 FROM Agreement a
				WHERE a.hcf.id = :hcfId
				AND a.facility.id = :facilityId
				AND a.status = 'ACTIVE'
			)
			ORDER BY e.eventTs DESC
			""")
	Page<BagEvent> findProcessedBagsForHcf(
			@Param("facilityId") UUID facilityId,
			@Param("hcfId") UUID hcfId,
			@Param("fromInstant") Instant fromInstant,
			@Param("toInstant") Instant toInstant,
			Pageable pageable);

	// =====================================================
	// HCF OPERATIONAL SUMMARY QUERIES
	// =====================================================

	/**
	 * Count total pickups (bag events) for an HCF.
	 */
	@Query("SELECT COUNT(DISTINCT DATE(e.eventTs)) FROM BagEvent e WHERE e.hcf.id = :hcfId AND e.eventType = 'HCF_COLLECTION'")
	int countPickupDaysByHcfId(@Param("hcfId") UUID hcfId);

	/**
	 * Sum total waste weight for an HCF.
	 */
	@Query("SELECT COALESCE(SUM(e.weightKg), 0) FROM BagEvent e WHERE e.hcf.id = :hcfId")
	java.math.BigDecimal sumTotalWasteByHcfId(@Param("hcfId") UUID hcfId);

	/**
	 * Get the most recent pickup time for an HCF.
	 */
	@Query("SELECT MAX(e.eventTs) FROM BagEvent e WHERE e.hcf.id = :hcfId AND e.eventType = 'HCF_COLLECTION'")
	Instant findLastPickupTimeByHcfId(@Param("hcfId") UUID hcfId);

	interface HcfLastPickup {
		UUID getHcfId();

		Instant getLastPickupAt();
	}

	@Query("""
			SELECT e.hcf.id AS hcfId, MAX(e.eventTs) AS lastPickupAt
			FROM BagEvent e
			WHERE e.hcf.id IN :hcfIds
			  AND e.eventType = 'HCF_COLLECTION'
			GROUP BY e.hcf.id
			""")
	List<HcfLastPickup> findLastPickupTimesByHcfIds(@Param("hcfIds") Collection<UUID> hcfIds);
}
