package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.dto.*;
import com.smartcbwtf.exception.AgreementBlockedException;
import com.smartcbwtf.exception.AgreementNotActiveException;
import com.smartcbwtf.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CONTRACTUAL GUARANTEE TESTS
 * 
 * These tests verify the non-negotiable agreement guard enforcement.
 * ALL operations must be blocked when agreement is not ACTIVE.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AgreementGuardContractTest {

    @Autowired
    private LabelService labelService;
    @Autowired
    private BagEventService bagEventService;
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private AgreementTransitionService transitionService;
    @Autowired
    private AgreementGuardService guardService;

    @Autowired
    private FacilityRepository facilityRepo;
    @Autowired
    private HcfRepository hcfRepo;
    @Autowired
    private AgreementRepository agreementRepo;
    @Autowired
    private BagLabelRepository labelRepo;

    private Facility testFacility;
    private Hcf testHcf;
    private Agreement testAgreement;
    private UUID authenticatedUserId;

    @BeforeEach
    void setupTestData() {
        // Create test facility (CBWTF) with all required fields
        testFacility = new Facility();
        testFacility.setName("Test CBWTF");
        testFacility.setCode("TEST001");
        testFacility.setAddress("123 Test Street, Test City");
        testFacility.setSubscriptionStatus("ACTIVE");
        testFacility.setGpsLat(28.6139);
        testFacility.setGpsLon(77.2090);
        testFacility.setGeofenceRadiusM(500);
        testFacility = facilityRepo.save(testFacility);

        // Create test HCF with all required fields
        testHcf = new Hcf();
        testHcf.setName("Test Hospital");
        testHcf.setCode("HCF001");
        testHcf.setAddress("456 Hospital Road, Delhi");
        testHcf.setStatus("ACTIVE"); // Required field
        testHcf.setNumberOfBeds(100);
        testHcf.setGpsLat(28.6200);
        testHcf.setGpsLon(77.2100);
        testHcf = hcfRepo.save(testHcf);

        // Create ACTIVE agreement
        testAgreement = new Agreement();
        testAgreement.setAgreementNumber("AGR-TEST-00001");
        testAgreement.setHcf(testHcf);
        testAgreement.setFacility(testFacility);
        testAgreement.setStatusEnum(Agreement.Status.ACTIVE);
        testAgreement.setDuesStatusEnum(Agreement.DuesStatus.CLEAR);
        testAgreement.setStartDate(LocalDate.now().minusMonths(1));
        testAgreement.setEndDate(LocalDate.now().plusMonths(11));
        testAgreement.setPerBedPerDayRate(BigDecimal.valueOf(50));
        testAgreement = agreementRepo.save(testAgreement);

        authenticatedUserId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(
                authenticatedUserId,
                testFacility.getId(),
                testHcf.getId(),
                "DRIVER",
                "agreement-guard-test"));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    // ====== CONTRACT TEST 1: QR Generation for EXPIRED Agreement ======

    @Test
    @Order(1)
    @DisplayName("CONTRACT: Generate QR for EXPIRED agreement → 409")
    void generateQrForExpiredAgreement_shouldReturn409() {
        // GIVEN: Agreement is EXPIRED
        testAgreement.setStatusEnum(Agreement.Status.EXPIRED);
        agreementRepo.save(testAgreement);

        // WHEN: Attempt to issue labels
        LabelIssueRequest request = new LabelIssueRequest();
        request.setHcfId(testHcf.getId());
        request.setFacilityId(testFacility.getId());
        request.setCategory("YELLOW");
        request.setQuantity(10);

        // THEN: Operation must be blocked (either AgreementNotActiveException or
        // IllegalStateException)
        Exception thrown = assertThrows(RuntimeException.class, () -> {
            labelService.issue(request);
        }, "QR generation MUST be blocked for EXPIRED agreement");

        // Verify it's one of the expected blocking exceptions
        assertTrue(
                thrown instanceof AgreementNotActiveException ||
                        thrown instanceof IllegalStateException,
                "Expected AgreementNotActiveException or IllegalStateException, got: " + thrown.getClass().getName());
    }

    // ====== CONTRACT TEST 2: Bag Sync for TERMINATED Agreement ======

    @Test
    @Order(2)
    @DisplayName("CONTRACT: Sync bag for TERMINATED agreement → 409")
    void syncBagForTerminatedAgreement_shouldReturn409() {
        // GIVEN: An issued label and TERMINATED agreement
        BagLabel label = createIssuedLabel();
        testAgreement.setStatusEnum(Agreement.Status.TERMINATED);
        testAgreement.setTerminationReason("Contract violation");
        testAgreement.setTerminatedAt(Instant.now());
        agreementRepo.save(testAgreement);

        // WHEN: Attempt to sync bag event
        BagEventSyncItem item = new BagEventSyncItem();
        item.setQrCode(label.getQrCode());
        item.setEventType("HCF_COLLECTION");
        item.setEventTs(Instant.now());
        item.setGpsLat(28.6200);
        item.setGpsLon(77.2100);
        item.setWeightKg(BigDecimal.valueOf(2.5));

        BagEventSyncRequest request = new BagEventSyncRequest();
        request.setEvents(List.of(item));

        // THEN: Operation must return AGREEMENT_BLOCKED status
        BagEventSyncResponse response = bagEventService.sync(request);
        assertEquals("AGREEMENT_BLOCKED", response.getAcks().get(0).getStatus(),
                "Bag sync MUST be blocked for TERMINATED agreement");
    }

    // ====== CONTRACT TEST 3: Bag Verification for DISPUTED Agreement ======

    @Test
    @Order(3)
    @DisplayName("CONTRACT: Verify bag for DISPUTED agreement → 409")
    void verifyBagForDisputedAgreement_shouldReturn409() {
        // GIVEN: An issued label and DISPUTED agreement
        BagLabel label = createIssuedLabel();
        testAgreement.setStatusEnum(Agreement.Status.DISPUTED);
        agreementRepo.save(testAgreement);

        // WHEN: Attempt to verify bag
        BagVerifyRequest request = new BagVerifyRequest();
        request.setQrCode(label.getQrCode());
        request.setEventTs(Instant.now());
        request.setGpsLat(testFacility.getGpsLat());
        request.setGpsLon(testFacility.getGpsLon());
        request.setWeightKg(BigDecimal.valueOf(2.5));

        // THEN: Operation must return 409 with AGREEMENT_NOT_ACTIVE
        BagEventService.VerifyResult result = bagEventService.verifyBag(request);
        assertEquals(409, result.getHttpStatus(),
                "Bag verification MUST return 409 for DISPUTED agreement");
        assertEquals("AGREEMENT_NOT_ACTIVE", result.getResponse().getStatus(),
                "Error status MUST be AGREEMENT_NOT_ACTIVE");
    }

    // ====== CONTRACT TEST 4: Invoice Generation for non-ACTIVE ======

    @Test
    @Order(4)
    @DisplayName("CONTRACT: Generate invoice for EXPIRED agreement → 409")
    void generateInvoiceForNonActiveAgreement_shouldReturn409() {
        // GIVEN: Agreement is EXPIRED
        testAgreement.setStatusEnum(Agreement.Status.EXPIRED);
        agreementRepo.save(testAgreement);

        // WHEN: Attempt to generate invoice
        InvoiceGenerateRequest request = new InvoiceGenerateRequest();
        request.setHcfId(testHcf.getId());
        request.setPeriodStart(LocalDate.now().minusMonths(1));
        request.setPeriodEnd(LocalDate.now());

        // THEN: Operation must be blocked (either AgreementNotActiveException or
        // IllegalStateException)
        Exception thrown = assertThrows(RuntimeException.class, () -> {
            invoiceService.generate(request);
        }, "Invoice generation MUST be blocked for non-ACTIVE agreement");

        // Verify it's one of the expected blocking exceptions
        assertTrue(
                thrown instanceof AgreementNotActiveException ||
                        thrown instanceof IllegalStateException,
                "Expected AgreementNotActiveException or IllegalStateException, got: " + thrown.getClass().getName());
    }

    // ====== CONTRACT TEST 5: ACTIVE → EXPIRED → QR Scan Blocked ======

    @Test
    @Order(5)
    @DisplayName("CONTRACT: ACTIVE → EXPIRED transition blocks subsequent QR scans")
    @Disabled("Requires PostgreSQL - H2 cannot handle jsonb audit log column")
    void activeToExpiredTransition_blocksSubsequentScans() {
        // GIVEN: Agreement is ACTIVE, label is issued
        assertTrue(testAgreement.isActive(), "Agreement should start ACTIVE");
        BagLabel label = createIssuedLabel();

        // WHEN: Agreement transitions to EXPIRED
        Agreement expired = transitionService.expire(testAgreement.getId(), null);
        assertEquals(Agreement.Status.EXPIRED, expired.getStatusEnum(),
                "Agreement should be EXPIRED after transition");

        // THEN: Subsequent QR scan must be blocked
        BagVerifyRequest request = new BagVerifyRequest();
        request.setQrCode(label.getQrCode());
        request.setEventTs(Instant.now());
        request.setGpsLat(testFacility.getGpsLat());
        request.setGpsLon(testFacility.getGpsLon());
        request.setWeightKg(BigDecimal.valueOf(2.5));

        BagEventService.VerifyResult result = bagEventService.verifyBag(request);
        assertEquals(409, result.getHttpStatus(),
                "QR scan MUST be blocked after ACTIVE → EXPIRED transition");
    }

    // ====== ADDITIONAL GUARD INVARIANT TESTS ======

    @Test
    @Order(6)
    @DisplayName("Guard: assertAgreementActive throws for TERMINATED")
    void assertAgreementActive_throwsForTerminated() {
        testAgreement.setStatusEnum(Agreement.Status.TERMINATED);
        agreementRepo.save(testAgreement);

        assertThrows(AgreementNotActiveException.class, () -> {
            guardService.assertAgreementActive(testAgreement.getId());
        });
    }

    @Test
    @Order(7)
    @DisplayName("Guard: assertAgreementActive passes for ACTIVE")
    void assertAgreementActive_passesForActive() {
        // Should not throw
        assertDoesNotThrow(() -> {
            guardService.assertAgreementActive(testAgreement.getId());
        });
    }

    @Test
    @Order(8)
    @DisplayName("Guard: isAgreementActive returns false for DISPUTED")
    void isAgreementActive_returnsFalseForDisputed() {
        testAgreement.setStatusEnum(Agreement.Status.DISPUTED);
        agreementRepo.save(testAgreement);

        assertFalse(guardService.isAgreementActive(testAgreement.getId()),
                "isAgreementActive MUST return false for DISPUTED");
    }

    // ====== HELPER METHODS ======

    private BagLabel createIssuedLabel() {
        BagLabel label = new BagLabel();
        label.setHcf(testHcf);
        label.setFacility(testFacility);
        label.setCategory("YELLOW");
        label.setSerialNo(String.format("%08d", System.currentTimeMillis() % 100000000));
        label.setQrCode("CBWTF|" + testHcf.getCode() + "|YELLOW|" + label.getSerialNo());
        label.setStatus("ISSUED");
        return labelRepo.save(label);
    }
}
