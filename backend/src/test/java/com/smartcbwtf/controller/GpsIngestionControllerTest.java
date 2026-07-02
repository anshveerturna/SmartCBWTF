package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.service.GpsDeviceBindingService;
import com.smartcbwtf.service.GpsIngestionService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GpsIngestionControllerTest {

    @Mock
    private GpsIngestionService ingestionService;

    @Mock
    private GpsDeviceBindingService bindingService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void bindDeviceUsesAuthenticatedUserInsteadOfRequestBodyActor() {
        UUID authenticatedUserId = UUID.randomUUID();
        UUID forgedUserId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        authenticateSuperAdmin(authenticatedUserId);
        GpsIngestionController controller = new GpsIngestionController(ingestionService, bindingService);

        var response = controller.bindDevice(new GpsIngestionController.BindRequest(
                "GPS-001",
                vehicleId,
                "wheelseye",
                forgedUserId,
                " Installed\nby support "));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bindingService).bindDevice(
                "GPS-001",
                vehicleId,
                "WHEELSEYE",
                authenticatedUserId,
                "Installed by support");
    }

    @Test
    void ingestGpsDataNormalizesVendorBeforeServiceCall() {
        GpsIngestionController controller = new GpsIngestionController(ingestionService, bindingService);
        when(ingestionService.ingestFromVendor("WHEELSEYE", "{}")).thenReturn(3);

        var response = controller.ingestGpsData(" wheelseye ", "{}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().success());
        assertEquals(3, response.getBody().eventsIngested());
        verify(ingestionService).ingestFromVendor("WHEELSEYE", "{}");
    }

    @Test
    void ingestGpsDataRejectsUnsafeVendorBeforeServiceCall() {
        GpsIngestionController controller = new GpsIngestionController(ingestionService, bindingService);

        var response = controller.ingestGpsData("wheel/seye", "{}");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(false, response.getBody().success());
        assertEquals("Vendor contains invalid characters", response.getBody().error());
        verifyNoInteractions(ingestionService);
    }

    @Test
    void unbindDeviceUsesAuthenticatedUserAndTrimsBlankNotes() {
        UUID authenticatedUserId = UUID.randomUUID();
        authenticateSuperAdmin(authenticatedUserId);
        GpsIngestionController controller = new GpsIngestionController(ingestionService, bindingService);

        var response = controller.unbindDevice(new GpsIngestionController.UnbindRequest(
                "GPS-001",
                UUID.randomUUID(),
                "   "));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bindingService).unbindDevice("GPS-001", authenticatedUserId, null);
    }

    @Test
    void bindDeviceWithoutAuthenticatedContextReturnsUnauthorized() {
        GpsIngestionController controller = new GpsIngestionController(ingestionService, bindingService);

        var response = controller.bindDevice(new GpsIngestionController.BindRequest(
                "GPS-001",
                UUID.randomUUID(),
                "WHEELSEYE",
                UUID.randomUUID(),
                null));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(false, response.getBody().success());
        verifyNoInteractions(bindingService);
    }

    @Test
    void bindRequestValidationRejectsMissingAndUnsafeFields() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        var violations = validator.validate(new GpsIngestionController.BindRequest(
                "",
                null,
                "wheel seye!",
                UUID.randomUUID(),
                "x".repeat(2001)));

        assertTrue(violations.stream().anyMatch(v -> "deviceId".contentEquals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "vehicleId".contentEquals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "vendor".contentEquals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "notes".contentEquals(v.getPropertyPath().toString())));
    }

    private void authenticateSuperAdmin(UUID userId) {
        TenantContext.set(new TenantContext.TenantInfo(
                userId,
                null,
                null,
                "SUPER_ADMIN",
                "super_admin"));
    }
}
