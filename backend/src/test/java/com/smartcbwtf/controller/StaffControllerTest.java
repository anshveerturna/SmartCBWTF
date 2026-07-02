package com.smartcbwtf.controller;

import com.smartcbwtf.service.StaffService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StaffControllerTest {

    @Test
    void createStaffCredentialResponseUsesNoStoreNoCacheHeaders() {
        StaffService staffService = mock(StaffService.class);
        StaffController controller = new StaffController(staffService);
        StaffService.CreateStaffRequest request = new StaffService.CreateStaffRequest(
                "Driver One",
                "driver@example.com",
                "9999999999",
                StaffService.ROLE_DRIVER,
                null);
        StaffService.StaffDTO created = new StaffService.StaffDTO(
                UUID.randomUUID(),
                "FAC-DRV-001",
                "Driver One",
                "driver@example.com",
                "9999999999",
                StaffService.ROLE_DRIVER,
                true,
                "offline",
                null,
                Instant.parse("2026-07-01T00:00:00Z"),
                "Tmp@123456");
        when(staffService.createStaff(request)).thenReturn(created);

        var response = controller.createStaff(request);

        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst("Pragma"));
        assertEquals("Tmp@123456", response.getBody().tempPassword());
    }
}
