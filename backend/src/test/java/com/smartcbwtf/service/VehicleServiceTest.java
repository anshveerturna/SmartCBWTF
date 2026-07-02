package com.smartcbwtf.service;

import com.smartcbwtf.domain.GpsEvent;
import com.smartcbwtf.domain.Vehicle;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.GpsEventRepository;
import com.smartcbwtf.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private GpsEventRepository gpsEventRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private AppUserRepository userRepository;

    private VehicleService service;

    @BeforeEach
    void setUp() {
        service = new VehicleService(vehicleRepository, gpsEventRepository, facilityRepository, userRepository);
    }

    @Test
    void getVehicleUsesFacilityScopedLookup() {
        UUID facilityId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        when(vehicleRepository.findByIdAndFacilityId(vehicleId, facilityId)).thenReturn(Optional.of(vehicle));

        Optional<Vehicle> result = service.getVehicle(facilityId, vehicleId);

        assertEquals(Optional.of(vehicle), result);
        verify(vehicleRepository).findByIdAndFacilityId(vehicleId, facilityId);
    }

    @Test
    void getLastLocationUsesFacilityScopedGpsLookup() {
        UUID facilityId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        when(gpsEventRepository.findLatestByFacilityIdAndVehicleId(facilityId, vehicleId)).thenReturn(Optional.empty());

        Optional<GpsEvent> result = service.getLastLocation(facilityId, vehicleId);

        assertEquals(Optional.empty(), result);
        verify(gpsEventRepository).findLatestByFacilityIdAndVehicleId(facilityId, vehicleId);
    }

    @Test
    void getGpsTrailDefaultsInvalidLimitInsteadOfThrowing() {
        UUID facilityId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(gpsEventRepository.findRecentByFacilityIdAndVehicleId(eq(facilityId), eq(vehicleId), pageable.capture()))
                .thenReturn(List.of());

        List<GpsEvent> trail = service.getGpsTrail(facilityId, vehicleId, -1);

        assertEquals(0, trail.size());
        assertEquals(50, pageable.getValue().getPageSize());
    }

    @Test
    void getGpsTrailCapsLargeLimit() {
        UUID facilityId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(gpsEventRepository.findRecentByFacilityIdAndVehicleId(eq(facilityId), eq(vehicleId), pageable.capture()))
                .thenReturn(List.of());

        service.getGpsTrail(facilityId, vehicleId, 5000);

        assertEquals(100, pageable.getValue().getPageSize());
    }
}
