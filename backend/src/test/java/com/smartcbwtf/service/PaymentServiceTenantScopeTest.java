package com.smartcbwtf.service;

import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.Payment;
import com.smartcbwtf.domain.PaymentMode;
import com.smartcbwtf.domain.PaymentReversal;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.AlertRepository;
import com.smartcbwtf.repository.BankAccountRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfAdvanceLedgerRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.InvoicePaymentRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import com.smartcbwtf.repository.PaymentRepository;
import com.smartcbwtf.repository.PaymentReversalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTenantScopeTest {

    @Mock
    PaymentRepository paymentRepository;
    @Mock
    PaymentReversalRepository reversalRepository;
    @Mock
    InvoicePaymentRepository invoicePaymentRepository;
    @Mock
    HcfAdvanceLedgerRepository advanceLedgerRepository;
    @Mock
    InvoiceRepository invoiceRepository;
    @Mock
    HcfRepository hcfRepository;
    @Mock
    FacilityRepository facilityRepository;
    @Mock
    AgreementRepository agreementRepository;
    @Mock
    BankAccountRepository bankAccountRepository;
    @Mock
    AlertRepository alertRepository;
    @Mock
    ReceiptService receiptService;
    @Mock
    EmailService emailService;

    PaymentService service;

    @BeforeEach
    void setUp() {
        AlertService alertService = new AlertService(alertRepository, facilityRepository);
        service = new PaymentService(
                paymentRepository,
                reversalRepository,
                invoicePaymentRepository,
                advanceLedgerRepository,
                invoiceRepository,
                hcfRepository,
                facilityRepository,
                agreementRepository,
                bankAccountRepository,
                alertService,
                receiptService,
                emailService);
    }

    @Test
    void listPaymentsForHcfIsFacilityScoped() {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);

        service.getPaymentsForHcf(facilityId, hcfId, pageable);

        verify(paymentRepository).findByFacilityIdAndHcfId(facilityId, hcfId, pageable);
    }

    @Test
    void advanceBalanceIsFacilityScoped() {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        when(advanceLedgerRepository.getAdvanceBalanceForFacility(facilityId, hcfId))
                .thenReturn(new BigDecimal("125.50"));

        BigDecimal balance = service.getAdvanceBalance(facilityId, hcfId);

        assertEquals(new BigDecimal("125.50"), balance);
        verify(advanceLedgerRepository).getAdvanceBalanceForFacility(facilityId, hcfId);
    }

    @Test
    void totalAdvanceBalanceIsFacilityScoped() {
        UUID facilityId = UUID.randomUUID();
        when(advanceLedgerRepository.getTotalAdvanceBalanceForFacility(facilityId))
                .thenReturn(new BigDecimal("450.75"));

        BigDecimal balance = service.getTotalAdvanceBalance(facilityId);

        assertEquals(new BigDecimal("450.75"), balance);
        verify(advanceLedgerRepository).getTotalAdvanceBalanceForFacility(facilityId);
    }

    @Test
    void outstandingUsesInvoiceTotalsMinusAllocations() {
        UUID facilityId = UUID.randomUUID();
        when(invoiceRepository.sumTotalAmountByFacilityId(facilityId)).thenReturn(new BigDecimal("1000.00"));
        when(invoicePaymentRepository.getTotalAllocatedForFacility(facilityId)).thenReturn(new BigDecimal("250.25"));

        BigDecimal outstanding = service.getTotalOutstanding(facilityId);

        assertEquals(new BigDecimal("749.75"), outstanding);
        verify(invoiceRepository).sumTotalAmountByFacilityId(facilityId);
        verify(invoicePaymentRepository).getTotalAllocatedForFacility(facilityId);
    }

    @Test
    void outstandingDoesNotGoNegativeWhenAllocationsExceedInvoices() {
        UUID facilityId = UUID.randomUUID();
        when(invoiceRepository.sumTotalAmountByFacilityId(facilityId)).thenReturn(new BigDecimal("100.00"));
        when(invoicePaymentRepository.getTotalAllocatedForFacility(facilityId)).thenReturn(new BigDecimal("150.00"));

        BigDecimal outstanding = service.getTotalOutstanding(facilityId);

        assertEquals(BigDecimal.ZERO, outstanding);
    }

    @Test
    void recordPaymentRejectsHcfOutsideTenantFacilityBeforeSaving() {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(facilityId);
        Hcf hcf = new Hcf();
        hcf.setId(hcfId);

        when(hcfRepository.findById(hcfId)).thenReturn(Optional.of(hcf));
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(agreementRepository.findActiveByHcfAndFacility(hcfId, facilityId)).thenReturn(Optional.empty());

        PaymentService.PaymentResult result = service.recordPayment(new PaymentService.RecordPaymentRequest(
                facilityId,
                hcfId,
                null,
                LocalDate.now(),
                new BigDecimal("100.00"),
                PaymentMode.UPI,
                null,
                "Payer",
                null,
                UUID.randomUUID()));

        assertFalse(result.success());
        assertEquals("HCF is not active under this facility", result.error());
        verify(paymentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reversePaymentPersistsNegativePaymentCounterEntry() {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setName("Facility");
        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        hcf.setName("Hospital");
        Payment original = new Payment();
        original.setId(paymentId);
        original.setFacility(facility);
        original.setHcf(hcf);
        original.setAmount(new BigDecimal("100.00"));
        original.setPaymentDate(LocalDate.now());
        original.setMode(PaymentMode.UPI);
        original.setChecksum("original-checksum");
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        when(paymentRepository.findByIdAndFacilityId(paymentId, facilityId)).thenReturn(Optional.of(original));
        when(reversalRepository.existsByOriginalPaymentId(paymentId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(UUID.randomUUID());
            return payment;
        });
        when(reversalRepository.save(any(PaymentReversal.class))).thenAnswer(invocation -> {
            PaymentReversal reversal = invocation.getArgument(0);
            reversal.setId(UUID.randomUUID());
            return reversal;
        });
        when(invoicePaymentRepository.findByPaymentId(paymentId)).thenReturn(java.util.List.of());
        when(advanceLedgerRepository.sumByPaymentId(paymentId)).thenReturn(BigDecimal.ZERO);
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));

        service.reversePayment(facilityId, paymentId, "Duplicate receipt", UUID.randomUUID());

        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(new BigDecimal("-100.00"), paymentCaptor.getValue().getAmount());
        verify(alertRepository).existsByEventIdAndType(any(UUID.class),
                eq(com.smartcbwtf.domain.AlertType.PAYMENT_RECEIVED));
    }

    @Test
    void reversePaymentRejectsReversalCounterEntry() {
        UUID facilityId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Payment counterEntry = new Payment();
        counterEntry.setId(paymentId);
        counterEntry.setAmount(new BigDecimal("-100.00"));

        when(paymentRepository.findByIdAndFacilityId(paymentId, facilityId)).thenReturn(Optional.of(counterEntry));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> service.reversePayment(facilityId, paymentId, "Undo reversal", UUID.randomUUID()));

        assertEquals("Reversal counter-entries cannot be reversed", thrown.getMessage());
        verify(reversalRepository, never()).existsByOriginalPaymentId(paymentId);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void reversePaymentIsFacilityScoped() {
        UUID facilityId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        when(paymentRepository.findByIdAndFacilityId(paymentId, facilityId)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.reversePayment(facilityId, paymentId, "Wrong tenant", UUID.randomUUID()));

        assertEquals("Payment not found: " + paymentId, thrown.getMessage());
        verify(paymentRepository).findByIdAndFacilityId(paymentId, facilityId);
        verify(paymentRepository, never()).findById(paymentId);
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
