package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.dto.LabelIssueRequest;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LabelServiceSecurityTest {

    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private AgreementGuardService agreementGuard;
    @Mock
    private PdfService pdfService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private QrAuthorizationService qrAuthService;

    private LabelService labelService;
    private UUID tenantFacilityId;

    @BeforeEach
    void setUp() {
        labelService = new LabelService(
                hcfRepository,
                facilityRepository,
                agreementRepository,
                agreementGuard,
                pdfService,
                auditLogService,
                qrAuthService);
        tenantFacilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(
                UUID.randomUUID(), tenantFacilityId, null, "CBWTF_ADMIN", "admin"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void issueRejectsClientSuppliedFacilityOutsideTenantBeforeLookup() {
        LabelIssueRequest request = new LabelIssueRequest();
        request.setHcfId(UUID.randomUUID());
        request.setFacilityId(UUID.randomUUID());
        request.setCategory("YELLOW");
        request.setQuantity(1);

        assertThrows(TenantAssertionService.TenantAccessDeniedException.class,
                () -> labelService.issue(request));

        verifyNoInteractions(hcfRepository, facilityRepository, agreementRepository, agreementGuard,
                pdfService, auditLogService, qrAuthService);
    }

    @Test
    void issueMultiCategoryRejectsFacilityOutsideTenantBeforeLookup() {
        assertThrows(TenantAssertionService.TenantAccessDeniedException.class,
                () -> labelService.issueMultiCategory(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Map.of("YELLOW", 1),
                        null));

        verifyNoInteractions(hcfRepository, facilityRepository, agreementRepository, agreementGuard,
                pdfService, auditLogService, qrAuthService);
    }
}
