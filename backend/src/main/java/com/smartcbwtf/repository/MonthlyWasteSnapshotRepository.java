package com.smartcbwtf.repository;

import com.smartcbwtf.domain.MonthlyWasteSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonthlyWasteSnapshotRepository extends JpaRepository<MonthlyWasteSnapshot, UUID> {

    /**
     * Find snapshot for specific facility and month
     */
    Optional<MonthlyWasteSnapshot> findByFacilityIdAndSnapshotMonth(UUID facilityId, LocalDate snapshotMonth);

    /**
     * Find all snapshots for a facility ordered by month
     */
    List<MonthlyWasteSnapshot> findByFacilityIdOrderBySnapshotMonthDesc(UUID facilityId);

    /**
     * Find snapshots for a facility within date range
     */
    List<MonthlyWasteSnapshot> findByFacilityIdAndSnapshotMonthBetweenOrderBySnapshotMonthDesc(
            UUID facilityId, LocalDate startMonth, LocalDate endMonth);

    /**
     * Get platform-wide monthly totals for SuperAdmin
     */
    @Query("""
            SELECT m.snapshotMonth,
                   COUNT(DISTINCT m.facility.id),
                   SUM(m.totalHcfsActive),
                   SUM(m.totalBags),
                   SUM(m.totalWeightGrams),
                   SUM(m.revenueInvoicedPaise),
                   SUM(m.revenueCollectedPaise)
            FROM MonthlyWasteSnapshot m
            WHERE m.snapshotMonth BETWEEN :startMonth AND :endMonth
            GROUP BY m.snapshotMonth
            ORDER BY m.snapshotMonth DESC
            """)
    List<Object[]> getPlatformMonthlyTotals(
            @Param("startMonth") LocalDate startMonth,
            @Param("endMonth") LocalDate endMonth);

    /**
     * Get latest month's data for all facilities (for ranking)
     */
    @Query("""
            SELECT m.facility.id, m.facility.name,
                   m.totalBags, m.totalWeightGrams, m.blueWastePercentage
            FROM MonthlyWasteSnapshot m
            WHERE m.snapshotMonth = :month
            ORDER BY m.totalWeightGrams DESC
            """)
    List<Object[]> getFacilityRankingForMonth(@Param("month") LocalDate month);
}
