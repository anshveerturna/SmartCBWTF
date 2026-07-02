package com.smartcbwtf.controller;

import com.smartcbwtf.service.GpsIngestionHealthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GpsHealthControllerTest {

    private final GpsIngestionHealthService healthService = mock(GpsIngestionHealthService.class);
    private final GpsHealthController controller = new GpsHealthController(healthService);

    @Test
    void allHealthResponseIsNotCacheable() {
        when(healthService.getAllHealth()).thenReturn(List.of());

        var response = controller.getAllHealth();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    @Test
    void facilityHealthResponseIsNotCacheable() {
        UUID facilityId = UUID.randomUUID();
        when(healthService.getHealthByFacility(facilityId)).thenReturn(List.of());

        var response = controller.getHealthByFacility(facilityId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }
}
