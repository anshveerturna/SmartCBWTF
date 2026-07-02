package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.DuesClearStatus;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.DuesClearanceRequestRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.service.PdfService;
import com.smartcbwtf.service.QrOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcfPortalAccessGuardTest {

    @Mock
    private QrOrderService qrOrderService;
    @Mock
    private PdfService pdfService;
    @Mock
    private HcfAccessGuard accessGuard;
    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private BagEventRepository bagEventRepository;
    @Mock
    private BagLabelRepository bagLabelRepository;
    @Mock
    private DuesClearanceRequestRepository duesRequestRepository;

    private UUID hcfId;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        hcfId = UUID.randomUUID();
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void qrPricingRequiresPortalAccess() {
        HcfQrOrderController controller = new HcfQrOrderController(qrOrderService, pdfService, accessGuard);
        when(qrOrderService.getPricing()).thenReturn(new QrOrderService.QrPricing(
                BigDecimal.ONE,
                BigDecimal.TEN,
                500));

        controller.getPricing();

        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
    }

    @Test
    void complianceStatusRequiresPortalAccess() {
        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        hcf.setDuesClearStatus(DuesClearStatus.PENDING);
        when(hcfRepository.findByIdAndFacilityId(hcfId, facilityId)).thenReturn(Optional.of(hcf));
        HcfComplianceController controller = new HcfComplianceController(
                hcfRepository,
                agreementRepository,
                bagEventRepository,
                bagLabelRepository,
                duesRequestRepository,
                accessGuard,
                pdfService);

        controller.getDuesStatus();

        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
    }
}
