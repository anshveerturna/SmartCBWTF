package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.QrLabelOrder;
import com.smartcbwtf.domain.QrLabelOrder.QrOrderStatus;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.QrLabelOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrOrderServiceSecurityTest {

    @Mock
    private QrLabelOrderRepository qrOrderRepository;
    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private SystemConfigService configService;
    @Mock
    private LabelService labelService;
    @Mock
    private AuditLogService auditLogService;

    private QrOrderService qrOrderService;
    private UUID tenantFacilityId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        qrOrderService = new QrOrderService(
                qrOrderRepository,
                hcfRepository,
                facilityRepository,
                agreementRepository,
                configService,
                labelService,
                auditLogService);
        tenantFacilityId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(adminUserId, tenantFacilityId, null, "CBWTF_ADMIN", "admin"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void adminDirectGenerateMultiResolvesAgreementInsideAuthenticatedTenant() {
        UUID hcfId = UUID.randomUUID();
        when(configService.getInt("qr.max_quantity_per_order", 500)).thenReturn(500);
        when(agreementRepository.findActiveByHcfAndFacility(hcfId, tenantFacilityId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> qrOrderService.adminDirectGenerateMulti(
                        hcfId,
                        Map.of("YELLOW", 1),
                        adminUserId,
                        null));

        verify(agreementRepository).findActiveByHcfAndFacility(hcfId, tenantFacilityId);
        verifyNoInteractions(hcfRepository, facilityRepository, labelService, auditLogService, qrOrderRepository);
    }

    @Test
    void fulfillMasksOrdersOutsideAuthenticatedFacility() {
        UUID orderId = UUID.randomUUID();
        when(qrOrderRepository.findByIdAndFacilityId(orderId, tenantFacilityId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> qrOrderService.fulfillRequest(orderId, tenantFacilityId, adminUserId));

        verify(qrOrderRepository).findByIdAndFacilityId(orderId, tenantFacilityId);
        verify(qrOrderRepository, never()).findById(orderId);
        verifyNoInteractions(labelService);
    }

    @Test
    void rejectMasksOrdersOutsideAuthenticatedFacility() {
        UUID orderId = UUID.randomUUID();
        when(qrOrderRepository.findByIdAndFacilityId(orderId, tenantFacilityId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> qrOrderService.rejectRequest(orderId, tenantFacilityId, adminUserId, "wrong tenant"));

        verify(qrOrderRepository).findByIdAndFacilityId(orderId, tenantFacilityId);
        verify(qrOrderRepository, never()).findById(orderId);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void listAllOrdersUsesBoundedRecentFacilityQuery() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        qrOrderService.listAllOrders(tenantFacilityId, null, 5000);

        verify(qrOrderRepository).findRecentByFacilityId(eq(tenantFacilityId), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(250, pageable.getValue().getPageSize());
    }

    @Test
    void listAllOrdersWithStatusUsesBoundedStatusQuery() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        qrOrderService.listAllOrders(tenantFacilityId, "fulfilled", 25);

        verify(qrOrderRepository).findRecentByFacilityIdAndStatus(
                eq(tenantFacilityId), eq(QrOrderStatus.FULFILLED), pageable.capture());
        assertEquals(25, pageable.getValue().getPageSize());
    }

    @Test
    void listHcfOrdersDefaultsInvalidLimit() {
        UUID hcfId = UUID.randomUUID();
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        qrOrderService.listHcfOrders(hcfId, 0);

        verify(qrOrderRepository).findByHcfIdAndFacilityIdOrderByRequestedAtDesc(
                eq(hcfId), eq(tenantFacilityId), pageable.capture());
        assertEquals(100, pageable.getValue().getPageSize());
        verify(qrOrderRepository, never()).findByHcfIdOrderByRequestedAtDesc(eq(hcfId), any(Pageable.class));
    }

    @Test
    void hcfCreateRequestResolvesHcfAndAgreementInsideAuthenticatedTenant() {
        UUID hcfId = UUID.randomUUID();
        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        Facility facility = new Facility();
        facility.setId(tenantFacilityId);
        Agreement agreement = new Agreement();
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setStatusEnum(Agreement.Status.ACTIVE);

        when(configService.getInt("qr.max_quantity_per_order", 500)).thenReturn(500);
        when(configService.getString("qr.price.cbwtf_request_per_unit", "5.00")).thenReturn("5.00");
        when(hcfRepository.findByIdAndFacilityId(hcfId, tenantFacilityId)).thenReturn(Optional.of(hcf));
        when(agreementRepository.findActiveByHcfAndFacility(hcfId, tenantFacilityId)).thenReturn(Optional.of(agreement));
        when(qrOrderRepository.save(any(QrLabelOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        qrOrderService.createCbwtfRequest(hcfId, "YELLOW", 1, null);

        verify(hcfRepository).findByIdAndFacilityId(hcfId, tenantFacilityId);
        verify(agreementRepository).findActiveByHcfAndFacility(hcfId, tenantFacilityId);
        verify(hcfRepository, never()).findById(hcfId);
        verify(agreementRepository, never()).findFirstByHcfIdAndStatusOrderByStartDateDesc(
                hcfId, Agreement.Status.ACTIVE.name());
    }
}
