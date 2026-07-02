package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.BankAccount;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.PaymentReceipt;
import com.smartcbwtf.domain.PaymentMode;
import com.smartcbwtf.repository.BankAccountRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.InvoicePaymentRepository;
import com.smartcbwtf.repository.PaymentRepository;
import com.smartcbwtf.repository.PaymentReversalRepository;
import com.smartcbwtf.service.PaymentService;
import com.smartcbwtf.service.ReceiptService;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.data.domain.PageImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialControllerValidationTest {

    @Mock
    private PaymentService paymentService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private InvoicePaymentRepository invoicePaymentRepository;
    @Mock
    private PaymentReversalRepository reversalRepository;
    @Mock
    private ReceiptService receiptService;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private FacilityRepository facilityRepository;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void financialMutationEndpointsValidateRequestBodies() throws NoSuchMethodException {
        assertValidatedRequestBody(BankAccountController.class, "createAccount",
                BankAccountController.CreateBankAccountRequest.class, 0);
        assertValidatedRequestBody(BankAccountController.class, "updateAccount",
                BankAccountController.UpdateBankAccountRequest.class, 1, UUID.class);
        assertValidatedRequestBody(PaymentController.class, "recordPayment",
                PaymentController.RecordPaymentRequest.class, 0);
        assertValidatedRequestBody(PaymentController.class, "reversePayment",
                PaymentController.ReversePaymentRequest.class, 1, UUID.class);
    }

    @Test
    void bankAccountRequestsRequireValidFinancialIdentifiers() {
        assertFieldViolation(new BankAccountController.CreateBankAccountRequest(
                "", "123456789", "HDFC0ABC123", "HDFC Bank", ""), "accountName");
        assertFieldViolation(new BankAccountController.CreateBankAccountRequest(
                "Main", "abc", "HDFC0ABC123", "HDFC Bank", ""), "accountNumber");
        assertFieldViolation(new BankAccountController.CreateBankAccountRequest(
                "Main", "123456789", "BADIFSC", "HDFC Bank", ""), "ifscCode");
        assertFieldViolation(new BankAccountController.CreateBankAccountRequest(
                "Main", "123456789", "HDFC0ABC123", "x".repeat(101), ""), "bankName");
        assertFieldViolation(new BankAccountController.CreateBankAccountRequest(
                "Main", "123456789", "HDFC0ABC123", "HDFC Bank", "not-upi"), "upiId");
        assertTrue(validator.validate(new BankAccountController.CreateBankAccountRequest(
                "Main", "123456789", "hdfc0abc123", "HDFC Bank", "smart.care@upi")).isEmpty());
    }

    @Test
    void paymentRequestsBoundDatesAmountsAndFreeText() {
        UUID hcfId = UUID.randomUUID();
        assertFieldViolation(validPayment(hcfId, LocalDate.now().plusDays(1), new BigDecimal("100.00")),
                "paymentDate");
        assertFieldViolation(validPayment(hcfId, LocalDate.now(), BigDecimal.ZERO), "amount");
        assertFieldViolation(validPayment(hcfId, LocalDate.now(), new BigDecimal("1.001")), "amount");
        assertFieldViolation(new PaymentController.RecordPaymentRequest(
                hcfId, null, LocalDate.now(), new BigDecimal("100.00"), PaymentMode.UPI,
                "x".repeat(101), null, null), "referenceNumber");
        assertFieldViolation(new PaymentController.RecordPaymentRequest(
                hcfId, null, LocalDate.now(), new BigDecimal("100.00"), PaymentMode.UPI,
                null, "x".repeat(256), null), "payerName");
        assertFieldViolation(new PaymentController.RecordPaymentRequest(
                hcfId, null, LocalDate.now(), new BigDecimal("100.00"), PaymentMode.UPI,
                null, null, "x".repeat(2001)), "notes");
        assertFieldViolation(new PaymentController.ReversePaymentRequest(""), "reason");
        assertFieldViolation(new PaymentController.ReversePaymentRequest("x".repeat(1001)), "reason");
        assertTrue(validator.validate(validPayment(hcfId, LocalDate.now(), new BigDecimal("100.00"))).isEmpty());
    }

    @Test
    void bankAccountCreateNormalizesIfscAndBlankOptionalUpi() {
        UUID facilityId = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(facilityId);
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(bankAccountRepository.countByFacilityIdAndStatus(facilityId, BankAccount.Status.ACTIVE)).thenReturn(0L);
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BankAccountController controller = new BankAccountController(bankAccountRepository, facilityRepository);
        ArgumentCaptor<BankAccount> accountCaptor = ArgumentCaptor.forClass(BankAccount.class);

        controller.createAccount(new BankAccountController.CreateBankAccountRequest(
                " Main Account ", "123456789", "hdfc0abc123", " HDFC Bank ", " "));

        verify(bankAccountRepository).save(accountCaptor.capture());
        BankAccount saved = accountCaptor.getValue();
        assertEquals("Main Account", saved.getAccountName());
        assertEquals("HDFC0ABC123", saved.getIfscCode());
        assertEquals("HDFC Bank", saved.getBankName());
        assertNull(saved.getUpiId());
    }

    @Test
    void bankAccountDetailUsesTenantScopedLookup() {
        UUID facilityId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(bankAccountRepository.findByIdAndFacilityId(accountId, facilityId)).thenReturn(Optional.empty());
        BankAccountController controller = new BankAccountController(bankAccountRepository, facilityRepository);

        var response = controller.getAccount(accountId);

        assertEquals(404, response.getStatusCode().value());
        verify(bankAccountRepository).findByIdAndFacilityId(accountId, facilityId);
        verify(bankAccountRepository, never()).findById(accountId);
    }

    @Test
    void bankAccountUpdateUsesTenantScopedLookupBeforeSave() {
        UUID facilityId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(bankAccountRepository.findByIdAndFacilityId(accountId, facilityId)).thenReturn(Optional.empty());
        BankAccountController controller = new BankAccountController(bankAccountRepository, facilityRepository);

        var response = controller.updateAccount(accountId, new BankAccountController.UpdateBankAccountRequest(
                "Main", "123456789", "HDFC0ABC123", "HDFC Bank", ""));

        assertEquals(404, response.getStatusCode().value());
        verify(bankAccountRepository).findByIdAndFacilityId(accountId, facilityId);
        verify(bankAccountRepository, never()).findById(accountId);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    void bankAccountSetPrimaryUsesTenantScopedLookupBeforeClearingFacility() {
        UUID facilityId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(bankAccountRepository.findByIdAndFacilityId(accountId, facilityId)).thenReturn(Optional.empty());
        BankAccountController controller = new BankAccountController(bankAccountRepository, facilityRepository);

        var response = controller.setPrimary(accountId);

        assertEquals(404, response.getStatusCode().value());
        verify(bankAccountRepository).findByIdAndFacilityId(accountId, facilityId);
        verify(bankAccountRepository, never()).findById(accountId);
        verify(bankAccountRepository, never()).clearPrimaryForFacility(facilityId);
    }

    @Test
    void paymentRecordNormalizesOptionalFreeTextBeforeServiceCall() {
        UUID facilityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, facilityId, null, "CBWTF_ADMIN", "admin"));
        when(paymentService.recordPayment(any(PaymentService.RecordPaymentRequest.class)))
                .thenReturn(PaymentService.PaymentResult.success(
                        UUID.randomUUID(),
                        "RCT-1",
                        new PaymentService.AllocationResult(BigDecimal.ZERO, BigDecimal.ZERO, List.of())));
        PaymentController controller = new PaymentController(
                paymentService, paymentRepository, invoicePaymentRepository, reversalRepository, receiptService);
        ArgumentCaptor<PaymentService.RecordPaymentRequest> requestCaptor =
                ArgumentCaptor.forClass(PaymentService.RecordPaymentRequest.class);

        controller.recordPayment(new PaymentController.RecordPaymentRequest(
                hcfId, null, LocalDate.now(), new BigDecimal("100.00"), PaymentMode.UPI,
                " REF-1 ", " Dr Rao ", " "));

        verify(paymentService).recordPayment(requestCaptor.capture());
        PaymentService.RecordPaymentRequest request = requestCaptor.getValue();
        assertEquals("REF-1", request.referenceNumber());
        assertEquals("Dr Rao", request.payerName());
        assertNull(request.notes());
    }

    @Test
    void paymentReverseUsesTenantScopedLookupBeforeServiceCall() {
        UUID facilityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, facilityId, null, "CBWTF_ADMIN", "admin"));
        when(paymentRepository.findByIdAndFacilityId(paymentId, facilityId)).thenReturn(Optional.empty());
        PaymentController controller = new PaymentController(
                paymentService, paymentRepository, invoicePaymentRepository, reversalRepository, receiptService);

        var response = controller.reversePayment(paymentId, new PaymentController.ReversePaymentRequest("Duplicate"));

        assertEquals(404, response.getStatusCode().value());
        verify(paymentRepository).findByIdAndFacilityId(paymentId, facilityId);
        verify(paymentRepository, never()).findById(paymentId);
        verify(paymentService, never()).reversePayment(any(UUID.class), any(UUID.class), any(String.class),
                any(UUID.class));
    }

    @Test
    void paymentListBatchesReversalFlagsForVisiblePage() {
        UUID facilityId = UUID.randomUUID();
        UUID firstPaymentId = UUID.randomUUID();
        UUID secondPaymentId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(paymentService.getPayments(any(UUID.class), any()))
                .thenReturn(new PageImpl<>(List.of(
                        payment(firstPaymentId, UUID.randomUUID(), "City Hospital"),
                        payment(secondPaymentId, UUID.randomUUID(), "Metro Clinic"))));
        when(reversalRepository.findOriginalPaymentIdsIn(List.of(firstPaymentId, secondPaymentId)))
                .thenReturn(List.of(firstPaymentId));
        when(reversalRepository.findReversalPaymentIdsIn(List.of(firstPaymentId, secondPaymentId)))
                .thenReturn(List.of(secondPaymentId));
        PaymentController controller = new PaymentController(
                paymentService, paymentRepository, invoicePaymentRepository, reversalRepository, receiptService);

        var response = controller.listPayments(0, 20, null);

        assertEquals(200, response.getStatusCode().value());
        var payments = response.getBody().getContent();
        assertTrue(payments.get(0).isReversed());
        assertTrue(payments.get(1).isReversalEntry());
        verify(reversalRepository).findOriginalPaymentIdsIn(List.of(firstPaymentId, secondPaymentId));
        verify(reversalRepository).findReversalPaymentIdsIn(List.of(firstPaymentId, secondPaymentId));
        verify(reversalRepository, never()).existsByOriginalPaymentId(any(UUID.class));
        verify(reversalRepository, never()).existsByReversalPaymentId(any(UUID.class));
    }

    @Test
    void paymentDetailUsesTenantScopedLookup() {
        UUID facilityId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(paymentRepository.findByIdAndFacilityId(paymentId, facilityId)).thenReturn(Optional.empty());
        PaymentController controller = new PaymentController(
                paymentService, paymentRepository, invoicePaymentRepository, reversalRepository, receiptService);

        var response = controller.getPayment(paymentId);

        assertEquals(404, response.getStatusCode().value());
        verify(paymentRepository).findByIdAndFacilityId(paymentId, facilityId);
        verify(paymentRepository, never()).findById(paymentId);
    }

    @Test
    void receiptDownloadUsesTenantScopedLookupBeforeReceiptService() {
        UUID facilityId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(paymentRepository.findByIdAndFacilityId(paymentId, facilityId)).thenReturn(Optional.empty());
        PaymentController controller = new PaymentController(
                paymentService, paymentRepository, invoicePaymentRepository, reversalRepository, receiptService);

        var response = controller.downloadReceipt(paymentId);

        assertEquals(404, response.getStatusCode().value());
        verify(paymentRepository).findByIdAndFacilityId(paymentId, facilityId);
        verify(paymentRepository, never()).findById(paymentId);
        verify(receiptService, never()).getReceipt(paymentId);
    }

    @Test
    void receiptDownloadUsesNoStoreCacheHeader() {
        UUID facilityId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        byte[] pdf = new byte[] { 1, 2, 3 };
        PaymentReceipt receipt = new PaymentReceipt();
        receipt.setReceiptNumber("RCT/2026/0001");
        receipt.setPdfBytes(pdf);
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(paymentRepository.findByIdAndFacilityId(paymentId, facilityId))
                .thenReturn(Optional.of(payment(paymentId, UUID.randomUUID(), "City Hospital")));
        when(receiptService.getReceipt(paymentId)).thenReturn(receipt);
        PaymentController controller = new PaymentController(
                paymentService, paymentRepository, invoicePaymentRepository, reversalRepository, receiptService);

        var response = controller.downloadReceipt(paymentId);

        assertEquals(200, response.getStatusCode().value());
        assertArrayEquals(pdf, (byte[]) response.getBody());
        assertEquals("no-store", response.getHeaders().getCacheControl());
    }

    private PaymentController.RecordPaymentRequest validPayment(UUID hcfId, LocalDate paymentDate, BigDecimal amount) {
        return new PaymentController.RecordPaymentRequest(
                hcfId, null, paymentDate, amount, PaymentMode.UPI, "REF-1", "Payer", "ok");
    }

    private com.smartcbwtf.domain.Payment payment(UUID paymentId, UUID hcfId, String hcfName) {
        var hcf = new com.smartcbwtf.domain.Hcf();
        hcf.setId(hcfId);
        hcf.setName(hcfName);
        var payment = new com.smartcbwtf.domain.Payment();
        payment.setId(paymentId);
        payment.setHcf(hcf);
        payment.setPaymentDate(LocalDate.now());
        payment.setAmount(new BigDecimal("100.00"));
        payment.setMode(PaymentMode.UPI);
        payment.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return payment;
    }

    private void assertValidatedRequestBody(Class<?> controllerClass, String methodName, Class<?> requestType,
            int parameterIndex, Class<?>... leadingParameterTypes) throws NoSuchMethodException {
        Class<?>[] parameterTypes = new Class<?>[leadingParameterTypes.length + 1];
        System.arraycopy(leadingParameterTypes, 0, parameterTypes, 0, leadingParameterTypes.length);
        parameterTypes[parameterTypes.length - 1] = requestType;
        Method method = controllerClass.getDeclaredMethod(methodName, parameterTypes);
        Parameter parameter = method.getParameters()[parameterIndex];

        assertTrue(parameter.isAnnotationPresent(Valid.class), methodName + " request must be validated");
        assertTrue(parameter.isAnnotationPresent(RequestBody.class), methodName + " request must remain a body");
    }

    private void assertFieldViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field);
    }
}
