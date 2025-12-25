package com.smartcbwtf.service;

import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.dto.AnalyticsPageDTO;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.BagEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for Analytics Page operations.
 * All data is aggregated server-side with proper tenant isolation.
 */
@Service
public class AnalyticsPageService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsPageService.class);
    private static final ZoneId FACILITY_ZONE = ZoneId.of("Asia/Kolkata");

    private final BagEventRepository bagEventRepository;
    private final AgreementRepository agreementRepository;

    public AnalyticsPageService(BagEventRepository bagEventRepository, AgreementRepository agreementRepository) {
        this.bagEventRepository = bagEventRepository;
        this.agreementRepository = agreementRepository;
    }

    /**
     * Get total waste collected for Analytics Page.
     *
     * @param facilityId The current tenant facility
     * @param fromDate   Start date (inclusive)
     * @param toDate     End date (inclusive)
     * @param hcfId      Optional HCF filter (null = all HCFs)
     * @return TotalWasteResponse with total weight and event count
     */
    public AnalyticsPageDTO.TotalWasteResponse getTotalWaste(
            UUID facilityId, LocalDate fromDate, LocalDate toDate, UUID hcfId) {

        Instant fromInstant = fromDate.atStartOfDay(FACILITY_ZONE).toInstant();
        Instant toInstant = toDate.plusDays(1).atStartOfDay(FACILITY_ZONE).toInstant();

        BigDecimal totalWeight;
        long eventCount;

        if (hcfId != null) {
            totalWeight = bagEventRepository.sumWeightByFacilityAndHcfAndDateRange(
                    facilityId, hcfId, fromInstant, toInstant);
            eventCount = 0; // Count per HCF not needed in this view
        } else {
            totalWeight = bagEventRepository.sumWeightByFacilityAndDateRange(
                    facilityId, fromInstant, toInstant);
            eventCount = bagEventRepository.countEventsByFacilityAndDateRange(
                    facilityId, fromInstant, toInstant);
        }

        totalWeight = totalWeight != null ? totalWeight : BigDecimal.ZERO;
        String periodLabel = formatPeriodLabel(fromDate, toDate);

        log.debug("getTotalWaste: facility={}, from={}, to={}, hcfId={}, total={}kg, events={}",
                facilityId, fromDate, toDate, hcfId, totalWeight, eventCount);

        return new AnalyticsPageDTO.TotalWasteResponse(
                totalWeight.setScale(2, RoundingMode.HALF_UP),
                periodLabel,
                eventCount);
    }

    /**
     * Get waste breakdown by category for Analytics Page.
     * Percentages are computed server-side.
     *
     * @param facilityId The current tenant facility
     * @param fromDate   Start date (inclusive)
     * @param toDate     End date (inclusive)
     * @param hcfId      Optional HCF filter (null = all HCFs)
     * @return WasteByCategoryResponse with category breakdown
     */
    public AnalyticsPageDTO.WasteByCategoryResponse getWasteByCategory(
            UUID facilityId, LocalDate fromDate, LocalDate toDate, UUID hcfId) {

        Instant fromInstant = fromDate.atStartOfDay(FACILITY_ZONE).toInstant();
        Instant toInstant = toDate.plusDays(1).atStartOfDay(FACILITY_ZONE).toInstant();

        List<Object[]> rawData;
        if (hcfId != null) {
            rawData = bagEventRepository.sumWeightGroupedByCategoryForHcf(
                    facilityId, hcfId, fromInstant, toInstant);
        } else {
            rawData = bagEventRepository.sumWeightGroupedByCategoryForFacility(
                    facilityId, fromInstant, toInstant);
        }

        // Calculate grand total first
        BigDecimal grandTotal = BigDecimal.ZERO;
        List<AnalyticsPageDTO.CategoryBreakdown> categories = new ArrayList<>();

        for (Object[] row : rawData) {
            String category = (String) row[0];
            BigDecimal weight = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            grandTotal = grandTotal.add(weight);
            categories.add(new AnalyticsPageDTO.CategoryBreakdown(
                    category,
                    weight.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO)); // Placeholder
        }

        // Compute percentages server-side
        List<AnalyticsPageDTO.CategoryBreakdown> withPercent = new ArrayList<>();
        for (AnalyticsPageDTO.CategoryBreakdown c : categories) {
            BigDecimal percent = BigDecimal.ZERO;
            if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                percent = c.weightKg()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(grandTotal, 2, RoundingMode.HALF_UP);
            }
            withPercent.add(new AnalyticsPageDTO.CategoryBreakdown(
                    c.category(),
                    c.weightKg(),
                    percent));
        }

        log.debug("getWasteByCategory: facility={}, from={}, to={}, hcfId={}, categories={}, total={}kg",
                facilityId, fromDate, toDate, hcfId, withPercent.size(), grandTotal);

        return new AnalyticsPageDTO.WasteByCategoryResponse(
                withPercent,
                grandTotal.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Get list of HCFs with ACTIVE agreements for dropdown.
     *
     * @param facilityId The current tenant facility
     * @return List of HcfOption for dropdown
     */
    public List<AnalyticsPageDTO.HcfOption> getActiveHcfs(UUID facilityId) {
        List<Hcf> hcfs = agreementRepository.findHcfsByFacilityId(facilityId);

        return hcfs.stream()
                .map(hcf -> new AnalyticsPageDTO.HcfOption(hcf.getId(), hcf.getName()))
                .toList();
    }

    /**
     * Format period label for display.
     */
    private String formatPeriodLabel(LocalDate from, LocalDate to) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy");
        return from.format(fmt) + " - " + to.format(fmt);
    }
}
