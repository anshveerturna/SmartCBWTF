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
 * Seeds Vehicle and GPS data for test_cbwtf ONLY.
 * This runs on startup in dev mode.
 */
@Component
@Order(4)
public class VehicleGpsDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VehicleGpsDataSeeder.class);
    private static final String TARGET_USERNAME = "test_cbwtf";
    private static final Random RANDOM = new Random(42); // Fixed seed for reproducibility

    private final AppUserRepository appUserRepository;
    private final FacilityRepository facilityRepository;
    private final VehicleRepository vehicleRepository;
    private final GpsEventRepository gpsEventRepository;
    private final GpsVendorIntegrationRepository vendorIntegrationRepository;

    public VehicleGpsDataSeeder(
            AppUserRepository appUserRepository,
            FacilityRepository facilityRepository,
            VehicleRepository vehicleRepository,
            GpsEventRepository gpsEventRepository,
            GpsVendorIntegrationRepository vendorIntegrationRepository) {
        this.appUserRepository = appUserRepository;
        this.facilityRepository = facilityRepository;
        this.vehicleRepository = vehicleRepository;
        this.gpsEventRepository = gpsEventRepository;
        this.vendorIntegrationRepository = vendorIntegrationRepository;
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

        // Check if already seeded
        List<Vehicle> existingVehicles = vehicleRepository.findByFacilityId(facility.getId());
        if (!existingVehicles.isEmpty()) {
            log.info("⏭️ Vehicle data already seeded ({} vehicles found). Skipping.", existingVehicles.size());
            return;
        }

        log.info("🚀 Seeding vehicle and GPS data for facility: {}", facility.getName());

        // Seed vendor integration
        seedVendorIntegration(facility);

        // Seed vehicles
        List<Vehicle> vehicles = seedVehicles(facility);

        // Seed GPS events
        seedGpsEvents(vehicles);

        log.info("============================================================");
        log.info("✅ Vehicle/GPS seeding complete:");
        log.info("   - Created {} vehicles", vehicles.size());
        log.info("============================================================");
    }

    private void seedVendorIntegration(Facility facility) {
        GpsVendorIntegration integration = new GpsVendorIntegration();
        integration.setFacility(facility);
        integration.setVendor("WHEELSEYE");
        integration.setIntegrationType("WEBHOOK");
        integration.setAuthType("API_KEY");
        integration.setStatus("ACTIVE");
        vendorIntegrationRepository.save(integration);
        log.info("   Created Wheelseye vendor integration");
    }

    private List<Vehicle> seedVehicles(Facility facility) {
        List<Vehicle> vehicles = new ArrayList<>();

        String[][] vehicleData = {
                { "DL-1C-AB-1234", "TRUCK", "IMEI-WE-001" },
                { "DL-2C-CD-5678", "VAN", "IMEI-WE-002" },
                { "DL-3C-EF-9012", "AUTO", "IMEI-WE-003" },
        };

        for (String[] data : vehicleData) {
            Vehicle vehicle = new Vehicle();
            vehicle.setFacility(facility);
            vehicle.setRegistrationNumber(data[0]);
            vehicle.setVehicleType(data[1]);
            vehicle.setGpsDeviceId(data[2]);
            vehicle.setGpsVendor("WHEELSEYE");
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
            // Generate GPS events for last 24 hours
            double currentLat = baseLat + (RANDOM.nextDouble() * 0.1 - 0.05);
            double currentLon = baseLon + (RANDOM.nextDouble() * 0.1 - 0.05);

            Instant lastEventTime = null;

            // Generate events every 2-5 minutes for last 24 hours
            for (int hoursAgo = 24; hoursAgo >= 0; hoursAgo--) {
                for (int minutesInHour = 0; minutesInHour < 60; minutesInHour += RANDOM.nextInt(3) + 2) {
                    // Simulate movement
                    currentLat += (RANDOM.nextDouble() * 0.002 - 0.001);
                    currentLon += (RANDOM.nextDouble() * 0.002 - 0.001);

                    // Keep in bounds
                    currentLat = Math.max(28.4, Math.min(28.9, currentLat));
                    currentLon = Math.max(76.9, Math.min(77.5, currentLon));

                    Instant recordedAt = now.minus(hoursAgo, ChronoUnit.HOURS)
                            .minus(60 - minutesInHour, ChronoUnit.MINUTES);

                    GpsEvent event = new GpsEvent();
                    event.setVehicle(vehicle);
                    event.setLatitude(BigDecimal.valueOf(currentLat).setScale(7, java.math.RoundingMode.HALF_UP));
                    event.setLongitude(BigDecimal.valueOf(currentLon).setScale(7, java.math.RoundingMode.HALF_UP));
                    event.setSpeed(BigDecimal.valueOf(RANDOM.nextDouble() * 60));
                    event.setHeading(BigDecimal.valueOf(RANDOM.nextDouble() * 360));
                    event.setRecordedAt(recordedAt);
                    event.setReceivedAt(recordedAt.plus(1, ChronoUnit.SECONDS));
                    event.setSource("VENDOR_API");

                    gpsEventRepository.save(event);
                    totalEvents++;
                    lastEventTime = recordedAt;
                }
            }

            // Update vehicle with last known position
            if (lastEventTime != null) {
                vehicle.setLastLatitude(BigDecimal.valueOf(currentLat).setScale(7, java.math.RoundingMode.HALF_UP));
                vehicle.setLastLongitude(BigDecimal.valueOf(currentLon).setScale(7, java.math.RoundingMode.HALF_UP));
                vehicle.setLastGpsAt(lastEventTime);
                vehicleRepository.save(vehicle);
            }
        }

        log.info("   Created {} GPS events across {} vehicles", totalEvents, vehicles.size());
    }
}
