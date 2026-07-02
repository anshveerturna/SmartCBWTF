package com.smartcbwtf.controller;

import com.smartcbwtf.dto.admin.TenantDTO;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import com.smartcbwtf.repository.SubscriptionAuditRepository;
import com.smartcbwtf.repository.SystemErrorRepository;
import com.smartcbwtf.service.EmailService;
import com.smartcbwtf.service.SubscriptionService;
import com.smartcbwtf.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerQueryValidationTest {

    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private SubscriptionAuditRepository auditRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private BagEventRepository bagEventRepository;
    @Mock
    private SystemErrorRepository systemErrorRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private EmailService emailService;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(facilityRepository, userRepository, hcfRepository, agreementRepository,
                subscriptionService, auditRepository, invoiceRepository, bagEventRepository, systemErrorRepository,
                passwordEncoder, systemConfigService, emailService);
    }

    @Test
    void listTenantsTrimsSearchBeforeRepositoryCall() {
        when(facilityRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(eq("plant"), eq("plant"),
                any(Pageable.class))).thenReturn(Page.empty());

        ResponseEntity<Page<TenantDTO>> response = controller.listTenants(null, "  plant  ", 0, 20);

        assertEquals(200, response.getStatusCode().value());
        verify(facilityRepository).findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(eq("plant"), eq("plant"),
                any(Pageable.class));
    }

    @Test
    void listTenantsTrimsStatusBeforeRepositoryCall() {
        when(facilityRepository.findBySubscriptionStatus(eq("ACTIVE"), any(Pageable.class))).thenReturn(Page.empty());

        ResponseEntity<Page<TenantDTO>> response = controller.listTenants(" ACTIVE ", null, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        verify(facilityRepository).findBySubscriptionStatus(eq("ACTIVE"), any(Pageable.class));
    }

    @Test
    void listTenantsRejectsOversizedSearchBeforeRepositoryCall() {
        assertThrows(IllegalArgumentException.class, () -> controller.listTenants(null, "x".repeat(121), 0, 20));

        verifyNoInteractions(facilityRepository);
    }

    @Test
    void platformStatsRequestsOnlyRecentSystemErrorLimit() {
        when(invoiceRepository.sumPaidAmount()).thenReturn(Optional.of(BigDecimal.ZERO));
        when(systemErrorRepository.findTop10OpenOrderedBySeverity(any(Pageable.class))).thenReturn(List.of());

        controller.getPlatformStats();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(systemErrorRepository).findTop10OpenOrderedBySeverity(pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void tenantAuditHistoryIsNotCacheable() {
        UUID facilityId = UUID.randomUUID();
        when(auditRepository.findByFacilityId(eq(facilityId), any(Pageable.class))).thenReturn(Page.empty());

        var response = controller.getAuditHistory(facilityId, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }
}
