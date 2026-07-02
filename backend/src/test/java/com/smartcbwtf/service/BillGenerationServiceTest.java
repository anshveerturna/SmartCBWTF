package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Bill;
import com.smartcbwtf.domain.BillingSnapshot;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.BillRepository;
import com.smartcbwtf.repository.BillingSnapshotRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillGenerationServiceTest {

    @Mock
    JdbcTemplate jdbcTemplate;
    @Mock
    AgreementRepository agreementRepository;
    @Mock
    BillingSnapshotRepository snapshotRepository;
    @Mock
    BillRepository billRepository;
    @Mock
    InvoiceRepository invoiceRepository;
    @Mock
    FacilityRepository facilityRepository;
    @Mock
    BillingCalculationService calculationService;
    @Mock
    AuditLogService auditLogService;

    BillGenerationService service;

    @BeforeEach
    void setUp() {
        service = new BillGenerationService(
                jdbcTemplate,
                agreementRepository,
                snapshotRepository,
                billRepository,
                invoiceRepository,
                facilityRepository,
                calculationService,
                auditLogService);
    }

    @Test
    void generateBillsAggregatesHcfCollectionEventsFromBagEventSource() {
        UUID facilityId = UUID.randomUUID();
        UUID agreementId = UUID.randomUUID();
        LocalDate month = LocalDate.of(2026, 6, 1);
        Agreement agreement = agreement(agreementId, facilityId, 7);
        Facility facility = agreement.getFacility();

        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(agreementRepository.findActiveByFacilityId(facilityId)).thenReturn(List.of(agreement));
        when(billRepository.existsByAgreementIdAndBillingMonth(agreementId, month)).thenReturn(false);
        stubBillingConfig(agreementId);

        BigDecimal pickupWeight = new BigDecimal("400.500");
        when(jdbcTemplate.queryForObject(
                argThat((String sql) -> containsSql(sql, "SUM(e.weight_kg)") && usesBagEventAgreementScope(sql)),
                eq(BigDecimal.class),
                eq(agreementId),
                eq(LocalDateTime.of(2026, 6, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 7, 1, 0, 0))))
                .thenReturn(pickupWeight);
        when(jdbcTemplate.queryForObject(
                argThat((String sql) -> containsSql(sql, "COUNT(e.id)") && usesBagEventAgreementScope(sql)),
                eq(Integer.class),
                eq(agreementId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(3);
        when(jdbcTemplate.queryForList(
                argThat((String sql) -> containsSql(sql, "SELECT e.id") && usesBagEventAgreementScope(sql)
                        && containsSql(sql, "ORDER BY e.id")),
                eq(UUID.class),
                eq(agreementId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(List.of(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        UUID.fromString("00000000-0000-0000-0000-000000000002")));

        BillingCalculationService.BillCalculation calculation = new BillingCalculationService.BillCalculation(
                pickupWeight,
                new BigDecimal("83.100"),
                new BigDecimal("317.400"),
                new BigDecimal("4650.00"),
                new BigDecimal("15870.00"),
                new BigDecimal("20520.00"),
                new BigDecimal("513.00"),
                new BigDecimal("513.00"),
                new BigDecimal("21546.00"));
        when(calculationService.calculate(
                eq(10),
                eq(30),
                eq(new BigDecimal("277")),
                eq(new BigDecimal("15.50")),
                eq(new BigDecimal("50.00")),
                eq(pickupWeight),
                eq(5.0)))
                .thenReturn(calculation);

        int generated = service.generateBillsForMonth(facilityId, month, UUID.randomUUID());

        assertEquals(1, generated);

        ArgumentCaptor<BillingSnapshot> snapshotCaptor = ArgumentCaptor.forClass(BillingSnapshot.class);
        verify(snapshotRepository).save(snapshotCaptor.capture());
        assertEquals(7, snapshotCaptor.getValue().getAgreementVersion());

        ArgumentCaptor<Bill> billCaptor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository).save(billCaptor.capture());
        Bill bill = billCaptor.getValue();
        assertEquals(pickupWeight, bill.getPickupWeightKg());
        assertEquals(3, bill.getPickupEventCount());
        assertNotNull(bill.getPickupEventHash());
        assertNotEquals(sha256("empty"), bill.getPickupEventHash());
    }

    @Test
    void pickupAggregationFailureDoesNotCreateSilentZeroBill() {
        UUID facilityId = UUID.randomUUID();
        UUID agreementId = UUID.randomUUID();
        LocalDate month = LocalDate.of(2026, 6, 1);
        Agreement agreement = agreement(agreementId, facilityId, 1);

        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(agreement.getFacility()));
        when(agreementRepository.findActiveByFacilityId(facilityId)).thenReturn(List.of(agreement));
        when(billRepository.existsByAgreementIdAndBillingMonth(agreementId, month)).thenReturn(false);
        stubBillingConfig(agreementId);
        when(jdbcTemplate.queryForObject(
                argThat((String sql) -> containsSql(sql, "SUM(e.weight_kg)") && usesBagEventAgreementScope(sql)),
                eq(BigDecimal.class),
                eq(agreementId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenThrow(new DataAccessResourceFailureException("bag_event unavailable"));

        int generated = service.generateBillsForMonth(facilityId, month, UUID.randomUUID());

        assertEquals(0, generated);
        verify(snapshotRepository, never()).save(any(BillingSnapshot.class));
        verify(billRepository, never()).save(any(Bill.class));
        verify(auditLogService).log(eq("BILLING"), eq(agreementId), eq("BILLING_FAILED"), any(UUID.class),
                eq("bag_event unavailable"));
    }

    private void stubBillingConfig(UUID agreementId) {
        when(jdbcTemplate.queryForObject(
                argThat((String sql) -> containsSql(sql, "base_grams_per_bed_per_day")),
                eq(BigDecimal.class),
                eq(agreementId)))
                .thenReturn(new BigDecimal("277"));
        when(jdbcTemplate.queryForObject(
                argThat((String sql) -> containsSql(sql, "base_rate_per_bed_per_day")),
                eq(BigDecimal.class),
                eq(agreementId)))
                .thenReturn(new BigDecimal("15.50"));
    }

    private static Agreement agreement(UUID agreementId, UUID facilityId, int version) {
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setExcessRatePerKg(new BigDecimal("50.00"));
        facility.setExcessRateEffectiveFrom(LocalDate.of(2020, 1, 1));

        Hcf hcf = new Hcf();
        hcf.setId(UUID.randomUUID());
        hcf.setNumberOfBeds(10);
        hcf.setTaxRate(5.0);

        Agreement agreement = new Agreement();
        agreement.setId(agreementId);
        agreement.setFacility(facility);
        agreement.setHcf(hcf);
        agreement.setStatus(Agreement.Status.ACTIVE.name());
        agreement.setVersion(version);
        return agreement;
    }

    private static boolean usesBagEventAgreementScope(String sql) {
        return containsSql(sql, "FROM bag_event e")
                && containsSql(sql, "JOIN agreement a")
                && containsSql(sql, "a.hcf_id = e.hcf_id")
                && containsSql(sql, "a.facility_id = e.facility_id")
                && containsSql(sql, "a.id = ?")
                && containsSql(sql, "e.event_type = 'HCF_COLLECTION'")
                && !containsSql(sql, "pickup_event");
    }

    private static boolean containsSql(String sql, String fragment) {
        return sql != null && sql.contains(fragment);
    }

    private static String sha256(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
