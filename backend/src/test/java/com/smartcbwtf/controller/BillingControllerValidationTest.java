package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Bill;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.Invoice;
import com.smartcbwtf.repository.BillRepository;
import com.smartcbwtf.repository.BillVersionRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import com.smartcbwtf.service.AuditLogService;
import com.smartcbwtf.service.BillAdjustmentService;
import com.smartcbwtf.service.BillGenerationService;
import com.smartcbwtf.service.BillPdfService;
import com.smartcbwtf.service.InvoicePdfService;
import com.smartcbwtf.service.TallyExportService;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingControllerValidationTest {

    @Mock
    private BillRepository billRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private BillGenerationService billGenerationService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private InvoicePdfService invoicePdfService;
    @Mock
    private BillAdjustmentService billAdjustmentService;
    @Mock
    private BillPdfService billPdfService;
    @Mock
    private TallyExportService tallyExportService;
    @Mock
    private BillVersionRepository billVersionRepository;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private BillingController controller;

    @BeforeEach
    void setUp() {
        controller = new BillingController(billRepository, invoiceRepository, facilityRepository,
                billGenerationService, auditLogService, invoicePdfService, billAdjustmentService, billPdfService,
                tallyExportService, billVersionRepository);
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), UUID.randomUUID(), null,
                "CBWTF_ADMIN", "admin"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void billingMutationEndpointsValidateRequestBodies() throws NoSuchMethodException {
        assertValidatedBody("triggerBillGeneration", BillingController.GenerateBillsRequest.class);
        assertValidatedBody("updateExcessRate", BillingController.UpdateExcessRateRequest.class);
    }

    @Test
    void billingRequestsConstrainRequiredFinancialFields() {
        assertFieldViolation(new BillingController.GenerateBillsRequest(null), "billingMonth");
        assertFieldViolation(new BillingController.UpdateExcessRateRequest(null, LocalDate.now()), "ratePerKg");
        assertFieldViolation(new BillingController.UpdateExcessRateRequest(BigDecimal.ZERO, LocalDate.now()),
                "ratePerKg");
        assertFieldViolation(new BillingController.UpdateExcessRateRequest(new BigDecimal("1.001"), LocalDate.now()),
                "ratePerKg");
        assertFieldViolation(new BillingController.UpdateExcessRateRequest(new BigDecimal("1.00"), null),
                "effectiveFrom");
    }

    @Test
    void triggerBillGenerationRejectsNullBodyBeforeServiceCall() {
        var response = controller.triggerBillGeneration(null);

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(billGenerationService);
    }

    @Test
    void updateExcessRateRejectsMissingEffectiveDateBeforeRepositoryAccess() {
        var response = controller.updateExcessRate(
                new BillingController.UpdateExcessRateRequest(new BigDecimal("1.00"), null));

        assertEquals(400, response.getStatusCode().value());
        verify(facilityRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getBillUsesTenantScopedLookup() {
        UUID billId = UUID.randomUUID();
        UUID facilityId = TenantContext.getTenantId();
        when(billRepository.findByIdAndFacilityId(billId, facilityId)).thenReturn(Optional.empty());

        var response = controller.getBill(billId);

        assertEquals(404, response.getStatusCode().value());
        verify(billRepository).findByIdAndFacilityId(billId, facilityId);
        verify(billRepository, never()).findById(billId);
    }

    @Test
    void getInvoiceUsesTenantScopedBillLookup() {
        UUID billId = UUID.randomUUID();
        UUID facilityId = TenantContext.getTenantId();
        when(invoiceRepository.findByBillIdAndFacilityId(billId, facilityId)).thenReturn(Optional.empty());

        var response = controller.getInvoice(billId);

        assertEquals(404, response.getStatusCode().value());
        verify(invoiceRepository).findByBillIdAndFacilityId(billId, facilityId);
        verify(invoiceRepository, never()).findByBillId(billId);
    }

    @Test
    void downloadInvoiceByIdUsesTenantScopedLookupBeforePdfGeneration() {
        UUID invoiceId = UUID.randomUUID();
        UUID facilityId = TenantContext.getTenantId();
        when(invoiceRepository.findByIdAndFacilityId(invoiceId, facilityId)).thenReturn(Optional.empty());

        var response = controller.downloadInvoiceById(invoiceId);

        assertEquals(404, response.getStatusCode().value());
        verify(invoiceRepository).findByIdAndFacilityId(invoiceId, facilityId);
        verify(invoiceRepository, never()).findById(invoiceId);
        verifyNoInteractions(invoicePdfService);
    }

    @Test
    void tallyExportRejectsInvalidPeriodBeforeServiceCall() {
        var badMonth = controller.exportForTally(2026, 13);
        var badYear = controller.exportForTally(1999, 6);

        assertEquals(400, badMonth.getStatusCode().value());
        assertEquals(400, badYear.getStatusCode().value());
        verifyNoInteractions(tallyExportService);
    }

    @Test
    void financeFileDownloadsUseNoStoreCacheHeader() throws Exception {
        UUID facilityId = TenantContext.getTenantId();
        Bill bill = bill(facilityId);
        Invoice invoice = invoice(bill);
        byte[] billPdf = new byte[] { 1, 2 };
        byte[] invoicePdf = new byte[] { 3, 4 };
        byte[] tallyBytes = new byte[] { 5, 6 };
        when(billRepository.findByIdAndFacilityId(bill.getId(), facilityId)).thenReturn(Optional.of(bill));
        when(invoiceRepository.findByIdAndFacilityId(invoice.getId(), facilityId)).thenReturn(Optional.of(invoice));
        when(billPdfService.generatePdf(bill.getId())).thenReturn(billPdf);
        when(invoicePdfService.generatePdf(bill.getId())).thenReturn(invoicePdf);
        when(tallyExportService.exportBillsForMonth(facilityId, java.time.YearMonth.of(2026, 6)))
                .thenReturn(tallyBytes);

        var billResponse = controller.downloadBillPdf(bill.getId());
        var invoiceResponse = controller.downloadInvoiceById(invoice.getId());
        var tallyResponse = controller.exportForTally(2026, 6);

        assertArrayEquals(billPdf, billResponse.getBody());
        assertEquals("no-store", billResponse.getHeaders().getCacheControl());
        assertArrayEquals(invoicePdf, invoiceResponse.getBody());
        assertEquals("no-store", invoiceResponse.getHeaders().getCacheControl());
        assertArrayEquals(tallyBytes, tallyResponse.getBody());
        assertEquals("no-store", tallyResponse.getHeaders().getCacheControl());
    }

    @Test
    void billingHistoryResponsesUseNoStoreCacheHeader() {
        UUID facilityId = TenantContext.getTenantId();
        Bill bill = bill(facilityId);
        when(billRepository.findByIdAndFacilityId(bill.getId(), facilityId)).thenReturn(Optional.of(bill));
        when(billVersionRepository.findByBillIdOrderByVersionDesc(bill.getId())).thenReturn(List.of());

        var versionsResponse = controller.getBillVersions(bill.getId());
        var rateHistoryResponse = controller.getExcessRateHistory();

        assertEquals("no-store", versionsResponse.getHeaders().getCacheControl());
        assertEquals("no-cache", versionsResponse.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertEquals("no-store", rateHistoryResponse.getHeaders().getCacheControl());
        assertEquals("no-cache", rateHistoryResponse.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    @Test
    void billingListRepositoriesFetchSummaryRelationships() throws NoSuchMethodException {
        assertEntityGraphContains(BillRepository.class, "findByFacilityId",
                new Class<?>[] { UUID.class, Pageable.class },
                "agreement", "agreement.hcf");
        assertEntityGraphContains(BillRepository.class, "findByFacilityAndMonth",
                new Class<?>[] { UUID.class, LocalDate.class },
                "agreement", "agreement.hcf");
        assertEntityGraphContains(InvoiceRepository.class, "findByFacilityId",
                new Class<?>[] { UUID.class, Pageable.class },
                "bill", "bill.agreement", "bill.agreement.hcf");
        assertEntityGraphContains(InvoiceRepository.class, "findByBillIdIn",
                new Class<?>[] { java.util.List.class },
                "bill");
    }

    private void assertValidatedBody(String methodName, Class<?> requestClass) throws NoSuchMethodException {
        Method method = BillingController.class.getDeclaredMethod(methodName, requestClass);
        Parameter parameter = method.getParameters()[0];

        assertTrue(parameter.isAnnotationPresent(Valid.class), methodName + " request must be validated");
        assertTrue(parameter.isAnnotationPresent(RequestBody.class), methodName + " request must remain a body");
    }

    private void assertFieldViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field + " on " + request.getClass().getSimpleName());
    }

    private void assertEntityGraphContains(Class<?> repositoryClass, String methodName, Class<?>[] parameterTypes,
            String... expectedPaths) throws NoSuchMethodException {
        Method method = repositoryClass.getDeclaredMethod(methodName, parameterTypes);
        EntityGraph graph = method.getAnnotation(EntityGraph.class);

        assertTrue(graph != null, methodName + " should fetch DTO relationships eagerly");
        assertTrue(Arrays.asList(graph.attributePaths()).containsAll(Arrays.asList(expectedPaths)),
                () -> methodName + " graph should include " + Arrays.toString(expectedPaths));
    }

    private Bill bill(UUID facilityId) {
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setCode("FAC");
        facility.setName("Facility");
        facility.setAddress("Address");

        Hcf hcf = new Hcf();
        hcf.setId(UUID.randomUUID());
        hcf.setCode("HCF");
        hcf.setName("City Hospital");
        hcf.setAddress("Address");

        Agreement agreement = new Agreement();
        agreement.setId(UUID.randomUUID());
        agreement.setFacility(facility);
        agreement.setHcf(hcf);
        agreement.setAgreementNumber("AGR-1");
        agreement.setStatusEnum(Agreement.Status.ACTIVE);
        agreement.setStartDate(LocalDate.of(2026, 1, 1));
        agreement.setPerBedPerDayRate(BigDecimal.TEN);

        Bill bill = new Bill();
        bill.setFacility(facility);
        bill.setAgreement(agreement);
        bill.setBillingMonth(LocalDate.of(2026, 6, 1));
        bill.setTotalAmount(new BigDecimal("100.00"));
        bill.setStatus(Bill.Status.FINALIZED.name());
        return bill;
    }

    private Invoice invoice(Bill bill) {
        Invoice invoice = new Invoice();
        invoice.setBill(bill);
        invoice.setFacility(bill.getFacility());
        invoice.setInvoiceNumber("FAC/2026-27/000001");
        invoice.setInvoiceDate(LocalDate.of(2026, 6, 30));
        invoice.setTotalAmount(new BigDecimal("100.00"));
        return invoice;
    }
}
