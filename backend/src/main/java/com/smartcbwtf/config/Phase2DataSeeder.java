package com.smartcbwtf.config;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.AgreementNumberGeneratorService;
import com.smartcbwtf.service.HCFIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Phase 2 Data Seeder - Seeds realistic test data for ONE CBWTF only.
 * 
 * CRITICAL REQUIREMENTS:
 * - Resolves facility_id from test_cbwtf user (NEVER hardcoded)
 * - All data is tenant-scoped to this facility
 * - Idempotent (safe to run multiple times)
 * - Only runs in dev/test profiles
 * 
 * SEEDED DATA:
 * - 4 HCFs with ACTIVE agreements
 * - 1 HCF with agreement expiring in 15 days (MEDIUM alert)
 * - Bag events for today and past 7 days (various categories)
 * - 1 PENDING invoice (triggers HIGH alert)
 * - 1 PAID invoice (historical)
 */
@Component
@Profile("dev")
@Order(200) // Run after DataInitializer
public class Phase2DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Phase2DataSeeder.class);
    private static final String TARGET_USERNAME = "test_cbwtf";
    private static final String SEED_MARKER_PREFIX = "SEED_P2_";

    private final AppUserRepository userRepo;
    private final FacilityRepository facilityRepo;
    private final HcfRepository hcfRepo;
    private final AgreementRepository agreementRepo;
    private final BagLabelRepository bagLabelRepo;
    private final BagEventRepository bagEventRepo;
    private final InvoiceRepository invoiceRepo;
    private final AgreementNumberGeneratorService agreementNumberGenerator;
    private final HCFIdentityService hcfIdentityService;

    // Seed tracking
    private int hcfCount = 0;
    private int agreementCount = 0;
    private int bagLabelCount = 0;
    private int bagEventCount = 0;
    private int invoiceCount = 0;
    private final List<String> agreementCodes = new ArrayList<>();

    public Phase2DataSeeder(
            AppUserRepository userRepo,
            FacilityRepository facilityRepo,
            HcfRepository hcfRepo,
            AgreementRepository agreementRepo,
            BagLabelRepository bagLabelRepo,
            BagEventRepository bagEventRepo,
            InvoiceRepository invoiceRepo,
            AgreementNumberGeneratorService agreementNumberGenerator,
            HCFIdentityService hcfIdentityService) {
        this.userRepo = userRepo;
        this.facilityRepo = facilityRepo;
        this.hcfRepo = hcfRepo;
        this.agreementRepo = agreementRepo;
        this.bagLabelRepo = bagLabelRepo;
        this.bagEventRepo = bagEventRepo;
        this.invoiceRepo = invoiceRepo;
        this.agreementNumberGenerator = agreementNumberGenerator;
        this.hcfIdentityService = hcfIdentityService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("============================================================");
        log.info("PHASE 2 DATA SEEDER - Starting");
        log.info("============================================================");

        // Step 1: Resolve facility from test_cbwtf user
        AppUser cbwtfAdmin = userRepo.findByUsername(TARGET_USERNAME).orElse(null);
        if (cbwtfAdmin == null) {
            log.error("❌ ABORT: User '{}' not found. Cannot seed data.", TARGET_USERNAME);
            return;
        }

        if (!"CBWTF_ADMIN".equals(cbwtfAdmin.getRole())) {
            log.error("❌ ABORT: User '{}' has role '{}', expected 'CBWTF_ADMIN'.",
                    TARGET_USERNAME, cbwtfAdmin.getRole());
            return;
        }

        Facility facility = cbwtfAdmin.getFacility();
        if (facility == null) {
            log.error("❌ ABORT: User '{}' has no facility assigned.", TARGET_USERNAME);
            return;
        }

        UUID facilityId = facility.getId();
        log.info("✅ Resolved facility: {} (ID: {})", facility.getName(), facilityId);

        // Step 2: Check if already seeded (idempotency)
        long existingSeededHcfs = hcfRepo.count();
        if (hcfRepo.findByCode(SEED_MARKER_PREFIX + "HCF_001").isPresent()) {
            log.info("⏭️ Data already seeded. Skipping to avoid duplicates.");
            log.info("============================================================");
            return;
        }

        // Step 3: Seed HCFs and Agreements
        seedHcfsAndAgreements(facility);

        // Step 4: Seed Bag Labels and Events
        seedBagLabelsAndEvents(facility, cbwtfAdmin.getId());

        // Step 5: Seed Invoices
        seedInvoices(facility);

        // Step 6: Adjust subscription for CRITICAL alert testing
        adjustSubscriptionForAlert(facility);

        // Final summary
        log.info("============================================================");
        log.info("PHASE 2 DATA SEEDER - Complete");
        log.info("============================================================");
        log.info("Summary for facility: {} ({})", facility.getName(), facilityId);
        log.info("  HCFs created:        {}", hcfCount);
        log.info("  Agreements created:  {}", agreementCount);
        log.info("  Bag labels created:  {}", bagLabelCount);
        log.info("  Bag events created:  {}", bagEventCount);
        log.info("  Invoices created:    {}", invoiceCount);
        log.info("  Agreement codes:     {}", agreementCodes);
        log.info("============================================================");
    }

    private void seedHcfsAndAgreements(Facility facility) {
        log.info("Seeding HCFs and Agreements...");

        // Create 4 regular HCFs with ACTIVE agreements
        String[] hcfNames = {
                "City General Hospital",
                "Apollo Health Clinic",
                "Metro Diagnostics Lab",
                "Green Valley Medical Center"
        };

        for (int i = 0; i < hcfNames.length; i++) {
            Hcf hcf = createHcf(facility, i + 1, hcfNames[i], false);
            createActiveAgreement(facility, hcf, LocalDate.now().plusYears(1));
        }

        // Create 1 HCF with agreement expiring in 15 days (MEDIUM alert)
        Hcf expiringHcf = createHcf(facility, 5, "Sunrise Nursing Home", false);
        createActiveAgreement(facility, expiringHcf, LocalDate.now().plusDays(15));
        log.info("  ⚠️ Created agreement expiring in 15 days for MEDIUM alert");
    }

    private Hcf createHcf(Facility facility, int index, String name, boolean historical) {
        String code = SEED_MARKER_PREFIX + "HCF_" + String.format("%03d", index);

        Hcf hcf = new Hcf();
        hcf.setCode(code);
        hcf.setName(name);
        hcf.setAddress("Test Address " + index + ", City");
        hcf.setContactEmail("contact" + index + "@test.com");
        hcf.setContactPhone("900000000" + index);
        hcf.setNumberOfBeds(20 + (index * 10));
        hcf.setGpsLat(28.6139 + (index * 0.01));
        hcf.setGpsLon(77.2090 + (index * 0.01));
        hcf.setStatus("ACTIVE");
        hcf.setPanNo("AAAAA" + index + "111A");
        hcf.setGstNo("07AAAAA" + index + "111A1Z5");
        hcf.setCreatedAt(Instant.now());
        hcf.setUpdatedAt(Instant.now());

        // Compute identity hash
        String identityHash = hcfIdentityService.computeFingerprint(
                name, hcf.getGstNo(), hcf.getPanNo(), hcf.getGpsLat(), hcf.getGpsLon());
        hcf.setIdentityHash(identityHash);

        hcf = hcfRepo.save(hcf);
        hcfCount++;
        log.info("  Created HCF: {} ({})", name, code);
        return hcf;
    }

    private void createActiveAgreement(Facility facility, Hcf hcf, LocalDate endDate) {
        Agreement agreement = new Agreement();
        agreement.setHcf(hcf);
        agreement.setFacility(facility);
        agreement.setAgreementNumber(agreementNumberGenerator.generateNextAgreementNumber(facility));
        agreement.setStatus("ACTIVE");
        agreement.setDuesStatus("CLEAR");
        agreement.setStartDate(LocalDate.now().minusMonths(6));
        agreement.setEndDate(endDate);
        agreement.setPerBedPerDayRate(new BigDecimal("15.50"));
        agreement.setTermsAccepted(true);
        agreement.setCreatedAt(Instant.now());
        agreement.setUpdatedAt(Instant.now());

        agreement = agreementRepo.save(agreement);
        agreementCount++;
        agreementCodes.add(agreement.getAgreementNumber());
        log.info("  Created Agreement: {} for HCF {} (expires: {})",
                agreement.getAgreementNumber(), hcf.getName(), endDate);
    }

    private void seedBagLabelsAndEvents(Facility facility, UUID collectorUserId) {
        log.info("Seeding Bag Labels and Events...");

        // Get all HCFs linked to this facility via agreements
        List<Agreement> activeAgreements = agreementRepo.findActiveByFacilityId(facility.getId());
        if (activeAgreements.isEmpty()) {
            log.warn("  No active agreements found. Skipping bag events.");
            return;
        }

        String[] categories = { "YELLOW", "RED", "BLUE", "WHITE" };
        BigDecimal[] weights = {
                new BigDecimal("2.5"), new BigDecimal("1.8"),
                new BigDecimal("0.5"), new BigDecimal("0.3")
        };
        Random random = new Random(42); // Deterministic for reproducibility

        // Create bag events for the past 7 days
        for (int daysAgo = 0; daysAgo <= 7; daysAgo++) {
            Instant eventTime = Instant.now()
                    .minus(daysAgo, ChronoUnit.DAYS)
                    .minus(random.nextInt(6), ChronoUnit.HOURS);

            // More events for today, fewer for older days
            int eventsForDay = daysAgo == 0 ? 15 : (8 - daysAgo);

            for (int i = 0; i < eventsForDay; i++) {
                // Pick a random HCF from active agreements
                Agreement agreement = activeAgreements.get(random.nextInt(activeAgreements.size()));
                Hcf hcf = agreement.getHcf();

                // Pick category with realistic distribution (more yellow, less white)
                int catIndex = random.nextInt(100) < 45 ? 0 : // 45% Yellow
                        random.nextInt(100) < 70 ? 1 : // 25% Red
                                random.nextInt(100) < 90 ? 2 : 3; // 20% Blue, 10% White
                String category = categories[catIndex];
                BigDecimal weight = weights[catIndex].add(new BigDecimal(random.nextDouble() * 0.5));

                // Create bag label
                BagLabel label = createBagLabel(facility, hcf, category, daysAgo, i);

                // Create bag event
                createBagEvent(facility, hcf, label, eventTime, weight, category, collectorUserId);
            }
        }
    }

    private BagLabel createBagLabel(Facility facility, Hcf hcf, String category, int day, int idx) {
        String qrCode = String.format("QR_%s_%s_D%d_%d_%d",
                SEED_MARKER_PREFIX, category, day, idx, System.nanoTime() % 10000);
        String serialNo = String.format("SN-%s-%d-%d", category.substring(0, 1), day, idx);

        BagLabel label = new BagLabel();
        label.setHcf(hcf);
        label.setFacility(facility);
        label.setCategory(category);
        label.setSerialNo(serialNo);
        label.setQrCode(qrCode);
        label.setStatus("USED");
        label.setIssuedAt(Instant.now().minus(7, ChronoUnit.DAYS));
        label.setUsedAt(Instant.now().minus(day, ChronoUnit.DAYS));

        label = bagLabelRepo.save(label);
        bagLabelCount++;
        return label;
    }

    private void createBagEvent(Facility facility, Hcf hcf, BagLabel label,
            Instant eventTime, BigDecimal weight, String category, UUID collectorUserId) {
        BagEvent event = new BagEvent();
        event.setBagLabel(label);
        event.setFacility(facility);
        event.setHcf(hcf);
        event.setEventType("CBWTF_VERIFICATION");
        event.setEventTs(eventTime);
        event.setGpsLat(hcf.getGpsLat());
        event.setGpsLon(hcf.getGpsLon());
        event.setWeightKg(weight);
        event.setCollectedByUserId(collectorUserId); // Use actual user ID
        event.setAppDeviceId("SEED_DEVICE_001");
        event.setAnomalyState("OK");
        event.setCreatedAt(Instant.now());

        bagEventRepo.save(event);
        bagEventCount++;
    }

    private void seedInvoices(Facility facility) {
        log.info("Seeding Invoices...");

        List<Agreement> agreements = agreementRepo.findActiveByFacilityId(facility.getId());
        if (agreements.isEmpty()) {
            log.warn("  No agreements found. Skipping invoices.");
            return;
        }

        Agreement firstAgreement = agreements.get(0);
        Hcf hcf = firstAgreement.getHcf();

        // Create 1 PENDING invoice (triggers HIGH alert - invoice overdue)
        createInvoice(facility, firstAgreement, hcf, "PENDING",
                LocalDate.now().minusDays(45), LocalDate.now().minusDays(15),
                new BigDecimal("15000.00"));
        log.info("  ⚠️ Created PENDING invoice (45 days old) for HIGH alert");

        // Create 1 PAID invoice (historical)
        if (agreements.size() > 1) {
            Agreement secondAgreement = agreements.get(1);
            createInvoice(facility, secondAgreement, secondAgreement.getHcf(), "PAID",
                    LocalDate.now().minusDays(90), LocalDate.now().minusDays(60),
                    new BigDecimal("12500.00"));
        }
    }

    private void createInvoice(Facility facility, Agreement agreement, Hcf hcf,
            String status, LocalDate periodStart, LocalDate periodEnd, BigDecimal amount) {
        // Use a deterministic seed-based suffix for invoice number idempotency
        String suffix = status + "_" + periodEnd.toEpochDay();
        String invoiceNumber = "INV-" + SEED_MARKER_PREFIX + hcf.getCode().substring(hcf.getCode().length() - 3) + "-"
                + suffix;

        if (invoiceRepo.findByInvoiceNumber(invoiceNumber).isPresent()) {
            log.info("  ⏭️ Invoice {} already exists. Skipping.", invoiceNumber);
            return;
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setHcf(hcf);
        invoice.setFacility(facility);
        invoice.setAgreement(agreement);
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setBeds(hcf.getNumberOfBeds());
        invoice.setPerBedPerDayRate(agreement.getPerBedPerDayRate());
        invoice.setBaseAmount(amount);
        invoice.setTaxAmount(amount.multiply(new BigDecimal("0.18")));
        invoice.setTotalAmount(amount.multiply(new BigDecimal("1.18")));
        invoice.setStatus(status);
        invoice.setCreatedAt(Instant.now().minus(45, ChronoUnit.DAYS));
        invoice.setUpdatedAt(Instant.now());

        invoiceRepo.save(invoice);
        invoiceCount++;
        log.info("  Created Invoice: {} (Status: {}, Amount: ₹{})",
                invoiceNumber, status, invoice.getTotalAmount());
    }

    private void adjustSubscriptionForAlert(Facility facility) {
        // For CRITICAL alert testing, we could set subscription to expire in 5 days
        // But the current test_cbwtf has 360 days left, which is fine for normal
        // testing
        // Uncomment below to test CRITICAL alert:

        // facility.setSubscriptionExpiresAt(Instant.now().plus(5, ChronoUnit.DAYS));
        // facilityRepo.save(facility);
        // log.info(" ⚠️ Set subscription to expire in 5 days for CRITICAL alert");

        log.info("  Subscription expiry unchanged (test CRITICAL alert manually if needed)");
    }
}
