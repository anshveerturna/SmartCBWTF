package com.smartcbwtf.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BillingCalculationService.
 * 
 * CRITICAL: These tests verify that billing calculations are:
 * 1. Deterministic (same input → same output)
 * 2. Mathematically correct
 * 3. Consistent across month lengths
 * 4. Properly rounded (paise handling)
 * 
 * ANY FAILURE HERE = PRODUCTION BLOCKER
 */
class BillingCalculationServiceTest {

    private BillingCalculationService service;

    // Standard test inputs
    private static final int BEDS = 50;
    private static final BigDecimal BASE_GRAMS = new BigDecimal("500.00"); // 500g per bed per day
    private static final BigDecimal BASE_RATE = new BigDecimal("10.00"); // ₹10 per bed per day
    private static final BigDecimal EXCESS_RATE = new BigDecimal("25.00"); // ₹25 per kg excess

    @BeforeEach
    void setUp() {
        service = new BillingCalculationService();
    }

    // ========================================================================
    // DETERMINISM TESTS
    // ========================================================================

    @Nested
    @DisplayName("Determinism Tests")
    class DeterminismTests {

        @Test
        @DisplayName("Same inputs MUST produce identical outputs - 100 iterations")
        void sameinputsProduceSameOutputs() {
            BigDecimal pickup = new BigDecimal("800.000");

            var firstResult = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            // Run 100 times - must be identical
            for (int i = 0; i < 100; i++) {
                var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

                assertEquals(firstResult.baseAllowanceKg(), result.baseAllowanceKg(),
                        "baseAllowanceKg must be deterministic on iteration " + i);
                assertEquals(firstResult.excessWeightKg(), result.excessWeightKg(),
                        "excessWeightKg must be deterministic on iteration " + i);
                assertEquals(firstResult.baseAmount(), result.baseAmount(),
                        "baseAmount must be deterministic on iteration " + i);
                assertEquals(firstResult.excessAmount(), result.excessAmount(),
                        "excessAmount must be deterministic on iteration " + i);
                assertEquals(firstResult.subtotal(), result.subtotal(),
                        "subtotal must be deterministic on iteration " + i);
                assertEquals(firstResult.cgst(), result.cgst(),
                        "cgst must be deterministic on iteration " + i);
                assertEquals(firstResult.sgst(), result.sgst(),
                        "sgst must be deterministic on iteration " + i);
                assertEquals(firstResult.totalAmount(), result.totalAmount(),
                        "totalAmount must be deterministic on iteration " + i);
            }
        }
    }

    // ========================================================================
    // BOUNDARY TESTS
    // ========================================================================

    @Nested
    @DisplayName("Boundary Tests")
    class BoundaryTests {

        @Test
        @DisplayName("Zero pickup - no excess charge")
        void zeroPickup() {
            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, BigDecimal.ZERO);

            // Base allowance: 50 beds × 500g × 30 days / 1000 = 750 kg
            assertEquals(new BigDecimal("750.000"), result.baseAllowanceKg());

            // Excess should be zero (not negative!)
            assertEquals(new BigDecimal("0.000"), result.excessWeightKg());

            // Excess amount should be zero
            assertEquals(new BigDecimal("0.00"), result.excessAmount());

            // Base amount: 50 beds × ₹10 × 30 days = ₹15,000
            assertEquals(new BigDecimal("15000.00"), result.baseAmount());

            // Subtotal = base + 0 = ₹15,000
            assertEquals(new BigDecimal("15000.00"), result.subtotal());

            // GST: 15000 × 0.025 = 375 each
            assertEquals(new BigDecimal("375.00"), result.cgst());
            assertEquals(new BigDecimal("375.00"), result.sgst());

            // Total: 15000 + 375 + 375 = 15750
            assertEquals(new BigDecimal("15750.00"), result.totalAmount());
        }

        @Test
        @DisplayName("Pickup equals allowance exactly - no excess")
        void pickupEqualsAllowance() {
            // Allowance for 50 beds × 500g × 30 days = 750 kg
            BigDecimal pickup = new BigDecimal("750.000");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            assertEquals(new BigDecimal("750.000"), result.baseAllowanceKg());
            assertEquals(new BigDecimal("0.000"), result.excessWeightKg());
            assertEquals(new BigDecimal("0.00"), result.excessAmount());
        }

        @Test
        @DisplayName("Pickup slightly above allowance - minimal excess")
        void pickupSlightlyAboveAllowance() {
            // Allowance = 750 kg, pickup = 750.001 kg (1 gram over)
            BigDecimal pickup = new BigDecimal("750.001");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            assertEquals(new BigDecimal("750.000"), result.baseAllowanceKg());
            assertEquals(new BigDecimal("0.001"), result.excessWeightKg());

            // Excess: 0.001 kg × 25 = 0.025 → rounded to 0.03 (HALF_UP)
            assertEquals(new BigDecimal("0.03"), result.excessAmount());
        }

        @Test
        @DisplayName("Pickup below allowance - excess must be ZERO not negative")
        void pickupBelowAllowance() {
            // Allowance = 750 kg, pickup = 500 kg
            BigDecimal pickup = new BigDecimal("500.000");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            // Excess must be 0, not -250
            assertEquals(new BigDecimal("0.000"), result.excessWeightKg());
            assertEquals(new BigDecimal("0.00"), result.excessAmount());
        }

        @Test
        @DisplayName("Large excess weight calculation")
        void largeExcess() {
            // Allowance = 750 kg, pickup = 1500 kg (750 kg excess)
            BigDecimal pickup = new BigDecimal("1500.000");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            assertEquals(new BigDecimal("750.000"), result.excessWeightKg());

            // Excess: 750 kg × 25 = 18,750
            assertEquals(new BigDecimal("18750.00"), result.excessAmount());

            // Subtotal: 15000 + 18750 = 33750
            assertEquals(new BigDecimal("33750.00"), result.subtotal());

            // Total with 5% GST: 33750 × 1.05 = 35437.50
            assertEquals(new BigDecimal("35437.50"), result.totalAmount());
        }
    }

    // ========================================================================
    // MONTH LENGTH VARIATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("Month Length Tests")
    class MonthLengthTests {

        @Test
        @DisplayName("28-day month (February non-leap)")
        void twentyEightDayMonth() {
            BigDecimal pickup = new BigDecimal("700.000");

            var result = service.calculate(BEDS, 28, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            // Allowance: 50 × 500 × 28 / 1000 = 700 kg
            assertEquals(new BigDecimal("700.000"), result.baseAllowanceKg());

            // Base amount: 50 × 10 × 28 = 14000
            assertEquals(new BigDecimal("14000.00"), result.baseAmount());
        }

        @Test
        @DisplayName("29-day month (February leap year)")
        void twentyNineDayMonth() {
            BigDecimal pickup = new BigDecimal("725.000");

            var result = service.calculate(BEDS, 29, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            // Allowance: 50 × 500 × 29 / 1000 = 725 kg
            assertEquals(new BigDecimal("725.000"), result.baseAllowanceKg());

            // Base amount: 50 × 10 × 29 = 14500
            assertEquals(new BigDecimal("14500.00"), result.baseAmount());
        }

        @Test
        @DisplayName("30-day month")
        void thirtyDayMonth() {
            BigDecimal pickup = new BigDecimal("750.000");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            // Allowance: 50 × 500 × 30 / 1000 = 750 kg
            assertEquals(new BigDecimal("750.000"), result.baseAllowanceKg());

            // Base amount: 50 × 10 × 30 = 15000
            assertEquals(new BigDecimal("15000.00"), result.baseAmount());
        }

        @Test
        @DisplayName("31-day month")
        void thirtyOneDayMonth() {
            BigDecimal pickup = new BigDecimal("775.000");

            var result = service.calculate(BEDS, 31, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            // Allowance: 50 × 500 × 31 / 1000 = 775 kg
            assertEquals(new BigDecimal("775.000"), result.baseAllowanceKg());

            // Base amount: 50 × 10 × 31 = 15500
            assertEquals(new BigDecimal("15500.00"), result.baseAmount());
        }

        @ParameterizedTest
        @CsvSource({
                "28, 700.000, 14000.00, 14700.00",
                "29, 725.000, 14500.00, 15225.00",
                "30, 750.000, 15000.00, 15750.00",
                "31, 775.000, 15500.00, 16275.00"
        })
        @DisplayName("Month length affects calculation proportionally")
        void monthLengthVariation(int days, String allowance, String baseAmt, String total) {
            BigDecimal pickup = new BigDecimal(allowance); // pickup = allowance = no excess

            var result = service.calculate(BEDS, days, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            assertEquals(new BigDecimal(allowance), result.baseAllowanceKg());
            assertEquals(new BigDecimal(baseAmt), result.baseAmount());
            assertEquals(new BigDecimal("0.000"), result.excessWeightKg());
            assertEquals(new BigDecimal(total), result.totalAmount());
        }
    }

    // ========================================================================
    // ROUNDING CONSISTENCY TESTS (Paise Handling)
    // ========================================================================

    @Nested
    @DisplayName("Rounding Consistency Tests")
    class RoundingTests {

        @Test
        @DisplayName("Rounding uses HALF_UP consistently")
        void halfUpRounding() {
            // Create a scenario where rounding matters
            // 0.001 kg × 25 = 0.025 → should round to 0.03 (HALF_UP)
            BigDecimal pickup = new BigDecimal("750.001");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            assertEquals(new BigDecimal("0.03"), result.excessAmount());
        }

        @Test
        @DisplayName("GST calculation rounds each component separately")
        void gstRoundsSeparately() {
            // Subtotal that causes rounding in GST
            // Using 33 beds × 10 × 30 = 9900 subtotal
            // CGST: 9900 × 0.025 = 247.50 (exact)
            // SGST: 9900 × 0.025 = 247.50 (exact)
            BigDecimal pickup = BigDecimal.ZERO;

            var result = service.calculate(33, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            // 33 × 500 × 30 / 1000 = 495 kg allowance
            // 33 × 10 × 30 = 9900 base
            assertEquals(new BigDecimal("9900.00"), result.subtotal());
            assertEquals(new BigDecimal("247.50"), result.cgst());
            assertEquals(new BigDecimal("247.50"), result.sgst());
            assertEquals(new BigDecimal("10395.00"), result.totalAmount());
        }

        @Test
        @DisplayName("Fractional paise rounds correctly - edge case")
        void fractionalPaiseRounding() {
            // Create scenario: 0.005 kg excess × 25 = 0.125 → 0.13 or 0.12?
            // With HALF_UP: 0.125 → 0.13
            BigDecimal pickup = new BigDecimal("750.005");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            assertEquals(new BigDecimal("0.005"), result.excessWeightKg());
            // 0.005 × 25 = 0.125 → 0.13 (HALF_UP rounds 5 up)
            assertEquals(new BigDecimal("0.13"), result.excessAmount());
        }

        @Test
        @DisplayName("All monetary values have exactly 2 decimal places")
        void allMoneyHasTwoDecimals() {
            BigDecimal pickup = new BigDecimal("800.000");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            assertEquals(2, result.baseAmount().scale());
            assertEquals(2, result.excessAmount().scale());
            assertEquals(2, result.subtotal().scale());
            assertEquals(2, result.cgst().scale());
            assertEquals(2, result.sgst().scale());
            assertEquals(2, result.totalAmount().scale());
        }

        @Test
        @DisplayName("All weight values have exactly 3 decimal places")
        void allWeightsHaveThreeDecimals() {
            BigDecimal pickup = new BigDecimal("800.000");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            assertEquals(3, result.pickupWeightKg().scale());
            assertEquals(3, result.baseAllowanceKg().scale());
            assertEquals(3, result.excessWeightKg().scale());
        }
    }

    // ========================================================================
    // MATHEMATICAL CORRECTNESS TESTS
    // ========================================================================

    @Nested
    @DisplayName("Mathematical Correctness")
    class MathCorrectness {

        @Test
        @DisplayName("Total = subtotal + CGST + SGST (exactly)")
        void totalEqualsSubtotalPlusGst() {
            BigDecimal pickup = new BigDecimal("900.000");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            BigDecimal expected = result.subtotal()
                    .add(result.cgst())
                    .add(result.sgst());

            assertEquals(expected, result.totalAmount());
        }

        @Test
        @DisplayName("Subtotal = baseAmount + excessAmount")
        void subtotalEqualsBaseAndExcess() {
            BigDecimal pickup = new BigDecimal("900.000");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            BigDecimal expected = result.baseAmount().add(result.excessAmount());

            assertEquals(expected, result.subtotal());
        }

        @Test
        @DisplayName("CGST and SGST are equal (both 2.5%)")
        void cgstEqualsSgst() {
            BigDecimal pickup = new BigDecimal("900.000");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            assertEquals(result.cgst(), result.sgst());
        }

        @Test
        @DisplayName("GST is 5% of subtotal (within rounding)")
        void gstIsFivePercent() {
            BigDecimal pickup = new BigDecimal("800.000");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            // Total GST
            BigDecimal totalGst = result.cgst().add(result.sgst());

            // Expected: subtotal × 0.05
            BigDecimal expected = result.subtotal()
                    .multiply(new BigDecimal("0.05"))
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            // Allow 1 paise tolerance due to separate rounding
            assertTrue(totalGst.subtract(expected).abs().compareTo(new BigDecimal("0.01")) <= 0,
                    "GST should be 5% of subtotal within 1 paise");
        }
    }

    // ========================================================================
    // EDGE CASE TESTS
    // ========================================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Single bed facility")
        void singleBed() {
            var result = service.calculate(1, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, BigDecimal.ZERO);

            // 1 × 500 × 30 / 1000 = 15 kg
            assertEquals(new BigDecimal("15.000"), result.baseAllowanceKg());

            // 1 × 10 × 30 = 300
            assertEquals(new BigDecimal("300.00"), result.baseAmount());
        }

        @Test
        @DisplayName("Very large hospital (1000 beds)")
        void largeHospital() {
            var result = service.calculate(1000, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, BigDecimal.ZERO);

            // 1000 × 500 × 30 / 1000 = 15000 kg
            assertEquals(new BigDecimal("15000.000"), result.baseAllowanceKg());

            // 1000 × 10 × 30 = 300000
            assertEquals(new BigDecimal("300000.00"), result.baseAmount());
        }

        @Test
        @DisplayName("High precision pickup weight preserved")
        void highPrecisionWeight() {
            BigDecimal pickup = new BigDecimal("750.123");

            var result = service.calculate(BEDS, 30, BASE_GRAMS, BASE_RATE, EXCESS_RATE, pickup);

            assertEquals(new BigDecimal("750.123"), result.pickupWeightKg());
            assertEquals(new BigDecimal("0.123"), result.excessWeightKg());
        }
    }
}
