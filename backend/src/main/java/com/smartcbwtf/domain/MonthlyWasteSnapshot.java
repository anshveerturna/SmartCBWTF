package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Monthly waste snapshot - pre-aggregated analytics per facility per month.
 * Used for facility-level dashboards and compliance reporting.
 */
@Entity
@Table(name = "monthly_waste_snapshot")
public class MonthlyWasteSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "snapshot_month", nullable = false)
    private LocalDate snapshotMonth; // First day of the month

    // Rollup counts
    @Column(name = "total_hcfs_active", nullable = false)
    private int totalHcfsActive = 0;

    @Column(name = "total_pickups", nullable = false)
    private int totalPickups = 0;

    @Column(name = "total_bags", nullable = false)
    private int totalBags = 0;

    // Category breakdown
    @Column(name = "yellow_bags", nullable = false)
    private int yellowBags = 0;

    @Column(name = "red_bags", nullable = false)
    private int redBags = 0;

    @Column(name = "blue_bags", nullable = false)
    private int blueBags = 0;

    @Column(name = "white_bags", nullable = false)
    private int whiteBags = 0;

    // Weight in grams
    @Column(name = "total_weight_grams", nullable = false)
    private long totalWeightGrams = 0;

    @Column(name = "yellow_weight_grams", nullable = false)
    private long yellowWeightGrams = 0;

    @Column(name = "red_weight_grams", nullable = false)
    private long redWeightGrams = 0;

    @Column(name = "blue_weight_grams", nullable = false)
    private long blueWeightGrams = 0;

    @Column(name = "white_weight_grams", nullable = false)
    private long whiteWeightGrams = 0;

    // Blue waste compliance percentage
    @Column(name = "blue_waste_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal blueWastePercentage = BigDecimal.ZERO;

    // Revenue in paise
    @Column(name = "revenue_invoiced_paise", nullable = false)
    private long revenueInvoicedPaise = 0;

    @Column(name = "revenue_collected_paise", nullable = false)
    private long revenueCollectedPaise = 0;

    @Column(name = "revenue_outstanding_paise", nullable = false)
    private long revenueOutstandingPaise = 0;

    // Quality metrics
    @Column(name = "verified_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal verifiedPercentage = BigDecimal.ZERO;

    @Column(name = "discrepancy_count", nullable = false)
    private int discrepancyCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public LocalDate getSnapshotMonth() {
        return snapshotMonth;
    }

    public void setSnapshotMonth(LocalDate snapshotMonth) {
        this.snapshotMonth = snapshotMonth;
    }

    public int getTotalHcfsActive() {
        return totalHcfsActive;
    }

    public void setTotalHcfsActive(int totalHcfsActive) {
        this.totalHcfsActive = totalHcfsActive;
    }

    public int getTotalPickups() {
        return totalPickups;
    }

    public void setTotalPickups(int totalPickups) {
        this.totalPickups = totalPickups;
    }

    public int getTotalBags() {
        return totalBags;
    }

    public void setTotalBags(int totalBags) {
        this.totalBags = totalBags;
    }

    public int getYellowBags() {
        return yellowBags;
    }

    public void setYellowBags(int yellowBags) {
        this.yellowBags = yellowBags;
    }

    public int getRedBags() {
        return redBags;
    }

    public void setRedBags(int redBags) {
        this.redBags = redBags;
    }

    public int getBlueBags() {
        return blueBags;
    }

    public void setBlueBags(int blueBags) {
        this.blueBags = blueBags;
    }

    public int getWhiteBags() {
        return whiteBags;
    }

    public void setWhiteBags(int whiteBags) {
        this.whiteBags = whiteBags;
    }

    public long getTotalWeightGrams() {
        return totalWeightGrams;
    }

    public void setTotalWeightGrams(long totalWeightGrams) {
        this.totalWeightGrams = totalWeightGrams;
    }

    public long getYellowWeightGrams() {
        return yellowWeightGrams;
    }

    public void setYellowWeightGrams(long yellowWeightGrams) {
        this.yellowWeightGrams = yellowWeightGrams;
    }

    public long getRedWeightGrams() {
        return redWeightGrams;
    }

    public void setRedWeightGrams(long redWeightGrams) {
        this.redWeightGrams = redWeightGrams;
    }

    public long getBlueWeightGrams() {
        return blueWeightGrams;
    }

    public void setBlueWeightGrams(long blueWeightGrams) {
        this.blueWeightGrams = blueWeightGrams;
    }

    public long getWhiteWeightGrams() {
        return whiteWeightGrams;
    }

    public void setWhiteWeightGrams(long whiteWeightGrams) {
        this.whiteWeightGrams = whiteWeightGrams;
    }

    public BigDecimal getBlueWastePercentage() {
        return blueWastePercentage;
    }

    public void setBlueWastePercentage(BigDecimal blueWastePercentage) {
        this.blueWastePercentage = blueWastePercentage;
    }

    public long getRevenueInvoicedPaise() {
        return revenueInvoicedPaise;
    }

    public void setRevenueInvoicedPaise(long revenueInvoicedPaise) {
        this.revenueInvoicedPaise = revenueInvoicedPaise;
    }

    public long getRevenueCollectedPaise() {
        return revenueCollectedPaise;
    }

    public void setRevenueCollectedPaise(long revenueCollectedPaise) {
        this.revenueCollectedPaise = revenueCollectedPaise;
    }

    public long getRevenueOutstandingPaise() {
        return revenueOutstandingPaise;
    }

    public void setRevenueOutstandingPaise(long revenueOutstandingPaise) {
        this.revenueOutstandingPaise = revenueOutstandingPaise;
    }

    public BigDecimal getVerifiedPercentage() {
        return verifiedPercentage;
    }

    public void setVerifiedPercentage(BigDecimal verifiedPercentage) {
        this.verifiedPercentage = verifiedPercentage;
    }

    public int getDiscrepancyCount() {
        return discrepancyCount;
    }

    public void setDiscrepancyCount(int discrepancyCount) {
        this.discrepancyCount = discrepancyCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public double getTotalWeightKg() {
        return totalWeightGrams / 1000.0;
    }

    public double getRevenueInvoicedRupees() {
        return revenueInvoicedPaise / 100.0;
    }

    public double getRevenueCollectedRupees() {
        return revenueCollectedPaise / 100.0;
    }

    public double getRevenueOutstandingRupees() {
        return revenueOutstandingPaise / 100.0;
    }
}
