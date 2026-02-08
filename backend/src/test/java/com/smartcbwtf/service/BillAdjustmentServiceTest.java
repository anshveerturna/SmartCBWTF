package com.smartcbwtf.service;

import com.smartcbwtf.domain.Bill;
import com.smartcbwtf.domain.BillVersion;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.repository.BillRepository;
import com.smartcbwtf.repository.BillVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BillAdjustmentService.
 * 
 * Tests the core adjustment logic:
 * - Only FINALIZED bills can be adjusted
 * - Adjustment must be negative (concession only)
 * - Adjustment cannot exceed bill total
 * - Creates BillVersion audit record
 * - Updates bill status to FINALIZED_WITH_ADJUSTMENT
 */
@ExtendWith(MockitoExtension.class)
class BillAdjustmentServiceTest {

    @Mock
    private BillRepository billRepository;

    @Mock
    private BillVersionRepository billVersionRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BillAdjustmentService billAdjustmentService;

    private UUID billId;
    private UUID userId;
    private Bill testBill;

    @BeforeEach
    void setUp() {
        billId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testBill = createTestBill(billId, Bill.Status.FINALIZED.name(), new BigDecimal("10000.00"));
    }

    private Bill createTestBill(UUID id, String status, BigDecimal totalAmount) {
        Bill bill = new Bill();
        // Use reflection or setters to set the ID since Bill constructor generates
        // random UUID
        try {
            var idField = Bill.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(bill, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        bill.setStatus(status);
        bill.setTotalAmount(totalAmount);
        bill.setFinalPayableAmount(totalAmount);
        bill.setBillVersion(1);

        // Mock facility
        Facility facility = mock(Facility.class);
        lenient().when(facility.getId()).thenReturn(UUID.randomUUID());
        lenient().when(facility.getContactEmail()).thenReturn("test@facility.com");
        bill.setFacility(facility);

        // Mock agreement
        Agreement agreement = mock(Agreement.class);
        lenient().when(agreement.getHcf()).thenReturn(null);
        bill.setAgreement(agreement);

        return bill;
    }

    @Nested
    @DisplayName("Successful Adjustment Tests")
    class SuccessfulAdjustmentTests {

        @Test
        @DisplayName("Should apply valid concession to finalized bill")
        void shouldApplyValidConcession() {
            // Given
            BigDecimal adjustmentAmount = new BigDecimal("-500.00");
            String reason = "Customer loyalty discount";

            when(billRepository.findById(billId)).thenReturn(Optional.of(testBill));
            when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));
            when(billVersionRepository.save(any(BillVersion.class))).thenAnswer(i -> i.getArgument(0));

            // When
            Bill result = billAdjustmentService.applyAdjustment(billId, adjustmentAmount, reason, userId);

            // Then
            assertThat(result.getStatus()).isEqualTo(Bill.Status.FINALIZED_WITH_ADJUSTMENT.name());
            assertThat(result.getAdjustmentAmount()).isEqualTo(adjustmentAmount);
            assertThat(result.getAdjustmentReason()).isEqualTo(reason);
            assertThat(result.getAdjustedBy()).isEqualTo(userId);
            assertThat(result.getAdjustedAt()).isNotNull();
            assertThat(result.getBillVersion()).isEqualTo(2);

            // Final amount should be totalAmount + adjustmentAmount (negative)
            BigDecimal expectedFinal = new BigDecimal("9500.00");
            assertThat(result.getFinalPayableAmount()).isEqualByComparingTo(expectedFinal);
        }

        @Test
        @DisplayName("Should create BillVersion audit record")
        void shouldCreateBillVersionAuditRecord() {
            // Given
            BigDecimal adjustmentAmount = new BigDecimal("-1000.00");
            String reason = "Billing error correction";

            when(billRepository.findById(billId)).thenReturn(Optional.of(testBill));
            when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));
            when(billVersionRepository.save(any(BillVersion.class))).thenAnswer(i -> i.getArgument(0));

            // When
            billAdjustmentService.applyAdjustment(billId, adjustmentAmount, reason, userId);

            // Then
            ArgumentCaptor<BillVersion> versionCaptor = ArgumentCaptor.forClass(BillVersion.class);
            verify(billVersionRepository).save(versionCaptor.capture());

            BillVersion savedVersion = versionCaptor.getValue();
            assertThat(savedVersion.getBill().getId()).isEqualTo(billId);
            assertThat(savedVersion.getAdjustmentAmount()).isEqualTo(adjustmentAmount);
            assertThat(savedVersion.getAdjustmentReason()).isEqualTo(reason);
            assertThat(savedVersion.getAdjustedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should log audit entry for adjustment")
        void shouldLogAuditEntry() {
            // Given
            BigDecimal adjustmentAmount = new BigDecimal("-200.00");
            String reason = "Goodwill gesture";

            when(billRepository.findById(billId)).thenReturn(Optional.of(testBill));
            when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));
            when(billVersionRepository.save(any(BillVersion.class))).thenAnswer(i -> i.getArgument(0));

            // When
            billAdjustmentService.applyAdjustment(billId, adjustmentAmount, reason, userId);

            // Then
            verify(auditLogService).log(
                    eq("BILL_ADJUSTMENT"),
                    eq(billId),
                    eq("BILL_ADJUSTED"),
                    eq(userId),
                    contains("adjustment"));
        }
    }

    @Nested
    @DisplayName("Validation Failure Tests")
    class ValidationFailureTests {

        @Test
        @DisplayName("Should reject adjustment on DRAFT bill")
        void shouldRejectAdjustmentOnDraftBill() {
            // Given
            testBill.setStatus(Bill.Status.DRAFT.name());
            when(billRepository.findById(billId)).thenReturn(Optional.of(testBill));

            // When/Then
            assertThatThrownBy(
                    () -> billAdjustmentService.applyAdjustment(billId, new BigDecimal("-100"), "test", userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only FINALIZED bills can be adjusted");
        }

        @Test
        @DisplayName("Should reject adjustment on already adjusted bill")
        void shouldRejectAdjustmentOnAlreadyAdjustedBill() {
            // Given
            testBill.setStatus(Bill.Status.FINALIZED_WITH_ADJUSTMENT.name());
            testBill.setAdjustmentAmount(new BigDecimal("-500"));
            when(billRepository.findById(billId)).thenReturn(Optional.of(testBill));

            // When/Then
            assertThatThrownBy(
                    () -> billAdjustmentService.applyAdjustment(billId, new BigDecimal("-100"), "test", userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already been adjusted");
        }

        @Test
        @DisplayName("Should reject positive adjustment amount")
        void shouldRejectPositiveAdjustmentAmount() {
            // Given
            when(billRepository.findById(billId)).thenReturn(Optional.of(testBill));

            // When/Then
            assertThatThrownBy(
                    () -> billAdjustmentService.applyAdjustment(billId, new BigDecimal("100"), "test", userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be negative");
        }

        @Test
        @DisplayName("Should reject zero adjustment amount")
        void shouldRejectZeroAdjustmentAmount() {
            // Given
            when(billRepository.findById(billId)).thenReturn(Optional.of(testBill));

            // When/Then
            assertThatThrownBy(() -> billAdjustmentService.applyAdjustment(billId, BigDecimal.ZERO, "test", userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be negative");
        }

        @Test
        @DisplayName("Should reject adjustment exceeding bill total")
        void shouldRejectAdjustmentExceedingBillTotal() {
            // Given
            when(billRepository.findById(billId)).thenReturn(Optional.of(testBill));
            BigDecimal excessiveAdjustment = new BigDecimal("-15000.00"); // Bill total is 10000

            // When/Then
            assertThatThrownBy(() -> billAdjustmentService.applyAdjustment(billId, excessiveAdjustment, "test", userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot exceed bill total");
        }

        @Test
        @DisplayName("Should reject empty reason")
        void shouldRejectEmptyReason() {
            // Given
            when(billRepository.findById(billId)).thenReturn(Optional.of(testBill));

            // When/Then
            assertThatThrownBy(() -> billAdjustmentService.applyAdjustment(billId, new BigDecimal("-100"), "", userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is mandatory");
        }

        @Test
        @DisplayName("Should reject null reason")
        void shouldRejectNullReason() {
            // Given
            when(billRepository.findById(billId)).thenReturn(Optional.of(testBill));

            // When/Then
            assertThatThrownBy(
                    () -> billAdjustmentService.applyAdjustment(billId, new BigDecimal("-100"), null, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is mandatory");
        }

        @Test
        @DisplayName("Should throw when bill not found")
        void shouldThrowWhenBillNotFound() {
            // Given
            when(billRepository.findById(billId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(
                    () -> billAdjustmentService.applyAdjustment(billId, new BigDecimal("-100"), "test", userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bill not found");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle adjustment equal to bill total (100% discount)")
        void shouldHandleFullDiscountAdjustment() {
            // Given
            BigDecimal fullDiscount = new BigDecimal("-10000.00"); // Equals bill total

            when(billRepository.findById(billId)).thenReturn(Optional.of(testBill));
            when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));
            when(billVersionRepository.save(any(BillVersion.class))).thenAnswer(i -> i.getArgument(0));

            // When
            Bill result = billAdjustmentService.applyAdjustment(billId, fullDiscount, "Full waiver", userId);

            // Then
            assertThat(result.getFinalPayableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getStatus()).isEqualTo(Bill.Status.FINALIZED_WITH_ADJUSTMENT.name());
        }

        @Test
        @DisplayName("Should handle small adjustment accurately")
        void shouldHandleSmallAdjustmentAccurately() {
            // Given
            BigDecimal smallAdjustment = new BigDecimal("-0.01");

            when(billRepository.findById(billId)).thenReturn(Optional.of(testBill));
            when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));
            when(billVersionRepository.save(any(BillVersion.class))).thenAnswer(i -> i.getArgument(0));

            // When
            Bill result = billAdjustmentService.applyAdjustment(billId, smallAdjustment, "Rounding", userId);

            // Then
            BigDecimal expectedFinal = new BigDecimal("9999.99");
            assertThat(result.getFinalPayableAmount()).isEqualByComparingTo(expectedFinal);
        }
    }
}
