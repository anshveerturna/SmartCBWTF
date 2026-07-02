package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.enums.RouteStatus;
import com.smartcbwtf.dto.route.RouteAlertDTO;
import com.smartcbwtf.dto.route.RouteDTO;
import com.smartcbwtf.service.RouteAssignmentService;
import com.smartcbwtf.service.RouteMapService;
import com.smartcbwtf.service.RouteService;
import com.smartcbwtf.service.RouteWaypointService;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CbwtfRouteControllerValidationTest {

    @Mock
    private RouteService routeService;
    @Mock
    private RouteWaypointService waypointService;
    @Mock
    private RouteAssignmentService assignmentService;
    @Mock
    private RouteMapService mapService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private CbwtfRouteController controller;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        controller = new CbwtfRouteController(routeService, waypointService, assignmentService, mapService);
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void routeMutationEndpointsValidateRequestBodies() throws NoSuchMethodException {
        assertValidatedBody("setStatus", UUID.class, CbwtfRouteController.SetRouteStatusRequest.class);
        assertValidatedBody("resolveAlert", UUID.class, CbwtfRouteController.ResolveAlertRequest.class);
    }

    @Test
    void routeRequestBodiesBoundStatusAndNotes() {
        assertFieldViolation(new CbwtfRouteController.SetRouteStatusRequest(""), "status");
        assertFieldViolation(new CbwtfRouteController.SetRouteStatusRequest("x".repeat(41)), "status");
        assertFieldViolation(new CbwtfRouteController.ResolveAlertRequest("x".repeat(1001)), "notes");
    }

    @Test
    void setStatusAcceptsCaseInsensitiveStatusWithoutRawMap() {
        UUID routeId = UUID.randomUUID();
        RouteDTO route = route(routeId, RouteStatus.ACTIVE);
        when(routeService.setStatus(routeId, facilityId, RouteStatus.ACTIVE)).thenReturn(route);

        var response = controller.setStatus(routeId, new CbwtfRouteController.SetRouteStatusRequest(" active "));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(RouteStatus.ACTIVE, response.getBody().status());
        verify(routeService).setStatus(routeId, facilityId, RouteStatus.ACTIVE);
    }

    @Test
    void setStatusRejectsInvalidStatusBeforeServiceCall() {
        UUID routeId = UUID.randomUUID();

        var response = controller.setStatus(routeId, new CbwtfRouteController.SetRouteStatusRequest("gone"));

        assertEquals(400, response.getStatusCode().value());
        verify(routeService, never()).setStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resolveAlertNormalizesOptionalNotesBeforeServiceCall() {
        UUID alertId = UUID.randomUUID();
        RouteAlertDTO alert = alert(alertId, "checked with driver");
        when(routeService.resolveAlert(alertId, facilityId, "checked with driver")).thenReturn(alert);

        var response = controller.resolveAlert(alertId,
                new CbwtfRouteController.ResolveAlertRequest("  checked with driver  "));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("checked with driver", response.getBody().resolutionNotes());
        verify(routeService).resolveAlert(alertId, facilityId, "checked with driver");
    }

    @Test
    void getAlertsUsesBoundedDefaultLimit() {
        when(routeService.getUnresolvedAlerts(facilityId, 100)).thenReturn(List.of());

        var response = controller.getAlerts(100);

        assertEquals(200, response.getStatusCode().value());
        verify(routeService).getUnresolvedAlerts(facilityId, 100);
    }

    @Test
    void routeExecutionAndHistoryAreNotCacheable() {
        UUID routeId = UUID.randomUUID();
        when(routeService.getRouteExecution(routeId, facilityId)).thenReturn(null);
        when(routeService.getCycleHistory(routeId, facilityId, 0, 10)).thenReturn(List.of());

        var executionResponse = controller.getRouteExecution(routeId);
        var historyResponse = controller.getCycleHistory(routeId, 0, 10);

        assertEquals("no-store", executionResponse.getHeaders().getCacheControl());
        assertEquals("no-cache", executionResponse.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertEquals("no-store", historyResponse.getHeaders().getCacheControl());
        assertEquals("no-cache", historyResponse.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    private void assertValidatedBody(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = CbwtfRouteController.class.getDeclaredMethod(methodName, parameterTypes);
        Parameter parameter = method.getParameters()[method.getParameterCount() - 1];

        assertTrue(parameter.isAnnotationPresent(Valid.class), methodName + " request body must be validated");
        assertTrue(parameter.isAnnotationPresent(RequestBody.class), methodName + " must remain a request body");
    }

    private void assertFieldViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field + " on " + request.getClass().getSimpleName());
    }

    private static RouteDTO route(UUID routeId, RouteStatus status) {
        Instant now = Instant.now();
        return new RouteDTO(routeId, "Route A", null, "#3366ff", status, status == RouteStatus.ACTIVE, 0,
                null, null, 7, null, now, now);
    }

    private static RouteAlertDTO alert(UUID alertId, String notes) {
        Instant now = Instant.now();
        return new RouteAlertDTO(alertId.toString(), UUID.randomUUID().toString(), "Route A", "#3366ff",
                UUID.randomUUID().toString(), 1, null, null, "MISSED_HCF", "MEDIUM", "Missed stop",
                "A stop was missed", 1, null, null, true, "Admin", now, notes, now);
    }
}
