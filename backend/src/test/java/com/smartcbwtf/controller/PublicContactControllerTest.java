package com.smartcbwtf.controller;

import com.smartcbwtf.domain.ContactMessage;
import com.smartcbwtf.dto.ContactRequestDTO;
import com.smartcbwtf.repository.ContactMessageRepository;
import com.smartcbwtf.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicContactControllerTest {

    @Mock
    private ContactMessageRepository contactMessageRepository;

    @Mock
    private EmailService emailService;

    private PublicContactController controller;

    @BeforeEach
    void setUp() {
        controller = new PublicContactController(contactMessageRepository, emailService);
    }

    @Test
    void submitContactFormPersistsLeadAndSendsNotification() {
        ContactRequestDTO request = validRequest();
        MockHttpServletRequest servletRequest = requestFrom("203.0.113.10");
        when(contactMessageRepository.countByEmailIgnoreCaseAndCreatedAtAfter(eq("lead@example.com"), any(Instant.class)))
                .thenReturn(0L);
        when(contactMessageRepository.countBySourceIpAndCreatedAtAfter(eq("203.0.113.10"), any(Instant.class)))
                .thenReturn(0L);

        ResponseEntity<Map<String, String>> response = controller.submitContactForm(request, servletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<ContactMessage> captor = ArgumentCaptor.forClass(ContactMessage.class);
        verify(contactMessageRepository).save(captor.capture());
        ContactMessage saved = captor.getValue();
        assertEquals("Riya Sharma", saved.getName());
        assertEquals("lead@example.com", saved.getEmail());
        assertEquals("CBWTF Operator", saved.getOrganizationType());
        assertEquals("Request Demo", saved.getInquiryType());
        assertEquals("203.0.113.10", saved.getSourceIp());
        assertEquals("SmartCBWTF Test Browser", saved.getUserAgent());
        verify(emailService).sendEmail(eq("info@smartcbwtf.com"), eq("New Contact Request from Riya Sharma"),
                any(String.class));
    }

    @Test
    void submitContactFormStillSucceedsWhenNotificationEmailFails() {
        ContactRequestDTO request = validRequest();
        MockHttpServletRequest servletRequest = requestFrom("203.0.113.20");
        when(contactMessageRepository.countByEmailIgnoreCaseAndCreatedAtAfter(eq("lead@example.com"), any(Instant.class)))
                .thenReturn(0L);
        when(contactMessageRepository.countBySourceIpAndCreatedAtAfter(eq("203.0.113.20"), any(Instant.class)))
                .thenReturn(0L);
        doThrow(new RuntimeException("mail down")).when(emailService)
                .sendEmail(eq("info@smartcbwtf.com"), any(String.class), any(String.class));

        ResponseEntity<Map<String, String>> response = controller.submitContactForm(request, servletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(contactMessageRepository).save(any(ContactMessage.class));
    }

    @Test
    void submitContactFormRateLimitsRepeatedEmail() {
        ContactRequestDTO request = validRequest();
        MockHttpServletRequest servletRequest = requestFrom("203.0.113.30");
        when(contactMessageRepository.countByEmailIgnoreCaseAndCreatedAtAfter(eq("lead@example.com"), any(Instant.class)))
                .thenReturn(3L);

        ResponseEntity<Map<String, String>> response = controller.submitContactForm(request, servletRequest);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        verify(contactMessageRepository, never()).save(any(ContactMessage.class));
        verify(emailService, never()).sendEmail(any(String.class), any(String.class), any(String.class));
    }

    @Test
    void submitContactFormRejectsUnknownCategoryValues() {
        ContactRequestDTO request = validRequest();
        request.setOrganizationType("Search Ranking Outreach");

        ResponseEntity<Map<String, String>> response = controller.submitContactForm(request, requestFrom("203.0.113.35"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(contactMessageRepository, never()).countByEmailIgnoreCaseAndCreatedAtAfter(any(String.class),
                any(Instant.class));
        verify(contactMessageRepository, never()).save(any(ContactMessage.class));
        verify(emailService, never()).sendEmail(any(String.class), any(String.class), any(String.class));
    }

    @Test
    void submitContactFormRateLimitsByRemoteAddressWhenForwardedForIsSpoofed() {
        ContactRequestDTO request = validRequest();
        MockHttpServletRequest servletRequest = requestFrom("203.0.113.200", "198.51.100.55");
        when(contactMessageRepository.countByEmailIgnoreCaseAndCreatedAtAfter(eq("lead@example.com"), any(Instant.class)))
                .thenReturn(0L);
        when(contactMessageRepository.countBySourceIpAndCreatedAtAfter(eq("198.51.100.55"), any(Instant.class)))
                .thenReturn(5L);

        ResponseEntity<Map<String, String>> response = controller.submitContactForm(request, servletRequest);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        verify(contactMessageRepository, never()).countBySourceIpAndCreatedAtAfter(eq("203.0.113.200"),
                any(Instant.class));
        verify(contactMessageRepository, never()).save(any(ContactMessage.class));
        verify(emailService, never()).sendEmail(any(String.class), any(String.class), any(String.class));
    }

    @Test
    void submitContactFormStoresForwardedAddressFromTrustedProxy() {
        ContactRequestDTO request = validRequest();
        MockHttpServletRequest servletRequest = requestFrom("203.0.113.60", "172.16.0.10");
        when(contactMessageRepository.countByEmailIgnoreCaseAndCreatedAtAfter(eq("lead@example.com"), any(Instant.class)))
                .thenReturn(0L);
        when(contactMessageRepository.countBySourceIpAndCreatedAtAfter(eq("203.0.113.60"), any(Instant.class)))
                .thenReturn(0L);

        ResponseEntity<Map<String, String>> response = controller.submitContactForm(request, servletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<ContactMessage> captor = ArgumentCaptor.forClass(ContactMessage.class);
        verify(contactMessageRepository).save(captor.capture());
        assertEquals("203.0.113.60", captor.getValue().getSourceIp());
    }

    @Test
    void submitContactFormDropsHoneypotSubmissionsWithoutPersisting() {
        ContactRequestDTO request = validRequest();
        request.setWebsite("https://spam.example");

        ResponseEntity<Map<String, String>> response = controller.submitContactForm(request, requestFrom("203.0.113.40"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(contactMessageRepository, never()).save(any(ContactMessage.class));
        verify(emailService, never()).sendEmail(any(String.class), any(String.class), any(String.class));
    }

    private static ContactRequestDTO validRequest() {
        return new ContactRequestDTO(
                " Riya Sharma ",
                "Lead@Example.com",
                "+91 98765 43210",
                "North Plant",
                "CBWTF Operator",
                "Request Demo",
                "Please help us evaluate the platform.");
    }

    private static MockHttpServletRequest requestFrom(String sourceIp) {
        return requestFrom(sourceIp, "10.0.0.1");
    }

    private static MockHttpServletRequest requestFrom(String sourceIp, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", sourceIp);
        request.addHeader("User-Agent", "SmartCBWTF Test Browser");
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
