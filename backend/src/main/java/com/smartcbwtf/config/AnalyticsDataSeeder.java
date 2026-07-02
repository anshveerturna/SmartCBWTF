package com.smartcbwtf.config;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Seeds realistic analytics test data ONLY for test_cbwtf user.
 * Creates HCFs, ACTIVE agreements, and BagEvents for 12 months.
 * 
 * TENANT ISOLATION: Data is scoped to test_cbwtf facility only.
 * Other CBWTF users will see ZERO of this data.
 */
@Component
@Profile("dev")
@Order(200) // Run after Phase2DataSeeder
public class AnalyticsDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsDataSeeder.class);
    private static final String TARGET_USERNAME = "test_cbwtf";
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final String[] CATEGORIES = { "YELLOW", "RED", "BLUE", "WHITE" };

    private final AppUserRepository appUserRepository;
    private final HcfRepository hcfRepository;
    private final AgreementRepository agreementRepository;
    private final BagLabelRepository bagLabelRepository;
    private final BagEventRepository bagEventRepository;

    public AnalyticsDataSeeder(
            AppUserRepository appUserRepository,
            HcfRepository hcfRepository,
            AgreementRepository agreementRepository,
            BagLabelRepository bagLabelRepository,
            BagEventRepository bagEventRepository) {
        this.appUserRepository = appUserRepository;
        this.hcfRepository = hcfRepository;
        this.agreementRepository = agreementRepository;
        this.bagLabelRepository = bagLabelRepository;
        this.bagEventRepository = bagEventRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("============================================================");
        log.info("ANALYTICS DATA SEEDER - Starting");
        log.info("============================================================");

        // Step 1: Resolve facility from test_cbwtf user
        Optional<AppUser> userOpt = appUserRepository.findByUsername(TARGET_USERNAME);
        if (userOpt.isEmpty()) {
            log.warn("⏭️ User '{}' not found. Skipping analytics seeder.", TARGET_USERNAME);
            return;
        }

        AppUser user = userOpt.get();
        Facility facility = user.getFacility();
        if (facility == null) {
            log.warn("⏭️ User '{}' has no facility. Skipping analytics seeder.", TARGET_USERNAME);
            return;
        }

        UUID facilityId = facility.getId();
        log.info("✅ Resolved facility: {} (ID: {})", facility.getName(), facilityId);

        // Step 2: Check if analytics data already seeded
        long existingHcfCount = hcfRepository.countByNameStartingWith("Analytics-HCF-");

        if (existingHcfCount > 0) {
            log.info("⏭️ Analytics data already seeded ({} HCFs found). Skipping.", existingHcfCount);
            return;
        }

        // Step 3: Get collector user for bag events
        UUID collectorUserId = user.getId();

        log.info("🚀 Seeding analytics data for facility: {}", facility.getName());

        // Step 4: Create HCFs and agreements
        List<Hcf> hcfs = createAnalyticsHcfs(facility);
        createActiveAgreements(facility, hcfs);

        // Step 5: Generate bag events
        int totalEvents = generateBagEvents(facility, hcfs, collectorUserId);

        log.info("✅ Analytics seeding complete:");
        log.info("   - Created {} HCFs with ACTIVE agreements", hcfs.size());
        log.info("   - Generated {} bag events across 12 months", totalEvents);
        log.info("============================================================");
    }

    private List<Hcf> createAnalyticsHcfs(Facility facility) {
        List<Hcf> hcfs = new ArrayList<>();
        String[] hcfNames = {
                "Analytics-HCF-001 City Hospital",
                "Analytics-HCF-002 Metro Clinic",
                "Analytics-HCF-003 Central Lab",
                "Analytics-HCF-004 Regional Medical",
                "Analytics-HCF-005 Community Health"
        };

        for (int i = 0; i < hcfNames.length; i++) {
            Hcf hcf = new Hcf();
            hcf.setName(hcfNames[i]);
            hcf.setCode("AHCF-" + String.format("%03d", i + 1));
            hcf.setAddress(String.format("%d Analytics Street, Test City", 100 + i));
            hcf.setContactPhone("98765432" + String.format("%02d", i));
            hcf.setContactEmail("analytics-hcf" + (i + 1) + "@test.com");
            hcf.setNumberOfBeds(50 + (i * 20));
            hcf.setGpsLat(28.6139 + (i * 0.01));
            hcf.setGpsLon(77.2090 + (i * 0.01));
            hcf.setStatus("ACTIVE");
            hcf.setPanNo("ANALY" + String.format("%04d", i + 1) + "A");
            hcf.setDoctorName("Dr. Analytics " + (i + 1));

            hcfs.add(hcfRepository.save(hcf));
        }

        log.info("   Created {} Analytics HCFs", hcfs.size());
        return hcfs;
    }

    private void createActiveAgreements(Facility facility, List<Hcf> hcfs) {
        LocalDate today = LocalDate.now();

        for (int i = 0; i < hcfs.size(); i++) {
            Hcf hcf = hcfs.get(i);

            Agreement agreement = new Agreement();
            agreement.setAgreementNumber("ANALYTICS-AGR-" + String.format("%03d", i + 1));
            agreement.setFacility(facility);
            agreement.setHcf(hcf);
            agreement.setStatus("ACTIVE");
            agreement.setDuesStatus("CLEAR");
            agreement.setStartDate(today.minusMonths(12));
            agreement.setEndDate(today.plusMonths(12));
            agreement.setPerBedPerDayRate(BigDecimal.valueOf(2.50 + (i * 0.25)));

            agreementRepository.save(agreement);
        }

        log.info("   Created {} ACTIVE agreements", hcfs.size());
    }

    private int generateBagEvents(Facility facility, List<Hcf> hcfs, UUID collectorUserId) {
        LocalDate today = LocalDate.now();
        int totalEvents = 0;
        Random random = new Random(42); // Fixed seed for reproducibility

        // Generate events for past 12 months
        for (int monthsAgo = 0; monthsAgo <= 11; monthsAgo++) {
            LocalDate monthStart = today.minusMonths(monthsAgo).withDayOfMonth(1);
            int daysInMonth = monthStart.lengthOfMonth();

            // More events for recent months
            int eventsPerMonth = (12 - monthsAgo) * 15 + 30;

            for (int e = 0; e < eventsPerMonth; e++) {
                Hcf hcf = hcfs.get(random.nextInt(hcfs.size()));
                String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
                int dayOfMonth = 1 + random.nextInt(daysInMonth);
                LocalDate eventDate = monthStart.withDayOfMonth(dayOfMonth);

                // Generate weight based on category
                double weight = switch (category) {
                    case "YELLOW" -> 1.5 + random.nextDouble() * 4.0; // 1.5-5.5 kg
                    case "RED" -> 0.5 + random.nextDouble() * 2.0; // 0.5-2.5 kg
                    case "BLUE" -> 0.3 + random.nextDouble() * 1.5; // 0.3-1.8 kg
                    case "WHITE" -> 0.2 + random.nextDouble() * 1.0; // 0.2-1.2 kg
                    default -> 1.0;
                };

                // Create BagLabel
                BagLabel bagLabel = new BagLabel();
                bagLabel.setHcf(hcf);
                bagLabel.setFacility(facility);
                bagLabel.setCategory(category);
                bagLabel.setSerialNo("ALAB-" + System.nanoTime() + "-" + random.nextInt(10000));
                bagLabel.setQrCode("AQR-" + UUID.randomUUID().toString().substring(0, 8));
                bagLabel.setStatus("USED");
                bagLabel.setIssuedAt(eventDate.atStartOfDay(ZONE).toInstant());
                bagLabel.setUsedAt(
                        eventDate.atTime(10 + random.nextInt(8), random.nextInt(60)).atZone(ZONE).toInstant());
                bagLabel = bagLabelRepository.save(bagLabel);

                // Create BagEvent
                BagEvent bagEvent = new BagEvent();
                bagEvent.setBagLabel(bagLabel);
                bagEvent.setFacility(facility);
                bagEvent.setHcf(hcf);
                bagEvent.setEventType("HCF_COLLECTION");
                bagEvent.setEventTs(bagLabel.getUsedAt());
                bagEvent.setGpsLat(hcf.getGpsLat() + (random.nextDouble() - 0.5) * 0.001);
                bagEvent.setGpsLon(hcf.getGpsLon() + (random.nextDouble() - 0.5) * 0.001);
                bagEvent.setWeightKg(BigDecimal.valueOf(weight).setScale(3, java.math.RoundingMode.HALF_UP));
                bagEvent.setCollectedByUserId(collectorUserId);
                bagEvent.setAppDeviceId("ANALYTICS-SEEDER");
                bagEvent.setAnomalyState("OK");
                bagEvent.setNotes("Analytics test data");

                bagEventRepository.save(bagEvent);
                totalEvents++;
            }
        }

        return totalEvents;
    }
}
