package com.smartcbwtf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendEmail(String to, String subject, String body) {
        // Stub: log instead of sending. Integrate SMTP in production.
        log.info("[DEV-EMAIL] to={} subject={} body={}", to, subject, body);
    }
}
