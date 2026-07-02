package com.smartcbwtf.config;

import com.smartcbwtf.service.SystemConfigService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemConfigFilterTest {

    private final SystemConfigService configService = mock(SystemConfigService.class);
    private final SystemConfigFilter filter = new SystemConfigFilter(configService);

    @Test
    void maintenanceModeAllowsHealthChecks() throws ServletException, IOException {
        when(configService.getBoolean("platform.maintenance_mode", false)).thenReturn(true);
        when(configService.getString("platform.maintenance_message",
                "System is undergoing scheduled maintenance. Please try again later."))
                .thenReturn("Maintenance");

        MockHttpServletResponse response = doFilter("GET", "/api/health");

        assertEquals(200, response.getStatus());
    }

    @Test
    void maintenanceModeDoesNotBypassAllActuatorPaths() throws ServletException, IOException {
        when(configService.getBoolean("platform.maintenance_mode", false)).thenReturn(true);
        when(configService.getString("platform.maintenance_message",
                "System is undergoing scheduled maintenance. Please try again later."))
                .thenReturn("Maintenance");

        MockHttpServletResponse response = doFilter("GET", "/actuator/env");

        assertEquals(503, response.getStatus());
    }

    @Test
    void maintenanceModeAllowsActuatorHealthOnly() throws ServletException, IOException {
        when(configService.getBoolean("platform.maintenance_mode", false)).thenReturn(true);
        when(configService.getString("platform.maintenance_message",
                "System is undergoing scheduled maintenance. Please try again later."))
                .thenReturn("Maintenance");

        MockHttpServletResponse response = doFilter("GET", "/actuator/health");

        assertEquals(200, response.getStatus());
    }

    private MockHttpServletResponse doFilter(String method, String path) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
