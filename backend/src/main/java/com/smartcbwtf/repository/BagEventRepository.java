package com.smartcbwtf.repository;

import com.smartcbwtf.domain.BagEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	List<BagEvent> findByFacilityIdAndEventTypeAndEventTsBetween(UUID facilityId, String eventType, Instant start,
			Instant end);

	List<BagEvent> findByEventTypeAndAnomalyState(String eventType, String anomalyState);

	@Query("select e from BagEvent e where e.eventType = 'HCF_COLLECTION' and e.eventTs < :cutoff and not exists (select 1 from BagEvent v where v.bagLabel = e.bagLabel and v.eventType = 'CBWTF_VERIFICATION')")
	List<BagEvent> findMissingBags(@Param("cutoff") Instant cutoff);

	Optional<BagEvent> findFirstByBagLabelIdAndEventTypeOrderByEventTsDesc(UUID bagLabelId, String eventType);

	boolean existsByBagLabelIdAndEventTypeAndEventTs(UUID bagLabelId, String eventType, Instant eventTs);

	boolean existsByBagLabelIdAndEventType(UUID bagLabelId, String eventType);

	// Master Data queries for SuperAdmin
	Page<BagEvent> findByFacilityId(UUID facilityId, Pageable pageable);

	Page<BagEvent> findByEventType(String eventType, Pageable pageable);

	Page<BagEvent> findByEventTsBetween(Instant start, Instant end, Pageable pageable);

	// Dashboard metrics queries
	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.facility.id = :facilityId AND e.eventType = :eventType AND e.eventTs >= :since")
	long countByFacilityIdAndEventTypeAndEventTsAfter(
			@Param("facilityId") UUID facilityId,
			@Param("eventType") String eventType,
			@Param("since") Instant since);

	@Query("SELECT COUNT(e) FROM BagEvent e WHERE e.facility.id = :facilityId AND e.anomalyState != 'OK' AND e.eventTs >= :since")
	long countAnomaliesByFacilityIdSince(@Param("facilityId") UUID facilityId, @Param("since") Instant since);

	@Query("SELECT e FROM BagEvent e WHERE e.facility.id = :facilityId ORDER BY e.eventTs DESC LIMIT :limit")
	List<BagEvent> findRecentByFacilityId(@Param("facilityId") UUID facilityId, @Param("limit") int limit);

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

	List<BagEvent> findTop10ByHcfIdOrderByEventTsDesc(UUID hcfId);

	@Query("SELECT COALESCE(SUM(e.weightKg), 0) FROM BagEvent e WHERE e.hcf.id = :hcfId AND e.eventTs >= :start AND e.eventTs < :end")
	java.math.BigDecimal sumWeightByHcfIdAndEventTsBetween(
			@Param("hcfId") UUID hcfId,
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
}
