package com.smartcbwtf.service;

import com.smartcbwtf.domain.AlertSeverity;
import com.smartcbwtf.domain.AlertType;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.FacilityNotificationSettings;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.Invoice;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import com.smartcbwtf.service.email.EmailTemplates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailReminderSchedulerTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private NotificationSettingsService settingsService;
    @Mock
    private AlertService alertService;
    @Mock
    private EmailService emailService;

    private EmailReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new EmailReminderScheduler(
                facilityRepository,
                agreementRepository,
                invoiceRepository,
                settingsService,
                alertService,
                emailService);
    }

    @Test
    void agreementExpiryCreatesIdempotentAlertAndSendsEmailWhenNew() {
        LocalDate today = LocalDate.now(IST);
        Facility facility = facility();
        Hcf hcf = hcf();
        Agreement agreement = new Agreement();
        agreement.setId(UUID.randomUUID());
        agreement.setFacility(facility);
        agreement.setHcf(hcf);
        agreement.setAgreementNumber("AGR-2026-001");
        agreement.setEndDate(today.plusDays(10));

        FacilityNotificationSettings settings = settings(30, 7, 3, 5);
        when(facilityRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(facility)));
        when(settingsService.getSettings(facility.getId())).thenReturn(settings);
        when(agreementRepository.findActiveExpiringBetweenByFacilityId(
                facility.getId(), today, today.plusDays(30))).thenReturn(List.of(agreement));
        when(alertService.createAlert(
                any(), eq(facility.getId()), eq(AlertType.AGREEMENT_EXPIRING), eq(AlertSeverity.WARN),
                anyString(), contains("AGR-2026-001"), eq("Agreement"), eq(agreement.getId()))).thenReturn(true);
        when(emailService.getTemplates()).thenReturn(new EmailTemplates());

        scheduler.checkAgreementExpiry();

        verify(emailService).sendHtmlEmail(
                eq("hcf@example.com"),
                eq("Agreement Expiry Warning - AGR-2026-001"),
                contains("AGR-2026-001"));
        verify(facilityRepository, never()).findAll();
    }

    @Test
    void paymentReminderDoesNotSendDuplicateEmailWhenAlertAlreadyExists() {
        LocalDate today = LocalDate.now(IST);
        Facility facility = facility();
        Hcf hcf = hcf();
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setFacility(facility);
        invoice.setHcf(hcf);
        invoice.setInvoiceNumber("INV-2026-001");
        invoice.setPeriodEnd(today.plusDays(5).minusDays(30));
        invoice.setTotalAmount(new BigDecimal("1250.00"));

        FacilityNotificationSettings settings = settings(30, 7, 3, 5);
        when(facilityRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(facility)));
        when(settingsService.getSettings(facility.getId())).thenReturn(settings);
        when(invoiceRepository.findUnpaidByFacilityIdWithHcf(facility.getId())).thenReturn(List.of(invoice));
        when(alertService.createAlert(
                any(), eq(facility.getId()), eq(AlertType.PAYMENT_DUE), eq(AlertSeverity.WARN),
                anyString(), contains("INV-2026-001"), eq("Invoice"), eq(invoice.getId()))).thenReturn(false);

        scheduler.sendPaymentReminders();

        verify(emailService, never()).sendHtmlEmail(anyString(), anyString(), anyString());
        verify(facilityRepository, never()).findAll();
    }

    @Test
    void agreementExpiryProcessesFacilitiesAcrossPages() {
        LocalDate today = LocalDate.now(IST);
        Facility first = facility();
        Facility second = facility();
        when(facilityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first), org.springframework.data.domain.PageRequest.of(0, 1), 2))
                .thenReturn(new PageImpl<>(List.of(second), org.springframework.data.domain.PageRequest.of(1, 1), 2));
        when(settingsService.getSettings(first.getId())).thenReturn(settings(30, 7, 3, 5));
        when(settingsService.getSettings(second.getId())).thenReturn(settings(30, 7, 3, 5));
        when(agreementRepository.findActiveExpiringBetweenByFacilityId(
                any(), eq(today), eq(today.plusDays(30)))).thenReturn(List.of());

        scheduler.checkAgreementExpiry();

        verify(settingsService).getSettings(first.getId());
        verify(settingsService).getSettings(second.getId());
        verify(facilityRepository, times(2)).findAll(any(Pageable.class));
        verify(facilityRepository, never()).findAll();
    }

    private Facility facility() {
        Facility facility = new Facility();
        facility.setId(UUID.randomUUID());
        facility.setCode("CBWTF-001");
        facility.setName("Smart CBWTF");
        facility.setAddress("Facility address");
        return facility;
    }

    private Hcf hcf() {
        Hcf hcf = new Hcf();
        hcf.setId(UUID.randomUUID());
        hcf.setCode("HCF-001");
        hcf.setName("City Hospital");
        hcf.setAddress("Hospital address");
        hcf.setContactEmail("hcf@example.com");
        hcf.setGpsLat(29.0);
        hcf.setGpsLon(79.0);
        hcf.setStatus("ACTIVE");
        return hcf;
    }

    private FacilityNotificationSettings settings(
            int agreementWarningDays,
            int paymentStartDays,
            int frequencyDays,
            int maxOverdueReminders) {
        FacilityNotificationSettings settings = new FacilityNotificationSettings();
        settings.setAgreementExpiryWarningDays(agreementWarningDays);
        settings.setPaymentReminderStartDays(paymentStartDays);
        settings.setPaymentReminderFrequencyDays(frequencyDays);
        settings.setMaxOverdueReminders(maxOverdueReminders);
        return settings;
    }
}
