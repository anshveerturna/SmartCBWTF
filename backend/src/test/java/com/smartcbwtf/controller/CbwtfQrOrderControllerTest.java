package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.QrLabelOrder;
import com.smartcbwtf.service.PdfService;
import com.smartcbwtf.service.QrOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CbwtfQrOrderControllerTest {

    @Mock
    private QrOrderService qrOrderService;
    @Mock
    private PdfService pdfService;

    private CbwtfQrOrderController controller;
    private UUID facilityId;
    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        controller = new CbwtfQrOrderController(qrOrderService, pdfService);
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listAllOrdersUsesFacilityHistoryQuery() {
        when(qrOrderService.listAllOrders(facilityId, null, 100)).thenReturn(List.of());

        var response = controller.listAllOrders(null, 100);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(), response.getBody());
        verify(qrOrderService).listAllOrders(facilityId, null, 100);
        verify(qrOrderService, never()).listPendingOrders(facilityId);
    }

    @Test
    void listAllOrdersPassesStatusAndLimitToService() {
        when(qrOrderService.listAllOrders(facilityId, "FULFILLED", 25)).thenReturn(List.of());

        var response = controller.listAllOrders("FULFILLED", 25);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(qrOrderService).listAllOrders(facilityId, "FULFILLED", 25);
    }

    @Test
    void downloadOrderPdfUsesNoStoreCacheHeader() throws Exception {
        UUID orderId = UUID.randomUUID();
        QrLabelOrder order = new QrLabelOrder();
        order.setId(orderId);
        order.setPdfUrl("/files/labels/order.pdf");
        Path pdf = tempDir.resolve("order.pdf");
        Files.write(pdf, new byte[] { 1, 2, 3 });
        when(qrOrderService.getOrderPdfForFacility(orderId, facilityId)).thenReturn(order);
        when(pdfService.generatedFilePath(order.getPdfUrl())).thenReturn(pdf);

        var response = controller.downloadOrderPdf(orderId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("no-store", response.getHeaders().getCacheControl());
    }
}
