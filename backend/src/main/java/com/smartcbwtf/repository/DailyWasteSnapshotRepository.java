package com.smartcbwtf.repository;

import com.smartcbwtf.domain.DailyWasteSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyWasteSnapshotRepository extends JpaRepository<DailyWasteSnapshot, UUID> {

        /**
         * Find snapshot for specific HCF on a date
         */
        Optional<DailyWasteSnapshot> findByHcfIdAndSnapshotDate(UUID hcfId, LocalDate date);

        /**
         * Find all snapshots for a facility within date range
         */
        List<DailyWasteSnapshot> findByFacilityIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                        UUID facilityId, LocalDate startDate, LocalDate endDate);

        /**
         * Find all snapshots for an HCF within date range
         */
        List<DailyWasteSnapshot> findByHcfIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                        UUID hcfId, LocalDate startDate, LocalDate endDate);

        /**
         * Find all snapshots for an HCF under a facility within date range.
         */
        List<DailyWasteSnapshot> findByFacilityIdAndHcfIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                        UUID facilityId, UUID hcfId, LocalDate startDate, LocalDate endDate);

        /**
         * Aggregate daily totals for a facility by date
         */
        @Query("""
                        SELECT d.snapshotDate,
                            SUM(d.totalBags),
                            SUM(d.totalWeightGrams),
                            SUM(d.yellowBags),
                            SUM(d.redBags),
                            SUM(d.blueBags),
                            SUM(d.whiteBags),
                            SUM(d.verifiedBags),
                            SUM(d.discrepancyCount),
                            SUM(d.missingBags)
                        FROM DailyWasteSnapshot d
                        WHERE d.facility.id = :facilityId
                        AND d.snapshotDate BETWEEN :startDate AND :endDate
                        GROUP BY d.snapshotDate
                        ORDER BY d.snapshotDate DESC
                        """)
        List<Object[]> aggregateByFacilityAndDateRange(
                        @Param("facilityId") UUID facilityId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Get top HCFs by waste volume for a facility in date range
         */
        @Query("""
                        SELECT d.hcf.id, d.hcf.name, SUM(d.totalBags), SUM(d.totalWeightGrams)
                        FROM DailyWasteSnapshot d
                        WHERE d.facility.id = :facilityId
                        AND d.snapshotDate BETWEEN :startDate AND :endDate
                        GROUP BY d.hcf.id, d.hcf.name
                        ORDER BY SUM(d.totalWeightGrams) DESC
                        """)
        List<Object[]> findTopHcfsByWasteVolume(
                        @Param("facilityId") UUID facilityId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Delete old snapshots for data retention
         */
        void deleteBySnapshotDateBefore(LocalDate date);
}
