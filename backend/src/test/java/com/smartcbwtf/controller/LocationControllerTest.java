package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.UserLocation;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.UserLocationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    @Mock
    private UserLocationRepository locationRepository;

    @Mock
    private AppUserRepository userRepository;

    private LocationController controller;

    @BeforeEach
    void setUp() {
        controller = new LocationController(locationRepository, userRepository);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void updateLocationRejectsNonFiniteCoordinatesBeforePersistence() {
        authenticateDriver(UUID.randomUUID());
        LocationController.LocationUpdateRequest request = request(Double.NaN, 78.0322, 12.0);

        var response = controller.updateLocation(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid coordinates", response.getBody().get("error"));
        verifyNoInteractions(locationRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateLocationRejectsOutOfRangeAccuracyBeforePersistence() {
        authenticateDriver(UUID.randomUUID());
        LocationController.LocationUpdateRequest request = request(30.3165, 78.0322, 10_000.1);

        var response = controller.updateLocation(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid accuracy", response.getBody().get("error"));
        verifyNoInteractions(locationRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateLocationPersistsValidLocationAndRefreshesUserPositionWhenDue() {
        UUID userId = UUID.randomUUID();
        authenticateDriver(userId);
        AppUser user = new AppUser();
        when(locationRepository.findFirstByUserIdOrderByRecordedAtDesc(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(locationRepository.save(any(UserLocation.class))).thenAnswer(invocation -> {
            UserLocation location = invocation.getArgument(0);
            location.setId(UUID.randomUUID());
            return location;
        });
        LocationController.LocationUpdateRequest request = request(30.3165, 78.0322, 8.5);

        var response = controller.updateLocation(request);

        ArgumentCaptor<UserLocation> locationCaptor = ArgumentCaptor.forClass(UserLocation.class);
        verify(locationRepository).save(locationCaptor.capture());
        UserLocation saved = locationCaptor.getValue();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, saved.getUserId());
        assertEquals(30.3165, saved.getLatitude());
        assertEquals(78.0322, saved.getLongitude());
        assertEquals(8.5, saved.getAccuracy());
        assertNotNull(saved.getRecordedAt());
        assertNotNull(response.getBody().get("locationId"));
        verify(userRepository).save(user);
        assertEquals(BigDecimal.valueOf(30.3165), user.getLastGpsLat());
        assertEquals(BigDecimal.valueOf(78.0322), user.getLastGpsLon());
    }

    @Test
    void updateLocationSkipsUserPositionRefreshWhenRecentlyUpdated() {
        UUID userId = UUID.randomUUID();
        authenticateDriver(userId);
        UserLocation recent = UserLocation.create(userId, 30.3, 78.0, 10.0);
        recent.setRecordedAt(Instant.now());
        when(locationRepository.findFirstByUserIdOrderByRecordedAtDesc(userId)).thenReturn(Optional.of(recent));
        when(locationRepository.save(any(UserLocation.class))).thenAnswer(invocation -> {
            UserLocation location = invocation.getArgument(0);
            location.setId(UUID.randomUUID());
            return location;
        });

        var response = controller.updateLocation(request(30.3165, 78.0322, 8.5));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verifyNoInteractions(userRepository);
    }

    @Test
    void currentLocationIsNotCacheable() {
        UUID userId = UUID.randomUUID();
        authenticateDriver(userId);
        UserLocation location = UserLocation.create(userId, 30.3165, 78.0322, 8.5);
        when(locationRepository.findFirstByUserIdOrderByRecordedAtDesc(userId)).thenReturn(Optional.of(location));

        var response = controller.getCurrentLocation();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    @Test
    void adminLocationHistoryIsNotCacheable() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);
        when(locationRepository.findByUserIdOrderByRecordedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(Page.empty());

        var response = controller.getLocationHistory(userId, 0, 50);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    @Test
    void adminLatestLocationIsNotCacheable() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("driver");
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(locationRepository.findFirstByUserIdOrderByRecordedAtDesc(userId)).thenReturn(Optional.empty());

        var response = controller.getUserLocation(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    private void authenticateDriver(UUID userId) {
        TenantContext.set(new TenantContext.TenantInfo(
                userId,
                UUID.randomUUID(),
                null,
                "DRIVER",
                "driver"));
    }

    private LocationController.LocationUpdateRequest request(Double latitude, Double longitude, Double accuracy) {
        LocationController.LocationUpdateRequest request = new LocationController.LocationUpdateRequest();
        request.latitude = latitude;
        request.longitude = longitude;
        request.accuracy = accuracy;
        return request;
    }
}
