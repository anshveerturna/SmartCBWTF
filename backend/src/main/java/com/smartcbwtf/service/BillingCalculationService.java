package com.smartcbwtf.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Billing Calculation Service - Pure mathematical calculations.
 * 
 * NO SIDE EFFECTS. Same inputs ALWAYS produce same outputs.
 * All amounts in INR with 2 decimal precision.
 * GST: 9% CGST + 9% SGST = 18% total.
 */
@Service
public class BillingCalculationService {

    private static final BigDecimal GST_RATE = new BigDecimal("0.09"); // 9% each for CGST & SGST
    private static final int SCALE = 2;
    private static final int WEIGHT_SCALE = 3;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Result record for bill calculation.
     * Immutable - all values are final.
     */
    public record BillCalculation(
            BigDecimal pickupWeightKg,
            BigDecimal baseAllowanceKg,
            BigDecimal excessWeightKg,
            BigDecimal baseAmount,
            BigDecimal excessAmount,
            BigDecimal subtotal,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal totalAmount) {
    }

    /**
     * Calculate bill amounts from inputs.
     * 
     * Formula:
     * base_allowance_kg = (bed_count × base_grams_per_bed × days) / 1000
     * excess_weight_kg = MAX(0, pickup_weight_kg - base_allowance_kg)
     * base_amount = bed_count × base_rate_per_bed × days
     * excess_amount = excess_weight_kg × excess_rate_per_kg
     * subtotal = base_amount + excess_amount
     * CGST = subtotal × 0.09
     * SGST = subtotal × 0.09
     * total = subtotal + CGST + SGST
     * 
     * @param bedCount              Number of beds in HCF
     * @param daysInMonth           Number of days in billing month
     * @param baseGramsPerBedPerDay Base waste allowance in grams
     * @param baseRatePerBedPerDay  Base rate per bed per day in INR
     * @param excessRatePerKg       Rate for excess waste in INR/kg
     * @param pickupWeightKg        Total pickup weight in kg
     * @return Immutable calculation result
     */
    public BillCalculation calculate(
            int bedCount,
            int daysInMonth,
            BigDecimal baseGramsPerBedPerDay,
            BigDecimal baseRatePerBedPerDay,
            BigDecimal excessRatePerKg,
            BigDecimal pickupWeightKg) {

        // Calculate base allowance in kg
        // Formula: (beds × grams × days) / 1000
        BigDecimal baseAllowanceKg = baseGramsPerBedPerDay
                .multiply(BigDecimal.valueOf(bedCount))
                .multiply(BigDecimal.valueOf(daysInMonth))
                .divide(BigDecimal.valueOf(1000), WEIGHT_SCALE, ROUNDING);

        // Calculate excess weight (never negative)
        BigDecimal excessWeightKg = pickupWeightKg.subtract(baseAllowanceKg);
        if (excessWeightKg.compareTo(BigDecimal.ZERO) < 0) {
            excessWeightKg = BigDecimal.ZERO.setScale(WEIGHT_SCALE, ROUNDING);
        }
        excessWeightKg = excessWeightKg.setScale(WEIGHT_SCALE, ROUNDING);

        // Calculate base amount
        // Formula: beds × rate × days
        BigDecimal baseAmount = baseRatePerBedPerDay
                .multiply(BigDecimal.valueOf(bedCount))
                .multiply(BigDecimal.valueOf(daysInMonth))
                .setScale(SCALE, ROUNDING);

        // Calculate excess amount
        BigDecimal excessAmount = excessWeightKg
                .multiply(excessRatePerKg)
                .setScale(SCALE, ROUNDING);

        // Calculate subtotal
        BigDecimal subtotal = baseAmount.add(excessAmount).setScale(SCALE, ROUNDING);

        // Calculate GST (9% CGST + 9% SGST)
        BigDecimal cgst = subtotal.multiply(GST_RATE).setScale(SCALE, ROUNDING);
        BigDecimal sgst = subtotal.multiply(GST_RATE).setScale(SCALE, ROUNDING);

        // Calculate total
        BigDecimal totalAmount = subtotal.add(cgst).add(sgst).setScale(SCALE, ROUNDING);

        return new BillCalculation(
                pickupWeightKg.setScale(WEIGHT_SCALE, ROUNDING),
                baseAllowanceKg,
                excessWeightKg,
                baseAmount,
                excessAmount,
                subtotal,
                cgst,
                sgst,
                totalAmount);
    }
}
