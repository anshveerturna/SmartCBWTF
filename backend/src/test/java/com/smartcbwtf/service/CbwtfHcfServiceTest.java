package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.AgreementBillingConfig;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.ApprovalStatus;
import com.smartcbwtf.domain.BillingModel;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.HcfType;
import com.smartcbwtf.dto.CbwtfAdminHcfRegistrationRequest;
import com.smartcbwtf.dto.HcfDetailDTO;
import com.smartcbwtf.dto.HcfListItemDTO;
import com.smartcbwtf.dto.HcfUpdateRequest;
import com.smartcbwtf.repository.AgreementBillingConfigRepository;
import com.smartcbwtf.repository.AgreementCorrectionRequestRepository;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.AttendanceRepository;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.FacilitySettingsRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CbwtfHcfServiceTest {

    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private AgreementBillingConfigRepository billingConfigRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private AgreementValidationService agreementValidationService;
    @Mock
    private AgreementNumberGeneratorService agreementNumberGenerator;
    @Mock
    private HCFIdentityService hcfIdentityService;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BagEventRepository bagEventRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private FacilitySettingsRepository facilitySettingsRepository;
    @Mock
    private AgreementCorrectionRequestRepository correctionRequestRepository;
    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;

    @InjectMocks
    private CbwtfHcfService service;

    @Test
    void listByFacilityIncludesLastPickupTimestamp() {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        Instant lastPickupAt = Instant.parse("2026-07-01T08:30:00Z");

        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        hcf.setCode("HCF-001");
        hcf.setName("City Clinic");
        hcf.setStatus("ACTIVE");

        Facility facility = new Facility();
        facility.setId(facilityId);

        Agreement agreement = new Agreement();
        agreement.setId(UUID.randomUUID());
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setAgreementNumber("AGR-001");
        agreement.setStatus(Agreement.Status.ACTIVE.name());
        agreement.setStartDate(LocalDate.now().minusDays(1));
        agreement.setEndDate(LocalDate.now().plusDays(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(agreementRepository.findLatestAgreementsByFacilityId(eq(facilityId), pageable.capture()))
                .thenReturn(List.of(agreement));
        when(bagEventRepository.findLastPickupTimesByHcfIds(List.of(hcfId)))
                .thenReturn(List.of(lastPickupRow(hcfId, lastPickupAt)));

        List<HcfListItemDTO> result = service.listByFacility(facilityId, 5000);

        assertEquals(lastPickupAt, result.get(0).getLastPickupAt());
        assertEquals(1000, pageable.getValue().getPageSize());
        verify(bagEventRepository).findLastPickupTimesByHcfIds(List.of(hcfId));
        verify(bagEventRepository, never()).findLastPickupTimeByHcfId(hcfId);
        verify(agreementRepository, never()).findLatestAgreementsByFacilityId(facilityId);
    }

    @Test
    void listPendingUsesBoundedLatestQueueQuery() {
        UUID facilityId = UUID.randomUUID();
        Agreement agreement = agreementForQueue(facilityId, "PENDING_APPROVAL", "PENDING_APPROVAL");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(agreementRepository.findLatestPendingOrResubmittableAgreementsByFacilityId(eq(facilityId),
                pageable.capture())).thenReturn(List.of(agreement));

        List<HcfListItemDTO> result = service.listPending(facilityId, 5000);

        assertEquals(1, result.size());
        assertEquals(250, pageable.getValue().getPageSize());
        verify(agreementRepository, never()).findLatestAgreementsByFacilityId(facilityId);
    }

    @Test
    void listDraftsUsesBoundedLatestDraftQuery() {
        UUID facilityId = UUID.randomUUID();
        Agreement agreement = agreementForQueue(facilityId, "DRAFT", "DRAFT");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(agreementRepository.findLatestDraftAgreementsByFacilityId(eq(facilityId), pageable.capture()))
                .thenReturn(List.of(agreement));

        List<HcfListItemDTO> result = service.listDrafts(facilityId, 5000);

        assertEquals(1, result.size());
        assertEquals(250, pageable.getValue().getPageSize());
        verify(agreementRepository, never()).findLatestAgreementsByFacilityId(facilityId);
    }

    @Test
    void getHcfDetailIncludesAttendanceSummary() {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        UUID agreementId = UUID.randomUUID();
        Instant lastPickupAt = Instant.parse("2026-07-01T08:30:00Z");
        Instant lastAttendanceAt = Instant.parse("2026-07-01T07:45:00Z");

        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        hcf.setCode("HCF-001");
        hcf.setName("City Clinic");
        hcf.setAddress("Main Road");
        hcf.setStatus("ACTIVE");

        Facility facility = new Facility();
        facility.setId(facilityId);

        Agreement agreement = new Agreement();
        agreement.setId(agreementId);
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setAgreementNumber("AGR-001");
        agreement.setStatus(Agreement.Status.ACTIVE.name());
        agreement.setStartDate(LocalDate.now().minusDays(1));
        agreement.setEndDate(LocalDate.now().plusDays(1));

        when(agreementRepository.findLatestByHcfIdAndFacilityId(eq(hcfId), eq(facilityId), any(Pageable.class)))
                .thenReturn(List.of(agreement));
        when(billingConfigRepository.findActiveByAgreementId(agreementId)).thenReturn(Optional.empty());
        when(bagEventRepository.countPickupDaysByHcfId(hcfId)).thenReturn(4);
        when(bagEventRepository.sumTotalWasteByHcfId(hcfId)).thenReturn(new BigDecimal("125.750"));
        when(bagEventRepository.findLastPickupTimeByHcfId(hcfId)).thenReturn(lastPickupAt);
        when(attendanceRepository.countByFacilityIdAndHcfId(facilityId, hcfId)).thenReturn(9L);
        when(attendanceRepository.findLastAttendanceTimeByFacilityIdAndHcfId(facilityId, hcfId))
                .thenReturn(lastAttendanceAt);

        HcfDetailDTO detail = service.getHcfDetail(hcfId, facilityId);

        assertEquals(4, detail.getSummary().getTotalPickups());
        assertEquals(new BigDecimal("125.750"), detail.getSummary().getTotalWasteKg());
        assertEquals(lastPickupAt, detail.getSummary().getLastPickupAt());
        assertEquals(9, detail.getSummary().getTotalAttendanceMarks());
        assertEquals(lastAttendanceAt, detail.getSummary().getLastAttendanceAt());
        verify(agreementRepository).findLatestByHcfIdAndFacilityId(eq(hcfId), eq(facilityId), any(Pageable.class));
        verify(agreementRepository, never()).findAllByHcfIdAndFacilityId(hcfId, facilityId);
    }

    @Test
    void resetPortalAdminPasswordEnforcesSharedPasswordPolicyBeforeSaving() {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        Agreement agreement = agreementForQueue(facilityId, Agreement.Status.ACTIVE.name(), "ACTIVE");
        agreement.getHcf().setId(hcfId);
        agreement.getHcf().setApprovalStatus(ApprovalStatus.APPROVED);
        agreement.getHcf().setPortalAccessManuallyEnabled(true);
        AppUser admin = new AppUser();
        admin.setId(UUID.randomUUID());
        admin.setUsername("AGR-001");

        when(agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)).thenReturn(List.of(agreement));
        when(userRepository.findByHcfIdAndRole(hcfId, "HCF_ADMIN")).thenReturn(List.of(admin));
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Password policy violation: too weak"))
                .when(passwordPolicyValidator).validateOrThrow("weakpass");

        assertThrows(IllegalArgumentException.class,
                () -> service.resetPortalAdminPassword(hcfId, facilityId, "weakpass"));

        verify(passwordPolicyValidator).validateOrThrow("weakpass");
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePendingBillingModel_updatesOnlyFacilityLinkedHcf() {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();

        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        hcf.setCode("HCF-001");
        hcf.setName("City Clinic");
        hcf.setAddress("Main Road");
        hcf.setStatus("PENDING_APPROVAL");
        hcf.setApprovalStatus(ApprovalStatus.PENDING);
        hcf.setCreatedAt(Instant.now());

        Facility facility = new Facility();
        facility.setId(facilityId);

        Agreement agreement = new Agreement();
        agreement.setId(UUID.randomUUID());
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setAgreementNumber("AGR-001");
        agreement.setStatus(Agreement.Status.PENDING_APPROVAL.name());

        HcfUpdateRequest request = new HcfUpdateRequest(
                null, null, null, null, null, null, null, null, null, null,
                BillingModel.BEDDED, 25, null, null);

        when(agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)).thenReturn(List.of(agreement));
        when(hcfRepository.save(any(Hcf.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HcfListItemDTO result = service.updatePendingBillingModel(hcfId, facilityId, request);

        assertEquals(hcfId, result.getId());
        assertEquals(BillingModel.BEDDED, hcf.getBillingModel());
        assertEquals(25, hcf.getNumberOfBeds());
        verify(hcfRepository).save(hcf);
        verify(auditLogService).log(eq("HCF"), eq(hcfId), eq("HCF_BILLING_MODEL_UPDATED"), any(), any());
    }

    @Test
    void updatePendingBillingModel_rejectsHcfOutsideFacility() {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        HcfUpdateRequest request = new HcfUpdateRequest(
                null, null, null, null, null, null, null, null, null, null,
                BillingModel.BEDDED, 25, null, null);

        when(agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.updatePendingBillingModel(hcfId, facilityId, request));
        verifyNoInteractions(hcfRepository);
    }

    @Test
    void registerHcfDirectly_promotesExistingDraftInsteadOfCreatingDuplicate() {
        UUID facilityId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        UUID agreementId = UUID.randomUUID();

        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setCode("GUT");
        facility.setName("Gurgaon Unit");

        Hcf draftHcf = new Hcf();
        draftHcf.setId(hcfId);
        draftHcf.setCode("HCF-DRAFT-001");
        draftHcf.setStatus("DRAFT");
        draftHcf.setCreatedAt(Instant.now());

        Agreement draftAgreement = new Agreement();
        draftAgreement.setId(agreementId);
        draftAgreement.setHcf(draftHcf);
        draftAgreement.setFacility(facility);
        draftAgreement.setStatus("DRAFT");
        draftAgreement.setAgreementNumber("DRAFT-12345678");
        draftAgreement.setStartDate(LocalDate.now());
        draftAgreement.setEndDate(LocalDate.now().plusMonths(1));
        draftAgreement.setPerBedPerDayRate(BigDecimal.ZERO);

        AgreementBillingConfig billingConfig = new AgreementBillingConfig();
        billingConfig.setAgreement(draftAgreement);
        billingConfig.setBaseGramsPerBedPerDay(277);
        billingConfig.setBaseRatePerBedPerDay(BigDecimal.ZERO);
        billingConfig.setEffectiveFrom(LocalDate.now());

        CbwtfAdminHcfRegistrationRequest request = new CbwtfAdminHcfRegistrationRequest();
        request.setId(hcfId);
        request.setName("Updated Draft Hospital");
        request.setAddress("Sector 10");
        request.setPincode("122001");
        request.setState("Haryana");
        request.setCity("Gurgaon");
        request.setDoctorName("Dr. Sharma");
        request.setContactPhone("9999999999");
        request.setContactEmail("draft@example.com");
        request.setOwnershipType("OWNED");
        request.setBedded(true);
        request.setNumberOfBeds(42);
        request.setMonthlyCharges(new BigDecimal("25000"));
        request.setGpsLat(28.4595);
        request.setGpsLon(77.0266);
        request.setAgreementStartDate(LocalDate.of(2026, 4, 1));
        request.setAgreementEndDate(LocalDate.of(2027, 3, 31));
        request.setPerBedPerDayRate(new BigDecimal("15"));
        request.setTaxRate(5.0);
        request.setExcessRatePerKg(new BigDecimal("120"));
        request.setHcfType(HcfType.HOSPITAL.name());
        request.setCustomAgreementNumber("GUT-HCF-2026-00999");

        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(hcfIdentityService.computeFingerprint(any(), any(), any(), any(), any()))
                .thenReturn("identity-hash-1234567890");
        when(hcfRepository.findById(hcfId)).thenReturn(Optional.of(draftHcf));
        when(hcfRepository.save(any(Hcf.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId))
                .thenReturn(List.of(draftAgreement));
        when(agreementRepository.findLatestByHcfIdAndFacilityId(eq(hcfId), eq(facilityId), any(Pageable.class)))
                .thenReturn(List.of(draftAgreement));
        when(agreementRepository.findByAgreementNumber("GUT-HCF-2026-00999")).thenReturn(Optional.empty());
        when(agreementRepository.save(any(Agreement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(billingConfigRepository.findActiveByAgreementId(agreementId))
                .thenReturn(Optional.of(billingConfig), Optional.of(billingConfig));
        when(billingConfigRepository.save(any(AgreementBillingConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(bagEventRepository.countPickupDaysByHcfId(hcfId)).thenReturn(0);
        when(bagEventRepository.sumTotalWasteByHcfId(hcfId)).thenReturn(BigDecimal.ZERO);
        when(bagEventRepository.findLastPickupTimeByHcfId(hcfId)).thenReturn(null);
        doNothing().when(agreementValidationService).assertCanCreateAgreement(hcfId);

        HcfDetailDTO detail = service.registerHcfDirectly(facilityId, adminUserId, request);

        assertEquals(hcfId, detail.getId());
        assertEquals("Updated Draft Hospital", detail.getName());
        assertEquals("PENDING_APPROVAL", detail.getHcfStatus());
        assertEquals("GUT-HCF-2026-00999", detail.getAgreement().getAgreementNumber());
        assertEquals("PENDING_APPROVAL", detail.getAgreement().getStatus());

        ArgumentCaptor<Hcf> hcfCaptor = ArgumentCaptor.forClass(Hcf.class);
        verify(hcfRepository).save(hcfCaptor.capture());
        assertSame(draftHcf, hcfCaptor.getValue());

        ArgumentCaptor<Agreement> agreementCaptor = ArgumentCaptor.forClass(Agreement.class);
        verify(agreementRepository).save(agreementCaptor.capture());
        assertSame(draftAgreement, agreementCaptor.getValue());
        assertEquals("GUT-HCF-2026-00999", agreementCaptor.getValue().getAgreementNumber());

        verify(agreementValidationService).assertCanCreateAgreement(eq(hcfId));
    }

    @Test
    void registerHcfDirectlyRejectsUnsafeRentAgreementUrlBeforePersistence() {
        UUID facilityId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        CbwtfAdminHcfRegistrationRequest request = new CbwtfAdminHcfRegistrationRequest();
        request.setOwnershipType("RENTED");
        request.setRentAgreementUrl("/uploads/rent-agreements/../secret.pdf");

        assertThrows(IllegalArgumentException.class,
                () -> service.registerHcfDirectly(facilityId, adminUserId, request));

        verifyNoInteractions(facilityRepository, hcfRepository, agreementRepository, billingConfigRepository);
    }

    @Test
    void registerHcfDirectlyRejectsRentAgreementUrlOwnedByDifferentFacility() {
        UUID facilityId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        CbwtfAdminHcfRegistrationRequest request = new CbwtfAdminHcfRegistrationRequest();
        request.setOwnershipType("RENTED");
        request.setRentAgreementUrl("/uploads/rent-agreements/" + UUID.randomUUID() + "_abc12345.pdf");

        assertThrows(IllegalArgumentException.class,
                () -> service.registerHcfDirectly(facilityId, adminUserId, request));

        verifyNoInteractions(facilityRepository, hcfRepository, agreementRepository, billingConfigRepository);
    }

    private static BagEventRepository.HcfLastPickup lastPickupRow(UUID hcfId, Instant lastPickupAt) {
        return new BagEventRepository.HcfLastPickup() {
            @Override
            public UUID getHcfId() {
                return hcfId;
            }

            @Override
            public Instant getLastPickupAt() {
                return lastPickupAt;
            }
        };
    }

    private static Agreement agreementForQueue(UUID facilityId, String hcfStatus, String agreementStatus) {
        Facility facility = new Facility();
        facility.setId(facilityId);

        Hcf hcf = new Hcf();
        hcf.setId(UUID.randomUUID());
        hcf.setCode("HCF-001");
        hcf.setName("City Clinic");
        hcf.setStatus(hcfStatus);

        Agreement agreement = new Agreement();
        agreement.setId(UUID.randomUUID());
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setAgreementNumber("AGR-001");
        agreement.setStatus(agreementStatus);
        return agreement;
    }
}
