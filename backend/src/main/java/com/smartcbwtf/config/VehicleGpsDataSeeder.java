package com.smartcbwtf.config;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Seeds Vehicle, GPS events, and Ingestion Health data for test_cbwtf ONLY.
 * This runs on startup in dev mode.
 * 
 * Creates:
 * - 3 Vehicles with GPS devices
 * - 24-hour GPS event history
 * - GpsIngestionLog entries with HEALTHY/DEGRADED/DOWN scenarios
 * - 2 vendor integrations (WHEELSEYE, GENERIC)
 */
@Component
@Order(4)
public class VehicleGpsDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VehicleGpsDataSeeder.class);
    private static final String TARGET_USERNAME = "test_cbwtf";
    private static final Random RANDOM = new Random(42); // Fixed seed for reproducibility

    private final AppUserRepository appUserRepository;
    private final VehicleRepository vehicleRepository;
    private final GpsEventRepository gpsEventRepository;
    private final GpsVendorIntegrationRepository vendorIntegrationRepository;
    private final GpsIngestionLogRepository ingestionLogRepository;

    public VehicleGpsDataSeeder(
            AppUserRepository appUserRepository,
            VehicleRepository vehicleRepository,
            GpsEventRepository gpsEventRepository,
            GpsVendorIntegrationRepository vendorIntegrationRepository,
            GpsIngestionLogRepository ingestionLogRepository) {
        this.appUserRepository = appUserRepository;
        this.vehicleRepository = vehicleRepository;
        this.gpsEventRepository = gpsEventRepository;
        this.vendorIntegrationRepository = vendorIntegrationRepository;
        this.ingestionLogRepository = ingestionLogRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("============================================================");
        log.info("VEHICLE GPS DATA SEEDER - Starting");
        log.info("============================================================");

        Optional<AppUser> userOpt = appUserRepository.findByUsername(TARGET_USERNAME);
        if (userOpt.isEmpty()) {
            log.warn("⏭️ User '{}' not found. Skipping Vehicle/GPS seeding.", TARGET_USERNAME);
            return;
        }

        AppUser user = userOpt.get();
        Facility facility = user.getFacility();
        if (facility == null) {
            log.warn("⏭️ User '{}' has no facility. Skipping.", TARGET_USERNAME);
            return;
        }

        log.info("✅ Resolved facility: {} (ID: {})", facility.getName(), facility.getId());

        // Check if already seeded (check for ingestion logs as new indicator)
        List<GpsIngestionLog> existingLogs = ingestionLogRepository.findByFacilityId(facility.getId());
        if (!existingLogs.isEmpty()) {
            log.info("⏭️ GPS data already seeded ({} ingestion logs found). Skipping.", existingLogs.size());
            return;
        }

        log.info("🚀 Seeding comprehensive GPS data for facility: {}", facility.getName());

        // Seed vendor integrations
        seedVendorIntegrations(facility);

        // Seed or update vehicles
        List<Vehicle> vehicles = seedVehicles(facility);

        // Seed fresh GPS events (last 2 hours for testing)
        seedGpsEvents(vehicles);

        // Seed GPS ingestion health logs with different statuses
        seedIngestionLogs(facility);

        log.info("============================================================");
        log.info("✅ Vehicle/GPS seeding complete:");
        log.info("   - Vehicles: {}", vehicles.size());
        log.info("   - Vendor integrations: 2 (WHEELSEYE, GENERIC)");
        log.info("   - Ingestion logs: 2 (HEALTHY, DEGRADED scenarios)");
        log.info("============================================================");
    }

    private void seedVendorIntegrations(Facility facility) {
        // WHEELSEYE - active and healthy
        GpsVendorIntegration wheelseye = new GpsVendorIntegration();
        wheelseye.setFacility(facility);
        wheelseye.setVendor("WHEELSEYE");
        wheelseye.setIntegrationType("WEBHOOK");
        wheelseye.setAuthType("API_KEY");
        wheelseye.setStatus("ACTIVE");
        // Set credentials as Map for JSONB
        Map<String, Object> wheelseyeCreds = new HashMap<>();
        wheelseyeCreds.put("api_key", "test-wheelseye-key-12345");
        wheelseyeCreds.put("account_id", "CBWTF-TEST-001");
        wheelseye.setCredentials(wheelseyeCreds);
        vendorIntegrationRepository.save(wheelseye);

        // GENERIC - for second integration scenario
        GpsVendorIntegration generic = new GpsVendorIntegration();
        generic.setFacility(facility);
        generic.setVendor("GENERIC");
        generic.setIntegrationType("WEBHOOK");
        generic.setAuthType("NONE");
        generic.setStatus("ACTIVE");
        // Set empty credentials as Map
        Map<String, Object> genericCreds = new HashMap<>();
        genericCreds.put("note", "Generic adapter - no auth required");
        generic.setCredentials(genericCreds);
        vendorIntegrationRepository.save(generic);

        log.info("   Created 2 vendor integrations (WHEELSEYE, GENERIC)");
    }

    private List<Vehicle> seedVehicles(Facility facility) {
        List<Vehicle> existingVehicles = vehicleRepository.findByFacilityId(facility.getId());
        if (!existingVehicles.isEmpty()) {
            log.info("   Using existing {} vehicles", existingVehicles.size());
            return existingVehicles;
        }

        List<Vehicle> vehicles = new ArrayList<>();

        String[][] vehicleData = {
                { "DL-1C-AB-1234", "TRUCK", "IMEI-WE-001", "WHEELSEYE" },
                { "DL-2C-CD-5678", "VAN", "IMEI-WE-002", "WHEELSEYE" },
                { "DL-3C-EF-9012", "AUTO", "IMEI-GN-003", "GENERIC" },
        };

        for (String[] data : vehicleData) {
            Vehicle vehicle = new Vehicle();
            vehicle.setFacility(facility);
            vehicle.setRegistrationNumber(data[0]);
            vehicle.setVehicleType(data[1]);
            vehicle.setGpsDeviceId(data[2]);
            vehicle.setGpsVendor(data[3]);
            vehicle.setStatus("ACTIVE");
            vehicle.setGpsStatus("ONLINE");
            vehicles.add(vehicleRepository.save(vehicle));
        }

        log.info("   Created {} vehicles", vehicles.size());
        return vehicles;
    }

    private void seedGpsEvents(List<Vehicle> vehicles) {
        // Delhi coordinates as center point
        double baseLat = 28.6139;
        double baseLon = 77.2090;

        Instant now = Instant.now();
        int totalEvents = 0;

        for (Vehicle vehicle : vehicles) {
            // Generate GPS events for last 2 hours (for fresh testing)
            double currentLat = baseLat + (RANDOM.nextDouble() * 0.1 - 0.05);
            double currentLon = baseLon + (RANDOM.nextDouble() * 0.1 - 0.05);

            Instant lastEventTime = null;

            // Generate events every 2-3 minutes for last 2 hours
            for (int minutesAgo = 120; minutesAgo >= 0; minutesAgo -= (RANDOM.nextInt(2) + 2)) {
                // Simulate movement
                currentLat += (RANDOM.nextDouble() * 0.002 - 0.001);
                currentLon += (RANDOM.nextDouble() * 0.002 - 0.001);

                // Keep in bounds
                currentLat = Math.max(28.4, Math.min(28.9, currentLat));
                currentLon = Math.max(76.9, Math.min(77.5, currentLon));

                Instant recordedAt = now.minus(minutesAgo, ChronoUnit.MINUTES);

                GpsEvent event = new GpsEvent();
                event.setVehicle(vehicle);
                event.setLatitude(BigDecimal.valueOf(currentLat).setScale(7, java.math.RoundingMode.HALF_UP));
                event.setLongitude(BigDecimal.valueOf(currentLon).setScale(7, java.math.RoundingMode.HALF_UP));
                event.setSpeed(BigDecimal.valueOf(RANDOM.nextDouble() * 60));
                event.setHeading(BigDecimal.valueOf(RANDOM.nextDouble() * 360));
                event.setRecordedAt(recordedAt);
                event.setReceivedAt(recordedAt.plus(1, ChronoUnit.SECONDS));
                event.setSource("VENDOR_API");

                // Set raw payload as Map for JSONB
                Map<String, Object> rawPayload = new HashMap<>();
                rawPayload.put("device_id", vehicle.getGpsDeviceId());
                rawPayload.put("lat", currentLat);
                rawPayload.put("lng", currentLon);
                rawPayload.put("speed", RANDOM.nextDouble() * 60);
                rawPayload.put("timestamp", recordedAt.toString());
                event.setRawPayload(rawPayload);

                gpsEventRepository.save(event);
                totalEvents++;
                lastEventTime = recordedAt;
            }

            // Update vehicle with last known position (very recent - within 5 min)
            if (lastEventTime != null) {
                vehicle.setLastLatitude(BigDecimal.valueOf(currentLat).setScale(7, java.math.RoundingMode.HALF_UP));
                vehicle.setLastLongitude(BigDecimal.valueOf(currentLon).setScale(7, java.math.RoundingMode.HALF_UP));
                vehicle.setLastGpsAt(now.minus(RANDOM.nextInt(5), ChronoUnit.MINUTES)); // Last 0-5 min
                vehicle.setGpsStatus("ONLINE");
                vehicleRepository.save(vehicle);
            }
        }

        log.info("   Created {} GPS events across {} vehicles", totalEvents, vehicles.size());
    }

    /**
     * Creates ingestion logs with different health scenarios:
     * - WHEELSEYE: HEALTHY (recent success, no failures)
     * - GENERIC: DEGRADED (success but some failures)
     */
    private void seedIngestionLogs(Facility facility) {
        Instant now = Instant.now();

        // WHEELSEYE - HEALTHY scenario
        GpsIngestionLog wheelseyeLog = new GpsIngestionLog();
        wheelseyeLog.setFacility(facility);
        wheelseyeLog.setVendor("WHEELSEYE");
        wheelseyeLog.recordSuccess(50); // 50 events ingested
        wheelseyeLog.recordSuccess(45); // Another batch
        wheelseyeLog.recordSuccess(52); // Another batch
        ingestionLogRepository.save(wheelseyeLog);

        // GENERIC - DEGRADED scenario (has some failures)
        GpsIngestionLog genericLog = new GpsIngestionLog();
        genericLog.setFacility(facility);
        genericLog.setVendor("GENERIC");
        genericLog.recordSuccess(20);
        genericLog.recordFailure("Connection timeout");
        genericLog.recordSuccess(15);
        genericLog.recordFailure("Invalid JSON payload");
        genericLog.recordSuccess(25); // Recent success (so not DOWN)
        ingestionLogRepository.save(genericLog);

        log.info("   Created 2 ingestion logs (WHEELSEYE: HEALTHY, GENERIC: DEGRADED)");
    }
}
