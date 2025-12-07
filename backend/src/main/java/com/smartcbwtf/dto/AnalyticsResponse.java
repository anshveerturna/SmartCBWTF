package com.smartcbwtf.dto;

import java.math.BigDecimal;
import java.util.Map;

public class AnalyticsResponse {
    private BigDecimal totalWeightKg;
    private long bagCount;
    private Map<String, BigDecimal> weightByCategory;
    private Map<String, Long> bagCountByCategory;
    private long mismatchCount;
    private long missingCount;
    private Map<String, BigDecimal> weightTrendByDate;
    private Map<String, Long> bagCountTrendByDate;

    public AnalyticsResponse(BigDecimal totalWeightKg,
                             long bagCount,
                             Map<String, BigDecimal> weightByCategory,
                             Map<String, Long> bagCountByCategory,
                             long mismatchCount,
                             long missingCount,
                             Map<String, BigDecimal> weightTrendByDate,
                             Map<String, Long> bagCountTrendByDate) {
        this.totalWeightKg = totalWeightKg;
        this.bagCount = bagCount;
        this.weightByCategory = weightByCategory;
        this.bagCountByCategory = bagCountByCategory;
        this.mismatchCount = mismatchCount;
        this.missingCount = missingCount;
        this.weightTrendByDate = weightTrendByDate;
        this.bagCountTrendByDate = bagCountTrendByDate;
    }

    public BigDecimal getTotalWeightKg() { return totalWeightKg; }
    public void setTotalWeightKg(BigDecimal totalWeightKg) { this.totalWeightKg = totalWeightKg; }
    public long getBagCount() { return bagCount; }
    public void setBagCount(long bagCount) { this.bagCount = bagCount; }
    public Map<String, BigDecimal> getWeightByCategory() { return weightByCategory; }
    public void setWeightByCategory(Map<String, BigDecimal> weightByCategory) { this.weightByCategory = weightByCategory; }
    public Map<String, Long> getBagCountByCategory() { return bagCountByCategory; }
    public void setBagCountByCategory(Map<String, Long> bagCountByCategory) { this.bagCountByCategory = bagCountByCategory; }
    public long getMismatchCount() { return mismatchCount; }
    public void setMismatchCount(long mismatchCount) { this.mismatchCount = mismatchCount; }
    public long getMissingCount() { return missingCount; }
    public void setMissingCount(long missingCount) { this.missingCount = missingCount; }
    public Map<String, BigDecimal> getWeightTrendByDate() { return weightTrendByDate; }
    public void setWeightTrendByDate(Map<String, BigDecimal> weightTrendByDate) { this.weightTrendByDate = weightTrendByDate; }
    public Map<String, Long> getBagCountTrendByDate() { return bagCountTrendByDate; }
    public void setBagCountTrendByDate(Map<String, Long> bagCountTrendByDate) { this.bagCountTrendByDate = bagCountTrendByDate; }
}
