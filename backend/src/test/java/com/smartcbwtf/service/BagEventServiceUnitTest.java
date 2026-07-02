package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.domain.BagLabel;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.QrAuthorization;
import com.smartcbwtf.dto.BagEventSyncItem;
import com.smartcbwtf.dto.BagEventSyncRequest;
import com.smartcbwtf.dto.BagVerifyRequest;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.QrAuthorizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BagEventServiceUnitTest {

    @Mock
    private BagLabelRepository bagLabelRepository;
    @Mock
    private BagEventRepository bagEventRepository;
    @Mock
    private QrAuthorizationRepository qrAuthorizationRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private AgreementGuardService agreementGuardService;
    @Mock
    private AuditLogService auditLogService;

    private BagEventService bagEventService;
    private UUID userId;
    private BagLabel label;

    @BeforeEach
    void setUp() {
        bagEventService = new BagEventService(
                bagLabelRepository,
                bagEventRepository,
                qrAuthorizationRepository,
                agreementRepository,
                agreementGuardService,
                auditLogService,
                200,
                200,
                0.5,
                100
        );

        userId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, facilityId, hcfId, "DRIVER", "unit-test"));

        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setGpsLat(28.6139);
        facility.setGpsLon(77.2090);
        facility.setGeofenceRadiusM(500);

        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        hcf.setGpsLat(28.6140);
        hcf.setGpsLon(77.2091);

        label = new BagLabel();
        label.setId(UUID.randomUUID());
        label.setFacility(facility);
        label.setHcf(hcf);
        label.setQrCode("{\"qrId\":\"abc\"}");
        label.setStatus("ISSUED");

        Agreement agreement = new Agreement();
        agreement.setId(UUID.randomUUID());
        agreement.setFacility(facility);
        agreement.setHcf(hcf);
        agreement.setStatusEnum(Agreement.Status.ACTIVE);
        agreement.setStartDate(LocalDate.now().minusDays(1));
        agreement.setPerBedPerDayRate(BigDecimal.ONE);

        lenient().when(agreementRepository.findActiveByHcfAndFacility(hcf.getId(), facility.getId()))
                .thenReturn(Optional.of(agreement));
        lenient().when(qrAuthorizationRepository.findFirstByQrPayloadAndFacilityId(anyString(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void syncMasksCrossTenantLabelBeforeAgreementLookup() {
        label.setFacility(otherFacility());

        BagEventSyncItem item = new BagEventSyncItem();
        item.setQrCode(label.getQrCode());
        item.setEventType("HCF_COLLECTION");
        item.setEventTs(Instant.now());
        item.setGpsLat(28.6140);
        item.setGpsLon(77.2091);
        item.setWeightKg(BigDecimal.valueOf(2.0));

        BagEventSyncRequest request = new BagEventSyncRequest();
        request.setEvents(List.of(item));

        when(bagLabelRepository.findByQrCode(label.getQrCode())).thenReturn(Optional.of(label));

        var response = bagEventService.sync(request);

        assertEquals("FAILED", response.getAcks().get(0).getStatus());
        assertEquals("Label not found", response.getAcks().get(0).getMessage());
        verify(agreementRepository, never()).findActiveByHcfAndFacility(any(), any());
        verify(bagEventRepository, never()).save(any(BagEvent.class));
    }

    @Test
    void verifyBagMasksCrossTenantLabelBeforeAgreementLookup() {
        label.setFacility(otherFacility());

        BagVerifyRequest request = new BagVerifyRequest();
        request.setQrCode(label.getQrCode());
        request.setVerifiedByUserId(userId);
        request.setGpsLat(28.6140);
        request.setGpsLon(77.2091);
        request.setWeightKg(BigDecimal.valueOf(1.25));

        when(bagLabelRepository.findByQrCode(label.getQrCode())).thenReturn(Optional.of(label));

        BagEventService.VerifyResult result = bagEventService.verifyBag(request);

        assertEquals(404, result.getHttpStatus());
        assertEquals("NOT_FOUND", result.getResponse().getStatus());
        verify(agreementRepository, never()).findActiveByHcfAndFacility(any(), any());
        verify(bagEventRepository, never()).save(any(BagEvent.class));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void verifyBagRejectsWhenCollectionNotRecorded() {
        BagVerifyRequest request = new BagVerifyRequest();
        request.setQrCode(label.getQrCode());
        request.setVerifiedByUserId(userId);
        request.setGpsLat(label.getFacility().getGpsLat());
        request.setGpsLon(label.getFacility().getGpsLon());
        request.setGpsAccuracy(5f);
        request.setWeightKg(BigDecimal.valueOf(1.25));
        request.setDeviceId("device-1");

        when(bagLabelRepository.findByQrCode(label.getQrCode())).thenReturn(Optional.of(label));
        when(bagEventRepository.existsByBagLabelIdAndEventType(label.getId(), "CBWTF_VERIFICATION")).thenReturn(false);
        when(bagEventRepository.existsByBagLabelIdAndEventType(label.getId(), "HCF_COLLECTION")).thenReturn(false);

        BagEventService.VerifyResult result = bagEventService.verifyBag(request);

        assertEquals(409, result.getHttpStatus());
        assertEquals("NOT_COLLECTED", result.getResponse().getStatus());
    }

    @Test
    void syncTreatsDuplicateCollectionAsIdempotentSuccess() {
        BagEventSyncItem item = new BagEventSyncItem();
        item.setQrCode(label.getQrCode());
        item.setEventType("HCF_COLLECTION");
        item.setEventTs(Instant.now());
        item.setGpsLat(28.6140);
        item.setGpsLon(77.2091);
        item.setWeightKg(BigDecimal.valueOf(2.0));

        BagEventSyncRequest request = new BagEventSyncRequest();
        request.setEvents(List.of(item));

        when(bagLabelRepository.findByQrCode(label.getQrCode())).thenReturn(Optional.of(label));
        when(bagEventRepository.existsByBagLabelIdAndEventTypeAndEventTs(any(), any(), any())).thenReturn(false);
        when(bagEventRepository.existsByBagLabelIdAndEventType(label.getId(), "HCF_COLLECTION")).thenReturn(true);

        var response = bagEventService.sync(request);

        assertEquals(1, response.getAcks().size());
        assertEquals("SUCCESS", response.getAcks().get(0).getStatus());
        assertTrue(response.getAcks().get(0).getMessage().contains("already collected"));
    }

    @Test
    void syncMarksQrAsUsedOnCollection() {
        BagEventSyncItem item = new BagEventSyncItem();
        item.setQrCode(label.getQrCode());
        item.setEventType("HCF_COLLECTION");
        item.setEventTs(Instant.now());
        item.setGpsLat(28.6140);
        item.setGpsLon(77.2091);
        item.setGpsAccuracyM(8.25);
        item.setWeightKg(BigDecimal.valueOf(2.0));

        BagEventSyncRequest request = new BagEventSyncRequest();
        request.setEvents(List.of(item));

        QrAuthorization qr = new QrAuthorization();
        qr.setStatusEnum(QrAuthorization.Status.ACTIVE);

        when(bagLabelRepository.findByQrCode(label.getQrCode())).thenReturn(Optional.of(label));
        when(qrAuthorizationRepository.findFirstByQrPayloadAndFacilityId(label.getQrCode(), label.getFacility().getId()))
                .thenReturn(Optional.of(qr));
        when(bagEventRepository.existsByBagLabelIdAndEventTypeAndEventTs(any(), any(), any())).thenReturn(false);
        when(bagEventRepository.existsByBagLabelIdAndEventType(label.getId(), "HCF_COLLECTION")).thenReturn(false);
        when(bagEventRepository.save(any(BagEvent.class))).thenAnswer(invocation -> {
            BagEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        var response = bagEventService.sync(request);

        assertEquals("SUCCESS", response.getAcks().get(0).getStatus());
        assertEquals(QrAuthorization.Status.USED.name(), qr.getStatus());
        assertTrue(qr.getPickupEventId() != null);
        var eventCaptor = forClass(BagEvent.class);
        verify(bagEventRepository).save(eventCaptor.capture());
        assertEquals(8.25, eventCaptor.getValue().getGpsAccuracyM());
        verify(qrAuthorizationRepository).save(qr);
    }

    @Test
    void verifyBagRejectsWhenQrAlreadyVerified() {
        BagVerifyRequest request = new BagVerifyRequest();
        request.setQrCode(label.getQrCode());
        request.setVerifiedByUserId(userId);
        request.setGpsLat(label.getFacility().getGpsLat());
        request.setGpsLon(label.getFacility().getGpsLon());
        request.setGpsAccuracy(5f);
        request.setWeightKg(BigDecimal.valueOf(1.25));
        request.setDeviceId("device-1");

        QrAuthorization qr = new QrAuthorization();
        qr.setStatusEnum(QrAuthorization.Status.VERIFIED);

        when(bagLabelRepository.findByQrCode(label.getQrCode())).thenReturn(Optional.of(label));
        when(qrAuthorizationRepository.findFirstByQrPayloadAndFacilityId(label.getQrCode(), label.getFacility().getId()))
                .thenReturn(Optional.of(qr));
        when(bagEventRepository.existsByBagLabelIdAndEventType(label.getId(), "CBWTF_VERIFICATION")).thenReturn(false);
        when(bagEventRepository.existsByBagLabelIdAndEventType(label.getId(), "HCF_COLLECTION")).thenReturn(true);

        BagEventService.VerifyResult result = bagEventService.verifyBag(request);

        assertEquals(409, result.getHttpStatus());
        assertEquals("ALREADY_VERIFIED", result.getResponse().getStatus());
    }

    private Facility otherFacility() {
        Facility facility = new Facility();
        facility.setId(UUID.randomUUID());
        facility.setGpsLat(28.6139);
        facility.setGpsLon(77.2090);
        facility.setGeofenceRadiusM(500);
        return facility;
    }
}
