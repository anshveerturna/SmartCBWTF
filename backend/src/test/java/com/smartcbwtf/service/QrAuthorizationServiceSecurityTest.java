package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AlertSeverity;
import com.smartcbwtf.domain.AlertType;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.QrAuthorization;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.QrAuthorizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrAuthorizationServiceSecurityTest {

    @Mock
    private QrAuthorizationRepository qrRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private BagLabelRepository bagLabelRepository;
    @Mock
    private QrSigningService signingService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private AlertService alertService;

    private QrAuthorizationService qrService;
    private UUID tenantFacilityId;

    @BeforeEach
    void setUp() {
        qrService = new QrAuthorizationService(
                qrRepository,
                agreementRepository,
                hcfRepository,
                facilityRepository,
                bagLabelRepository,
                signingService,
                auditLogService,
                alertService);
        tenantFacilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(
                UUID.randomUUID(), tenantFacilityId, null, "CBWTF_ADMIN", "admin"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void generateQrResolvesAgreementInsideAuthenticatedTenant() {
        UUID hcfId = UUID.randomUUID();
        when(agreementRepository.findActiveByHcfAndFacility(hcfId, tenantFacilityId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> qrService.generateQr(
                        hcfId,
                        "YELLOW",
                        Instant.now(),
                        Instant.now().plus(1, ChronoUnit.DAYS),
                        UUID.randomUUID()));

        verify(agreementRepository).findActiveByHcfAndFacility(hcfId, tenantFacilityId);
        verifyNoInteractions(hcfRepository, facilityRepository, qrRepository, bagLabelRepository,
                signingService, auditLogService, alertService);
    }

    @Test
    void bulkGenerateResolvesAgreementInsideAuthenticatedTenant() {
        UUID hcfId = UUID.randomUUID();
        when(agreementRepository.findActiveByHcfAndFacility(hcfId, tenantFacilityId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> qrService.generateQrBulk(
                        hcfId,
                        "YELLOW",
                        5,
                        Instant.now(),
                        Instant.now().plus(1, ChronoUnit.DAYS),
                        UUID.randomUUID()));

        verify(agreementRepository).findActiveByHcfAndFacility(hcfId, tenantFacilityId);
        verifyNoInteractions(hcfRepository, facilityRepository, qrRepository, bagLabelRepository,
                signingService, auditLogService, alertService);
    }

    @Test
    void listQrsUsesBoundedFacilityHcfStatusQuery() {
        UUID hcfId = UUID.randomUUID();
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(qrRepository.findByFacilityIdAndHcfIdAndStatusOrderByCreatedAtDesc(
                eq(tenantFacilityId), eq(hcfId), eq(QrAuthorization.Status.ACTIVE.name()), pageable.capture()))
                .thenReturn(List.of());

        qrService.listQrs(hcfId, "active", 5000);

        assertEquals(250, pageable.getValue().getPageSize());
        verify(qrRepository).findByFacilityIdAndHcfIdAndStatusOrderByCreatedAtDesc(
                eq(tenantFacilityId), eq(hcfId), eq(QrAuthorization.Status.ACTIVE.name()),
                org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(qrRepository, never()).findByFacilityIdAndHcfIdOrderByCreatedAtDesc(tenantFacilityId, hcfId);
    }

    @Test
    void listQrsRejectsInvalidStatusBeforeQuerying() {
        List<?> result = qrService.listQrs(null, "definitely-not-a-status", 100);

        assertEquals(List.of(), result);
        verifyNoInteractions(qrRepository);
    }

    @Test
    void verificationSlaBreachCreatesPortalAlert() {
        UUID qrId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        Instant usedAt = Instant.now().minus(25, ChronoUnit.HOURS);

        Facility facility = new Facility();
        facility.setId(facilityId);

        Hcf hcf = new Hcf();
        hcf.setName("City Clinic");

        QrAuthorization qr = new QrAuthorization();
        qr.setId(qrId);
        qr.setFacility(facility);
        qr.setHcf(hcf);
        qr.setWasteCategory("YELLOW");
        qr.setUsedAt(usedAt);

        when(qrRepository.findUsedQrsBeyondSla(any(Instant.class))).thenReturn(List.of(qr));

        qrService.checkVerificationSla();

        verify(auditLogService).log(eq("QR"), eq(qrId), eq("QR_VERIFICATION_SLA_BREACHED"), eq(null),
                contains("\"slaHours\":24"));
        verify(alertService).createAlert(
                eq(qrId),
                eq(facilityId),
                eq(AlertType.QR_VERIFICATION_SLA_BREACHED),
                eq(AlertSeverity.WARN),
                eq("QR verification SLA breached"),
                contains("City Clinic YELLOW QR"),
                eq("QR_AUTHORIZATION"),
                eq(qrId));
    }
}
