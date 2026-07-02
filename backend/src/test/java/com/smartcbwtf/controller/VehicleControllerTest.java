package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.GpsEvent;
import com.smartcbwtf.domain.Vehicle;
import com.smartcbwtf.service.VehicleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VehicleControllerTest {

    private final VehicleService vehicleService = mock(VehicleService.class);
    private final VehicleController controller = new VehicleController(vehicleService);
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void vehicleLocationResponsesAreNotCacheable() {
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = vehicle(vehicleId);
        GpsEvent event = GpsEvent.create(vehicle, new BigDecimal("28.6139000"), new BigDecimal("77.2090000"),
                BigDecimal.ZERO, Instant.parse("2026-07-01T00:00:00Z"), "VENDOR");
        when(vehicleService.getVehicle(facilityId, vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleService.getLastLocation(facilityId, vehicleId)).thenReturn(Optional.of(event));
        when(vehicleService.getGpsTrail(facilityId, vehicleId, 50)).thenReturn(List.of(event));

        var lastLocationResponse = controller.getLastLocation(vehicleId);
        var trailResponse = controller.getGpsTrail(vehicleId, 50);

        assertEquals("no-store", lastLocationResponse.getHeaders().getCacheControl());
        assertEquals("no-cache", lastLocationResponse.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertEquals("no-store", trailResponse.getHeaders().getCacheControl());
        assertEquals("no-cache", trailResponse.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    @Test
    void liveMapIsNotCacheable() {
        when(vehicleService.getLiveMap(facilityId)).thenReturn(List.of());

        var response = controller.getLiveMap();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    private static Vehicle vehicle(UUID id) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setRegistrationNumber("DL01AB1234");
        vehicle.setVehicleType("TRUCK");
        vehicle.setStatus("ACTIVE");
        return vehicle;
    }
}
