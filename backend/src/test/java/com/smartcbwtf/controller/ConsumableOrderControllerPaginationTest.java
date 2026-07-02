package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.ConsumableCategory;
import com.smartcbwtf.domain.ConsumableItem;
import com.smartcbwtf.domain.ConsumableOrder;
import com.smartcbwtf.domain.ConsumableOrderItem;
import com.smartcbwtf.domain.ConsumablePricing;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.ConsumableItemRepository;
import com.smartcbwtf.repository.ConsumableOrderRepository;
import com.smartcbwtf.repository.ConsumablePricingRepository;
import com.smartcbwtf.service.EmailService;
import com.smartcbwtf.service.HcfAccessGuard;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumableOrderControllerPaginationTest {

    @Mock
    private ConsumableOrderRepository orderRepo;
    @Mock
    private AgreementRepository agreementRepo;
    @Mock
    private EmailService emailService;
    @Mock
    private ConsumableItemRepository itemRepo;
    @Mock
    private ConsumablePricingRepository pricingRepo;
    @Mock
    private HcfAccessGuard accessGuard;

    private CbwtfConsumableOrderController cbwtfController;
    private HcfConsumableOrderController hcfController;
    private UUID facilityId;
    private UUID hcfId;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void setUp() {
        cbwtfController = new CbwtfConsumableOrderController(orderRepo, agreementRepo, emailService);
        hcfController = new HcfConsumableOrderController(
                orderRepo, itemRepo, pricingRepo, agreementRepo, accessGuard, emailService);
        facilityId = UUID.randomUUID();
        hcfId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void cbwtfListOrdersClampsLimitAndNormalizesStatus() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(orderRepo.findByFacilityIdAndStatusOrderByOrderedAtDesc(
                eq(facilityId), eq("PENDING"), pageable.capture())).thenReturn(List.of());
        when(orderRepo.countByFacilityIdAndStatus(facilityId, "PENDING")).thenReturn(432L);

        var response = cbwtfController.listOrders("pending", 5000);

        assertEquals(250, pageable.getValue().getPageSize());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(432L, body.get("total"));
    }

    @Test
    void cbwtfListOrdersRejectsInvalidStatusBeforeQuerying() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));

        var response = cbwtfController.listOrders("not-a-status", 100);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(0L, body.get("total"));
        verifyNoInteractions(orderRepo);
    }

    @Test
    void cbwtfListOrdersBatchesItemCountsAndAgreementNumbers() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        ConsumableOrder order = consumableOrder(ConsumableOrder.Status.PENDING);
        when(orderRepo.findByFacilityIdOrderByOrderedAtDesc(eq(facilityId), any(Pageable.class)))
                .thenReturn(List.of(order));
        when(orderRepo.countByFacilityId(facilityId)).thenReturn(1L);
        when(orderRepo.countItemsByOrderIds(List.of(order.getId())))
                .thenReturn(List.<Object[]>of(new Object[] { order.getId(), 3L }));
        when(agreementRepo.findActiveAgreementNumbersByFacilityAndHcfIds(facilityId, List.of(hcfId)))
                .thenReturn(List.<Object[]>of(new Object[] { hcfId, "AGR-1" }));

        var response = cbwtfController.listOrders(null, 100);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orders = (List<Map<String, Object>>) body.get("orders");
        assertEquals(3L, orders.get(0).get("itemCount"));
        assertEquals("AGR-1", orders.get(0).get("agreementNumber"));
        verify(orderRepo).countItemsByOrderIds(List.of(order.getId()));
        verify(agreementRepo).findActiveAgreementNumbersByFacilityAndHcfIds(facilityId, List.of(hcfId));
        verify(agreementRepo, never()).findByHcfIdAndStatus(hcfId, Agreement.Status.ACTIVE.name());
    }

    @Test
    void hcfListOrdersDefaultsInvalidLimitAndKeepsTotalCount() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(orderRepo.findByHcfIdAndFacilityIdOrderByOrderedAtDesc(eq(hcfId), eq(facilityId), pageable.capture()))
                .thenReturn(List.of());
        when(orderRepo.countByHcfIdAndFacilityId(hcfId, facilityId)).thenReturn(123L);

        var response = hcfController.getOrders(null, 0);

        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
        assertEquals(100, pageable.getValue().getPageSize());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(123L, body.get("total"));
        verify(orderRepo, never()).findByHcfIdOrderByOrderedAtDesc(eq(hcfId), any(Pageable.class));
        verify(orderRepo, never()).countByHcfId(hcfId);
    }

    @Test
    void hcfListOrdersBatchesItemCountsForVisiblePage() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
        ConsumableOrder order = consumableOrder(ConsumableOrder.Status.PENDING);
        when(orderRepo.findByHcfIdAndFacilityIdOrderByOrderedAtDesc(eq(hcfId), eq(facilityId), any(Pageable.class)))
                .thenReturn(List.of(order));
        when(orderRepo.countByHcfIdAndFacilityId(hcfId, facilityId)).thenReturn(1L);
        when(orderRepo.countItemsByOrderIds(List.of(order.getId())))
                .thenReturn(List.<Object[]>of(new Object[] { order.getId(), 2L }));

        var response = hcfController.getOrders(null, 100);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orders = (List<Map<String, Object>>) body.get("orders");
        assertEquals(2L, orders.get(0).get("itemCount"));
        verify(orderRepo).countItemsByOrderIds(List.of(order.getId()));
    }

    @Test
    void hcfCatalogBatchesActivePricingLookups() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
        ConsumableItem firstItem = consumableItem("BAG-Y", "Yellow Bag");
        ConsumableItem secondItem = consumableItem("BAG-R", "Red Bag");
        when(agreementRepo.findActiveByHcfAndFacility(hcfId, facilityId))
                .thenReturn(Optional.of(activeAgreement()));
        when(itemRepo.findActiveByFacility(facilityId)).thenReturn(List.of(firstItem, secondItem));
        when(pricingRepo.findActiveByConsumableItemIdIn(any()))
                .thenReturn(List.of(
                        pricing(firstItem, "12.50", "5.00"),
                        pricing(secondItem, "18.75", "12.00")));

        var response = hcfController.getCatalog();

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertEquals(new BigDecimal("12.50"), items.get(0).get("price"));
        assertEquals(new BigDecimal("18.75"), items.get(1).get("price"));

        ArgumentCaptor<List<UUID>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(pricingRepo).findActiveByConsumableItemIdIn(idsCaptor.capture());
        assertEquals(List.of(firstItem.getId(), secondItem.getId()), idsCaptor.getValue());
        verify(pricingRepo, never()).findActiveByConsumableItemId(any(UUID.class));
    }

    @Test
    void hcfPlaceOrderValidatesClientControlledFields() throws NoSuchMethodException {
        Method method = HcfConsumableOrderController.class.getDeclaredMethod("placeOrder",
                HcfConsumableOrderController.PlaceOrderRequest.class);
        Parameter parameter = method.getParameters()[0];

        assertTrue(parameter.isAnnotationPresent(Valid.class), "placeOrder request must be validated");
        assertTrue(parameter.isAnnotationPresent(RequestBody.class), "placeOrder request must remain a body");
        assertFieldViolation(orderWithItems(List.of()), "items");
        assertFieldViolation(orderWithItems(List.of(item(null, 1))), "itemId");
        assertFieldViolation(orderWithItems(List.of(item(UUID.randomUUID(), 0))), "quantity");
        assertFieldViolation(orderWithItems(List.of(item(UUID.randomUUID(), 100_001))), "quantity");
        assertFieldViolation(orderWithNotes("x".repeat(1001)), "notes");
        assertTrue(validator.validate(orderWithItems(List.of(item(UUID.randomUUID(), 1)))).isEmpty());
    }

    @Test
    void cbwtfStatusMutationEndpointsValidateOptionalNotes() throws NoSuchMethodException {
        assertValidatedCbwtfNotesBody("confirmOrder");
        assertValidatedCbwtfNotesBody("dispatchOrder");
        assertValidatedCbwtfNotesBody("deliverOrder");
        assertValidatedCbwtfNotesBody("cancelOrder");

        CbwtfConsumableOrderController.NotesRequest request = new CbwtfConsumableOrderController.NotesRequest();
        request.notes = "x".repeat(1001);
        assertFieldViolation(request, "notes");
    }

    @Test
    void cbwtfConfirmOrderStoresTrimmedNotes() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        ConsumableOrder order = consumableOrder(ConsumableOrder.Status.PENDING);
        CbwtfConsumableOrderController.NotesRequest notes = new CbwtfConsumableOrderController.NotesRequest();
        notes.notes = "  packed for dispatch  ";
        when(orderRepo.findByIdAndFacilityId(order.getId(), facilityId)).thenReturn(Optional.of(order));
        when(agreementRepo.findByHcfIdAndStatus(hcfId, Agreement.Status.ACTIVE.name())).thenReturn(List.of());

        var response = cbwtfController.confirmOrder(order.getId(), notes);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(ConsumableOrder.Status.CONFIRMED.name(), order.getStatus());
        assertEquals("packed for dispatch", order.getCbwtfNotes());
        verify(orderRepo).save(order);
        verify(orderRepo, never()).findById(order.getId());
    }

    @Test
    void cbwtfExportOrdersNeutralizesSpreadsheetFormulaText() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        ConsumableOrder order = consumableOrder(ConsumableOrder.Status.PENDING);
        order.setOrderNumber("=ORDER()");
        order.getHcf().setName("+HCF\n\"quoted\"");
        ConsumableOrderItem item = consumableOrderItem("@Yellow bag", "-kg");
        order.getItems().add(item);

        Agreement agreement = activeAgreement();
        agreement.setAgreementNumber("\t=AGR");
        when(orderRepo.countByFacilityIdAndOrderedAtAfter(eq(facilityId), any()))
                .thenReturn(1L);
        when(orderRepo.findExportRowsByFacilityIdAndOrderedAtAfter(eq(facilityId), any()))
                .thenReturn(List.of(order));
        when(agreementRepo.findActiveAgreementNumbersByFacilityAndHcfIds(facilityId, List.of(hcfId)))
                .thenReturn(List.<Object[]>of(new Object[] { hcfId, agreement.getAgreementNumber() }));

        var response = cbwtfController.exportOrders("month");

        String csv = response.getBody();
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertTrue(csv.contains("\"'=ORDER()\""));
        assertTrue(csv.contains("\"'+HCF \"\"quoted\"\"\""));
        assertTrue(csv.contains("\"'\t=AGR\""));
        assertTrue(csv.contains("\"'@Yellow bag\""));
        assertTrue(csv.contains("\"'-kg\""));
        verify(agreementRepo).findActiveAgreementNumbersByFacilityAndHcfIds(facilityId, List.of(hcfId));
        verify(agreementRepo, never()).findByHcfIdAndStatus(hcfId, Agreement.Status.ACTIVE.name());
    }

    @Test
    void cbwtfExportOrdersRejectsOversizedCsvBeforeLoadingRows() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(orderRepo.countByFacilityIdAndOrderedAtAfter(eq(facilityId), any()))
                .thenReturn(5_001L);

        var response = cbwtfController.exportOrders("month");

        assertEquals(413, response.getStatusCode().value());
        assertTrue(response.getBody().contains("5001 orders"));
        assertEquals("no-store", response.getHeaders().getCacheControl());
        verify(orderRepo, never()).findExportRowsByFacilityIdAndOrderedAtAfter(eq(facilityId), any());
        verify(agreementRepo, never()).findActiveAgreementNumbersByFacilityAndHcfIds(any(), any());
    }

    @Test
    void cbwtfAnalyticsRejectsOversizedResultBeforeLoadingRows() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(orderRepo.countByFacilityIdAndOrderedAtAfter(eq(facilityId), any()))
                .thenReturn(5_001L);

        var response = cbwtfController.getAnalytics("week");

        assertEquals(413, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("RESULT_SET_TOO_LARGE", body.get("error"));
        assertEquals(5_001L, body.get("totalRows"));
        assertEquals("no-store", response.getHeaders().getCacheControl());
        verify(orderRepo, never()).findByFacilityIdAndOrderedAtAfterOrderByOrderedAtDesc(eq(facilityId), any());
    }

    @Test
    void cbwtfExportOrdersNormalizesInvalidPeriodBeforeFilenameHeader() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(orderRepo.countByFacilityIdAndOrderedAtAfter(eq(facilityId), any()))
                .thenReturn(0L);
        when(orderRepo.findExportRowsByFacilityIdAndOrderedAtAfter(eq(facilityId), any()))
                .thenReturn(List.of());

        var response = cbwtfController.exportOrders("bad\"\r\nperiod");

        String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
        assertEquals(200, response.getStatusCode().value());
        assertTrue(contentDisposition.contains("consumable_orders_month_"));
        assertTrue(!contentDisposition.contains("bad"));
    }

    @Test
    void cbwtfOrderDetailsUsesTenantScopedLookup() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        UUID orderId = UUID.randomUUID();
        when(orderRepo.findByIdAndFacilityId(orderId, facilityId)).thenReturn(Optional.empty());

        var response = cbwtfController.getOrderDetails(orderId);

        assertEquals(404, response.getStatusCode().value());
        verify(orderRepo).findByIdAndFacilityId(orderId, facilityId);
        verify(orderRepo, never()).findById(orderId);
    }

    @Test
    void hcfOrderDetailsUsesHcfScopedLookup() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
        UUID orderId = UUID.randomUUID();
        when(orderRepo.findByIdAndHcfIdAndFacilityId(orderId, hcfId, facilityId)).thenReturn(Optional.empty());

        var response = hcfController.getOrderDetails(orderId);

        assertEquals(404, response.getStatusCode().value());
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
        verify(orderRepo).findByIdAndHcfIdAndFacilityId(orderId, hcfId, facilityId);
        verify(orderRepo, never()).findByIdAndHcfId(orderId, hcfId);
        verify(orderRepo, never()).findById(orderId);
    }

    @Test
    void hcfOrderHistoryAndDetailsAreNotCacheable() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
        ConsumableOrder order = consumableOrder(ConsumableOrder.Status.PENDING);
        when(orderRepo.findByHcfIdAndFacilityIdOrderByOrderedAtDesc(eq(hcfId), eq(facilityId), any(Pageable.class)))
                .thenReturn(List.of(order));
        when(orderRepo.countByHcfIdAndFacilityId(hcfId, facilityId)).thenReturn(1L);
        when(orderRepo.countItemsByOrderIds(List.of(order.getId())))
                .thenReturn(List.<Object[]>of(new Object[] { order.getId(), 1L }));
        when(orderRepo.findByIdAndHcfIdAndFacilityId(order.getId(), hcfId, facilityId))
                .thenReturn(Optional.of(order));

        var listResponse = hcfController.getOrders(null, 100);
        var detailResponse = hcfController.getOrderDetails(order.getId());

        assertEquals("no-store", listResponse.getHeaders().getCacheControl());
        assertEquals("no-cache", listResponse.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertEquals("no-store", detailResponse.getHeaders().getCacheControl());
        assertEquals("no-cache", detailResponse.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    @Test
    void hcfCancelOrderUsesHcfScopedLookupBeforeSaving() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
        UUID orderId = UUID.randomUUID();
        when(orderRepo.findByIdAndHcfIdAndFacilityId(orderId, hcfId, facilityId)).thenReturn(Optional.empty());

        var response = hcfController.cancelOrder(orderId);

        assertEquals(404, response.getStatusCode().value());
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
        verify(orderRepo).findByIdAndHcfIdAndFacilityId(orderId, hcfId, facilityId);
        verify(orderRepo, never()).findByIdAndHcfId(orderId, hcfId);
        verify(orderRepo, never()).findById(orderId);
        verify(orderRepo, never()).save(any());
    }

    @Test
    void hcfPlaceOrderRejectsItemsOutsideTenantFacilityBeforePricingOrSave() {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, facilityId, hcfId, "HCF_ADMIN", "hcf"));
        when(agreementRepo.findActiveByHcfAndFacility(hcfId, facilityId))
                .thenReturn(Optional.of(activeAgreement()));
        when(orderRepo.countByFacilityId(facilityId)).thenReturn(0L);
        when(itemRepo.findByIdAndFacilityId(itemId, facilityId)).thenReturn(Optional.empty());

        var response = hcfController.placeOrder(orderWithItems(List.of(item(itemId, 1))));

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("INVALID_ITEM", body.get("error"));
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
        verify(agreementRepo).findActiveByHcfAndFacility(hcfId, facilityId);
        verify(agreementRepo, never()).findByHcfIdAndStatus(hcfId, Agreement.Status.ACTIVE.name());
        verify(itemRepo).findByIdAndFacilityId(itemId, facilityId);
        verifyNoInteractions(pricingRepo);
        verify(orderRepo, never()).save(any());
    }

    private Agreement activeAgreement() {
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setCode("FAC");
        facility.setName("Facility");

        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        hcf.setCode("HCF");
        hcf.setName("HCF");

        Agreement agreement = new Agreement();
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setStatusEnum(Agreement.Status.ACTIVE);
        return agreement;
    }

    private HcfConsumableOrderController.PlaceOrderRequest orderWithItems(
            List<HcfConsumableOrderController.OrderItemRequest> items) {
        HcfConsumableOrderController.PlaceOrderRequest request = new HcfConsumableOrderController.PlaceOrderRequest();
        request.items = items;
        request.notes = "Please deliver at reception";
        return request;
    }

    private HcfConsumableOrderController.PlaceOrderRequest orderWithNotes(String notes) {
        HcfConsumableOrderController.PlaceOrderRequest request = orderWithItems(List.of(item(UUID.randomUUID(), 1)));
        request.notes = notes;
        return request;
    }

    private HcfConsumableOrderController.OrderItemRequest item(UUID itemId, Integer quantity) {
        HcfConsumableOrderController.OrderItemRequest item = new HcfConsumableOrderController.OrderItemRequest();
        item.itemId = itemId;
        item.quantity = quantity;
        return item;
    }

    private ConsumableOrder consumableOrder(ConsumableOrder.Status status) {
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setCode("FAC");
        facility.setName("Facility");

        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        hcf.setCode("HCF");
        hcf.setName("HCF");
        hcf.setAddress("Address");

        ConsumableOrder order = new ConsumableOrder();
        order.setId(UUID.randomUUID());
        order.setFacility(facility);
        order.setHcf(hcf);
        order.setOrderNumber("ORD-1");
        order.setStatusEnum(status);
        order.setOrderedBy(UUID.randomUUID());
        return order;
    }

    private ConsumableOrderItem consumableOrderItem(String itemName, String unitOfMeasure) {
        ConsumableOrderItem item = new ConsumableOrderItem();
        item.setItemName(itemName);
        item.setUnitOfMeasure(unitOfMeasure);
        item.setQuantity(2);
        item.setPricePerUnit(new BigDecimal("10.00"));
        item.setGstRate(new BigDecimal("5.00"));
        item.setLineSubtotal(new BigDecimal("20.00"));
        item.setLineGst(new BigDecimal("1.00"));
        item.setLineTotal(new BigDecimal("21.00"));
        return item;
    }

    private ConsumableItem consumableItem(String code, String name) {
        Facility facility = new Facility();
        facility.setId(facilityId);

        ConsumableCategory category = new ConsumableCategory();
        category.setId(UUID.randomUUID());
        category.setName("Bags");

        ConsumableItem item = new ConsumableItem();
        item.setId(UUID.randomUUID());
        item.setFacility(facility);
        item.setCategory(category);
        item.setConsumableCode(code);
        item.setName(name);
        item.setUnitOfMeasure("pcs");
        item.setIsActive(true);
        return item;
    }

    private ConsumablePricing pricing(ConsumableItem item, String price, String gstRate) {
        ConsumablePricing pricing = new ConsumablePricing();
        pricing.setId(UUID.randomUUID());
        pricing.setConsumableItem(item);
        pricing.setPricePerUnit(new BigDecimal(price));
        pricing.setGstRate(new BigDecimal(gstRate));
        pricing.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        pricing.setIsActive(true);
        return pricing;
    }

    private void assertValidatedCbwtfNotesBody(String methodName) throws NoSuchMethodException {
        Method method = CbwtfConsumableOrderController.class.getDeclaredMethod(
                methodName, UUID.class, CbwtfConsumableOrderController.NotesRequest.class);
        Parameter parameter = method.getParameters()[1];

        assertTrue(parameter.isAnnotationPresent(Valid.class), methodName + " request must be validated");
        assertTrue(parameter.isAnnotationPresent(RequestBody.class), methodName + " request must remain a body");
    }

    private void assertFieldViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> {
                    String property = violation.getPropertyPath().toString();
                    return field.equals(property) || property.endsWith("." + field);
                }),
                () -> "Expected validation violation for " + field);
    }
}
