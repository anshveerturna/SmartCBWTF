package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TENANT ISOLATION CONTRACT TESTS
 * 
 * These tests verify that multi-tenant data isolation is enforced.
 * User A cannot see User B's data.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TenantIsolationContractTest {

    @Autowired
    private TenantAssertionService tenantAssertion;
    @Autowired
    private CBWTFScopedQueryService scopedQuery;

    @Autowired
    private FacilityRepository facilityRepo;
    @Autowired
    private HcfRepository hcfRepo;
    @Autowired
    private AgreementRepository agreementRepo;

    private Facility facilityA;
    private Facility facilityB;
    private Hcf hcfUnderA;
    private Hcf hcfUnderB;
    private Agreement agreementA;
    private Agreement agreementB;

    @BeforeEach
    void setupTestData() {
        // Create Facility A (Tenant A)
        facilityA = new Facility();
        facilityA.setName("Facility A");
        facilityA.setCode("CBWTF-A");
        facilityA.setAddress("Address A");
        facilityA.setSubscriptionStatus("ACTIVE");
        facilityA.setGpsLat(28.6);
        facilityA.setGpsLon(77.2);
        facilityA = facilityRepo.save(facilityA);

        // Create Facility B (Tenant B)
        facilityB = new Facility();
        facilityB.setName("Facility B");
        facilityB.setCode("CBWTF-B");
        facilityB.setAddress("Address B");
        facilityB.setSubscriptionStatus("ACTIVE");
        facilityB.setGpsLat(19.1);
        facilityB.setGpsLon(72.8);
        facilityB = facilityRepo.save(facilityB);

        // Create HCF under Facility A
        hcfUnderA = new Hcf();
        hcfUnderA.setName("Hospital A");
        hcfUnderA.setCode("HCF-A");
        hcfUnderA.setAddress("Hospital A Address");
        hcfUnderA.setStatus("ACTIVE");
        hcfUnderA.setGpsLat(28.61);
        hcfUnderA.setGpsLon(77.21);
        hcfUnderA = hcfRepo.save(hcfUnderA);

        // Create HCF under Facility B
        hcfUnderB = new Hcf();
        hcfUnderB.setName("Hospital B");
        hcfUnderB.setCode("HCF-B");
        hcfUnderB.setAddress("Hospital B Address");
        hcfUnderB.setStatus("ACTIVE");
        hcfUnderB.setGpsLat(19.11);
        hcfUnderB.setGpsLon(72.81);
        hcfUnderB = hcfRepo.save(hcfUnderB);

        // Create Agreement A (linking HCF-A to Facility A)
        agreementA = new Agreement();
        agreementA.setAgreementNumber("AGR-A-001");
        agreementA.setHcf(hcfUnderA);
        agreementA.setFacility(facilityA);
        agreementA.setStatusEnum(Agreement.Status.ACTIVE);
        agreementA.setDuesStatusEnum(Agreement.DuesStatus.CLEAR);
        agreementA.setStartDate(LocalDate.now().minusMonths(1));
        agreementA.setPerBedPerDayRate(BigDecimal.valueOf(50));
        agreementA = agreementRepo.save(agreementA);

        // Create Agreement B (linking HCF-B to Facility B)
        agreementB = new Agreement();
        agreementB.setAgreementNumber("AGR-B-001");
        agreementB.setHcf(hcfUnderB);
        agreementB.setFacility(facilityB);
        agreementB.setStatusEnum(Agreement.Status.ACTIVE);
        agreementB.setDuesStatusEnum(Agreement.DuesStatus.CLEAR);
        agreementB.setStartDate(LocalDate.now().minusMonths(1));
        agreementB.setPerBedPerDayRate(BigDecimal.valueOf(60));
        agreementB = agreementRepo.save(agreementB);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    // ====== CONTRACT TEST 1: Tenant A cannot access Tenant B's facility ======

    @Test
    @Order(1)
    @DisplayName("CONTRACT: Tenant A cannot access Facility B")
    void tenantA_cannotAccessFacilityB() {
        // Set context as Tenant A
        setTenantContext(facilityA.getId(), "CBWTF_ADMIN");

        // Try to access Facility B
        assertThrows(TenantAssertionService.TenantAccessDeniedException.class, () -> {
            tenantAssertion.assertCanAccessFacility(facilityB.getId());
        }, "Tenant A MUST NOT be able to access Facility B");
    }

    // ====== CONTRACT TEST 2: Tenant A CAN access own facility ======

    @Test
    @Order(2)
    @DisplayName("CONTRACT: Tenant A CAN access own facility")
    void tenantA_canAccessOwnFacility() {
        // Set context as Tenant A
        setTenantContext(facilityA.getId(), "CBWTF_ADMIN");

        // Should not throw
        assertDoesNotThrow(() -> {
            tenantAssertion.assertCanAccessFacility(facilityA.getId());
        }, "Tenant A should be able to access own facility");
    }

    // ====== CONTRACT TEST 3: SuperAdmin can access any facility ======

    @Test
    @Order(3)
    @DisplayName("CONTRACT: SuperAdmin can access any facility")
    void superAdmin_canAccessAnyFacility() {
        // Set context as SuperAdmin (no tenantId)
        setTenantContext(null, "SUPER_ADMIN");

        // Should not throw for either facility
        assertDoesNotThrow(() -> {
            tenantAssertion.assertCanAccessFacility(facilityA.getId());
            tenantAssertion.assertCanAccessFacility(facilityB.getId());
        }, "SuperAdmin should be able to access any facility");
    }

    // ====== CONTRACT TEST 4: Scoped query only returns tenant's data ======

    @Test
    @Order(4)
    @DisplayName("CONTRACT: Scoped query only returns tenant's HCFs")
    void scopedQuery_onlyReturnsTenantHcfs() {
        // Set context as Tenant A
        setTenantContext(facilityA.getId(), "CBWTF_ADMIN");

        // Get HCFs via scoped query
        List<Hcf> hcfs = scopedQuery.getActiveHcfs();

        // Should only contain HCF-A, not HCF-B
        assertEquals(1, hcfs.size(), "Should only return tenant's HCFs");
        assertEquals(hcfUnderA.getId(), hcfs.get(0).getId(),
                "Returned HCF should be HCF-A");
    }

    // ====== CONTRACT TEST 5: Missing tenant context throws ======

    @Test
    @Order(5)
    @DisplayName("CONTRACT: Missing tenant context throws exception")
    void missingTenantContext_throwsException() {
        // Clear any context
        TenantContext.clear();

        // Non-SuperAdmin without tenant context should fail
        assertThrows(TenantAssertionService.TenantAccessDeniedException.class, () -> {
            tenantAssertion.requireTenantContext();
        }, "Missing tenant context MUST throw exception");
    }

    // ====== CONTRACT TEST 6: getRequiredTenantId returns correct ID ======

    @Test
    @Order(6)
    @DisplayName("CONTRACT: getRequiredTenantId returns correct tenant ID")
    void getRequiredTenantId_returnsCorrectId() {
        setTenantContext(facilityA.getId(), "CBWTF_ADMIN");

        UUID tenantId = tenantAssertion.getRequiredTenantId();
        assertEquals(facilityA.getId(), tenantId,
                "Should return the correct tenant ID");
    }

    // ====== HELPER METHODS ======

    private void setTenantContext(UUID tenantId, String role) {
        TenantContext.set(new TenantContext.TenantInfo(
                UUID.randomUUID(), // userId
                tenantId, // tenantId (facility_id)
                null, // hcfId
                role, // role
                "testuser" // username
        ));
    }
}
