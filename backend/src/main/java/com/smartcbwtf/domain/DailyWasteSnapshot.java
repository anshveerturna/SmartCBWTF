package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Daily waste snapshot - pre-aggregated analytics per HCF per day.
 * Populated by scheduled aggregation jobs for fast dashboard queries.
 */
@Entity
@Table(name = "daily_waste_snapshot")
public class DailyWasteSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hcf_id", nullable = false)
    private Hcf hcf;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    // Bag counts
    @Column(name = "total_bags", nullable = false)
    private int totalBags = 0;

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

    // Verification stats
    @Column(name = "verified_bags", nullable = false)
    private int verifiedBags = 0;

    @Column(name = "discrepancy_count", nullable = false)
    private int discrepancyCount = 0;

    @Column(name = "missing_bags", nullable = false)
    private int missingBags = 0;

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

    public Hcf getHcf() {
        return hcf;
    }

    public void setHcf(Hcf hcf) {
        this.hcf = hcf;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
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

    public int getVerifiedBags() {
        return verifiedBags;
    }

    public void setVerifiedBags(int verifiedBags) {
        this.verifiedBags = verifiedBags;
    }

    public int getDiscrepancyCount() {
        return discrepancyCount;
    }

    public void setDiscrepancyCount(int discrepancyCount) {
        this.discrepancyCount = discrepancyCount;
    }

    public int getMissingBags() {
        return missingBags;
    }

    public void setMissingBags(int missingBags) {
        this.missingBags = missingBags;
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

    // Helper method for weight in kg
    public double getTotalWeightKg() {
        return totalWeightGrams / 1000.0;
    }
}
