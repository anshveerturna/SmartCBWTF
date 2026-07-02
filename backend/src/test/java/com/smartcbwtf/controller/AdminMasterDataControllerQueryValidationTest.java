package com.smartcbwtf.controller;

import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.AttendanceRepository;
import com.smartcbwtf.repository.AuditLogRepository;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import com.smartcbwtf.repository.PaymentRepository;
import com.smartcbwtf.repository.SubscriptionAuditRepository;
import com.smartcbwtf.repository.VehicleRepository;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMasterDataControllerQueryValidationTest {

    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private BagLabelRepository bagLabelRepository;
    @Mock
    private BagEventRepository bagEventRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private SubscriptionAuditRepository subscriptionAuditRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private PaymentRepository paymentRepository;

    private AdminMasterDataController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminMasterDataController(hcfRepository, attendanceRepository, bagLabelRepository,
                bagEventRepository, invoiceRepository, auditLogRepository, subscriptionAuditRepository,
                userRepository, facilityRepository, vehicleRepository, paymentRepository);
    }

    @Test
    void listHcfsTrimsStatusAndSearchBeforeRepositoryCall() {
        UUID facilityId = UUID.randomUUID();
        when(hcfRepository.findByFacilityIdWithFilters(eq(facilityId), eq("ACTIVE"), eq("Hospital"),
                any(Pageable.class))).thenReturn(Page.empty());

        controller.listHcfs(facilityId, " ACTIVE ", "  Hospital  ", 0, 20);

        verify(hcfRepository).findByFacilityIdWithFilters(eq(facilityId), eq("ACTIVE"), eq("Hospital"),
                any(Pageable.class));
    }

    @Test
    void listHcfsRejectsOversizedSearchBeforeRepositoryCall() {
        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.listHcfs(null, null, "x".repeat(121), 0, 20));

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
        verifyNoInteractions(hcfRepository);
    }

    @Test
    void listQrLabelsUsesNormalizedStatusFilter() {
        when(bagLabelRepository.findByStatus(eq("ISSUED"), any(Pageable.class))).thenReturn(Page.empty());

        controller.listQrLabels(null, " ISSUED ", null, 0, 20);

        verify(bagLabelRepository).findByStatus(eq("ISSUED"), any(Pageable.class));
        verify(bagLabelRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listBagsUsesCategoryFilterWhenStatusMissing() {
        when(bagLabelRepository.findByCategory(eq("YELLOW"), any(Pageable.class))).thenReturn(Page.empty());

        controller.listBags(null, null, " YELLOW ", 0, 20);

        verify(bagLabelRepository).findByCategory(eq("YELLOW"), any(Pageable.class));
    }

    @Test
    void listAttendanceRejectsInvalidDateRangeBeforeRepositoryCall() {
        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.listAttendance(null, "2026-07-10", "2026-07-01", 0, 20));

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void listAttendanceUsesFacilityFilterWhenProvided() {
        UUID facilityId = UUID.randomUUID();
        when(attendanceRepository.findByFacilityId(eq(facilityId), any(Pageable.class))).thenReturn(Page.empty());

        controller.listAttendance(facilityId, "2026-07-01", "2026-07-02", 0, 20);

        verify(attendanceRepository).findByFacilityId(eq(facilityId), any(Pageable.class));
        verify(attendanceRepository, never()).findByEventTsBetween(any(Instant.class), any(Instant.class),
                any(Pageable.class));
    }

    @Test
    void listAuditLogsAreNotCacheable() {
        when(subscriptionAuditRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        var response = controller.listAuditLogs(null, null, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    @Test
    void listPickupsUsesDateRangeWhenNoFacilityOrEventTypeFilter() {
        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        when(bagEventRepository.findByEventTsBetween(from.capture(), to.capture(), any(Pageable.class)))
                .thenReturn(Page.empty());

        controller.listPickups(null, null, "2026-07-01", "2026-07-02", 0, 20);

        verify(bagEventRepository).findByEventTsBetween(any(Instant.class), any(Instant.class), any(Pageable.class));
        assertEquals(2, java.time.Duration.between(from.getValue(), to.getValue()).toDays());
    }
}
