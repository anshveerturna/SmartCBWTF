package com.smartcbwtf.service;

import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.SystemError;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.SystemErrorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemErrorServiceTest {

    @Mock
    private SystemErrorRepository errorRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private HcfRepository hcfRepository;

    private SystemErrorService service;

    @BeforeEach
    void setUp() {
        service = new SystemErrorService(errorRepository, facilityRepository, userRepository, hcfRepository);
        lenient().when(errorRepository.save(any(SystemError.class))).thenAnswer(invocation -> {
            SystemError error = invocation.getArgument(0);
            error.setId(UUID.randomUUID());
            return error;
        });
    }

    @Test
    void reportErrorUsesFacilityScopedHcfLookupWhenFacilityIsKnown() {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        Facility facility = new Facility();
        facility.setId(facilityId);
        Hcf hcf = new Hcf();
        hcf.setId(hcfId);
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(hcfRepository.findByIdAndFacilityId(hcfId, facilityId)).thenReturn(Optional.of(hcf));

        SystemError error = service.reportError(
                "Issue",
                "Description",
                "PORTAL",
                "WARNING",
                null,
                facilityId,
                hcfId);

        assertEquals(hcf, error.getHcf());
        verify(hcfRepository).findByIdAndFacilityId(hcfId, facilityId);
        verify(hcfRepository, never()).findById(hcfId);
    }

    @Test
    void reportErrorDoesNotAttachCrossTenantHcf() {
        UUID facilityId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        when(hcfRepository.findByIdAndFacilityId(hcfId, facilityId)).thenReturn(Optional.empty());

        SystemError error = service.reportError(
                "Issue",
                "Description",
                "PORTAL",
                "WARNING",
                null,
                facilityId,
                hcfId);

        assertNull(error.getHcf());
        verify(hcfRepository).findByIdAndFacilityId(hcfId, facilityId);
        verify(hcfRepository, never()).findById(hcfId);
    }

    @Test
    void reportErrorNormalizesSeverityThroughEnum() {
        SystemError error = service.reportError(
                "Issue",
                "Description",
                "PORTAL",
                "critical",
                null,
                null,
                null);

        assertEquals(SystemError.Severity.CRITICAL, error.getSeverityEnum());
    }

    @Test
    void reportErrorRejectsInvalidSeverityBeforePersisting() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.reportError(
                        "Issue",
                        "Description",
                        "PORTAL",
                        "EMERGENCY",
                        null,
                        null,
                        null));

        assertEquals("Invalid error severity: EMERGENCY", thrown.getMessage());
        verify(errorRepository, never()).save(any(SystemError.class));
    }

    @Test
    void updateStatusNormalizesStatusThroughEnum() {
        UUID errorId = UUID.randomUUID();
        SystemError error = SystemError.userReported("Issue", "Description", "PORTAL");
        when(errorRepository.findById(errorId)).thenReturn(Optional.of(error));

        service.updateStatus(errorId, "in_progress");

        assertEquals(SystemError.Status.IN_PROGRESS, error.getStatusEnum());
        verify(errorRepository).save(error);
    }

    @Test
    void updateStatusRejectsInvalidStatusBeforeLookup() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.updateStatus(UUID.randomUUID(), "DONE"));

        assertEquals("Invalid error status: DONE", thrown.getMessage());
        verify(errorRepository, never()).findById(any(UUID.class));
        verify(errorRepository, never()).save(any(SystemError.class));
    }

    @Test
    void getRecentOpenErrorsRequestsOnlyDashboardLimit() {
        when(errorRepository.findTop10OpenOrderedBySeverity(any(Pageable.class))).thenReturn(List.of());

        service.getRecentOpenErrors();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(errorRepository).findTop10OpenOrderedBySeverity(pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void autoDetectErrorsCreatesInactiveHcfIssueFromPickupActivity() {
        when(hcfRepository.countActiveHcfsWithoutRecentCollection(any(Instant.class))).thenReturn(2L);

        service.autoDetectErrors();

        ArgumentCaptor<SystemError> errorCaptor = ArgumentCaptor.forClass(SystemError.class);
        verify(errorRepository).save(errorCaptor.capture());
        SystemError error = errorCaptor.getValue();
        assertEquals("HCFs with no recent activity", error.getTitle());
        assertEquals("HCF_ACTIVITY", error.getComponent());
        assertEquals(SystemError.Source.AUTO_DETECTED, error.getSourceEnum());
        assertEquals(SystemError.Severity.WARNING, error.getSeverityEnum());
        assertTrue(error.getDescription().contains("2 active HCF(s)"));
        verify(hcfRepository).countActiveHcfsWithoutRecentCollection(any(Instant.class));
    }

    @Test
    void autoResolveErrorsResolvesInactiveHcfIssueWhenActivityRecovers() {
        SystemError error = SystemError.autoDetected(
                "HCFs with no recent activity",
                "Inactive HCFs",
                "HCF_ACTIVITY",
                SystemError.Severity.WARNING);
        error.setStatusEnum(SystemError.Status.OPEN);

        when(errorRepository.findOpenAutoDetectedErrors()).thenReturn(List.of(error));
        when(hcfRepository.countActiveHcfsWithoutRecentCollection(any(Instant.class))).thenReturn(0L);

        service.autoResolveErrors();

        assertEquals(SystemError.Status.RESOLVED, error.getStatusEnum());
        assertNotNull(error.getResolvedAt());
        assertEquals("Auto-resolved: condition no longer detected", error.getResolutionNotes());
        verify(errorRepository).save(error);
    }
}
