package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.UserGpsEvent;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.UserGpsEventRepository;
import com.smartcbwtf.service.AttendanceService;
import com.smartcbwtf.service.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileApiControllerTest {

    @Mock
    private AttendanceService attendanceService;
    @Mock
    private UserGpsEventRepository gpsEventRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private AuditLogService auditLogService;

    private MobileApiController controller;
    private UUID userId;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        controller = new MobileApiController(attendanceService, gpsEventRepository, appUserRepository,
                facilityRepository, auditLogService);
        userId = UUID.randomUUID();
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, facilityId, null, "DRIVER", "driver"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void currentUserProfileIsNotCacheable() {
        AppUser user = user();
        user.setFullName("Mobile Driver");
        Facility facility = facility();
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));

        var response = controller.getCurrentUser();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertEquals(userId, response.getBody().id());
        assertEquals(facilityId, response.getBody().facilityId());
    }

    @Test
    void gpsPingUsesSingleDuplicateLookupForBatch() {
        UUID existingEventId = UUID.randomUUID();
        UUID newEventId = UUID.randomUUID();
        AppUser user = user();
        Facility facility = facility();
        Instant recordedAt = Instant.parse("2026-06-01T10:15:30Z");
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(gpsEventRepository.findExistingClientEventIds(List.of(existingEventId, newEventId)))
                .thenReturn(List.of(existingEventId));

        var response = controller.pushGpsEvents(new MobileApiController.GpsPingRequest(List.of(
                gpsItem(existingEventId, "28.6139000", "77.2090000", recordedAt.minusSeconds(30)),
                gpsItem(newEventId, "28.6140000", "77.2100000", recordedAt),
                gpsItem(newEventId, "28.6150000", "77.2110000", recordedAt.plusSeconds(30)))));

        MobileApiController.GpsPingResponse body = response.getBody();
        assertEquals(3, body.totalReceived());
        assertEquals(1, body.successCount());
        assertEquals(2, body.duplicateCount());
        assertEquals(List.of(existingEventId, newEventId, newEventId), body.successIds());

        ArgumentCaptor<UserGpsEvent> eventCaptor = ArgumentCaptor.forClass(UserGpsEvent.class);
        verify(gpsEventRepository).save(eventCaptor.capture());
        assertEquals(newEventId, eventCaptor.getValue().getClientEventId());
        assertEquals(new BigDecimal("28.6140000"), eventCaptor.getValue().getLatitude());
        verify(gpsEventRepository, never()).existsByClientEventId(any(UUID.class));
        verify(appUserRepository).save(user);
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("driver");
        user.setRole("DRIVER");
        user.setActive(true);
        return user;
    }

    private Facility facility() {
        Facility facility = new Facility();
        facility.setId(facilityId);
        facility.setCode("FAC");
        facility.setName("Facility");
        return facility;
    }

    private static MobileApiController.GpsEventItem gpsItem(UUID clientEventId, String lat, String lon,
            Instant recordedAt) {
        return new MobileApiController.GpsEventItem(
                clientEventId,
                new BigDecimal(lat),
                new BigDecimal(lon),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("5.00"),
                recordedAt);
    }
}
