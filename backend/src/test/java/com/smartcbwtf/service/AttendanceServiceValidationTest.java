package com.smartcbwtf.service;

import com.smartcbwtf.dto.AttendanceSyncItem;
import com.smartcbwtf.dto.AttendanceSyncRequest;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.AttendanceRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceValidationTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private FeatureGuardService featureGuardService;
    @Mock
    private AgreementGuardService agreementGuard;
    @Mock
    private RouteExecutionService routeExecutionService;

    private AttendanceService attendanceService;
    private UUID facilityId;
    private UUID driverId;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(
                attendanceRepository,
                hcfRepository,
                appUserRepository,
                facilityRepository,
                auditLogService,
                featureGuardService,
                agreementGuard,
                routeExecutionService);
        facilityId = UUID.randomUUID();
        driverId = UUID.randomUUID();
    }

    @Test
    void syncRejectsFutureAttendanceBeforeRepositoryWork() {
        var response = attendanceService.sync(
                requestWithTimestamp(Instant.now().plus(1, ChronoUnit.DAYS)),
                driverId,
                facilityId);

        assertEquals(1, response.getFailureCount());
        assertFalse(response.getResults().get(0).isSuccess());
        assertEquals("INVALID_TIMESTAMP", response.getResults().get(0).getErrorCode());
        verify(featureGuardService).assertEnabled(facilityId, FeatureGuardService.ATTENDANCE_ENFORCEMENT);
        verifyNoInteractions(attendanceRepository, hcfRepository, appUserRepository, facilityRepository,
                auditLogService, agreementGuard, routeExecutionService);
    }

    @Test
    void syncRejectsStaleOfflineAttendanceBeforeRepositoryWork() {
        var response = attendanceService.sync(
                requestWithTimestamp(Instant.now().minus(31, ChronoUnit.DAYS)),
                driverId,
                facilityId);

        assertEquals(1, response.getFailureCount());
        assertFalse(response.getResults().get(0).isSuccess());
        assertEquals("INVALID_TIMESTAMP", response.getResults().get(0).getErrorCode());
        verify(featureGuardService).assertEnabled(facilityId, FeatureGuardService.ATTENDANCE_ENFORCEMENT);
        verifyNoInteractions(attendanceRepository, hcfRepository, appUserRepository, facilityRepository,
                auditLogService, agreementGuard, routeExecutionService);
    }

    private AttendanceSyncRequest requestWithTimestamp(Instant eventTs) {
        AttendanceSyncItem item = new AttendanceSyncItem();
        item.setClientEventId(UUID.randomUUID());
        item.setHcfId(UUID.randomUUID());
        item.setEventTsMillis(eventTs.toEpochMilli());
        item.setGpsLat(28.6140);
        item.setGpsLon(77.2091);

        AttendanceSyncRequest request = new AttendanceSyncRequest();
        request.setEvents(List.of(item));
        return request;
    }
}
