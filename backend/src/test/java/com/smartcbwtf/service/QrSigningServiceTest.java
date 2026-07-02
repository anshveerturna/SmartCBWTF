package com.smartcbwtf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrSigningServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QrSigningService service = new QrSigningService(
            objectMapper,
            "0123456789abcdef0123456789abcdef");

    @Test
    void generatedPayloadVerifiesSuccessfully() {
        QrSigningService.SignedPayload payload = signedPayload();

        assertTrue(service.verifyChecksum(payload.json()));
    }

    @Test
    void verificationRejectsTamperedPayload() throws Exception {
        QrSigningService.SignedPayload payload = signedPayload();
        @SuppressWarnings("unchecked")
        Map<String, Object> tampered = objectMapper.readValue(payload.json(), LinkedHashMap.class);
        tampered.put("wasteCategory", "RED");

        assertFalse(service.verifyChecksum(objectMapper.writeValueAsString(tampered)));
    }

    @Test
    void verificationRejectsMissingChecksum() throws Exception {
        QrSigningService.SignedPayload payload = signedPayload();
        @SuppressWarnings("unchecked")
        Map<String, Object> unsigned = objectMapper.readValue(payload.json(), LinkedHashMap.class);
        unsigned.remove("checksum");

        assertFalse(service.verifyChecksum(objectMapper.writeValueAsString(unsigned)));
    }

    @Test
    void constructorRejectsWeakSigningSecret() {
        assertThrows(IllegalStateException.class,
                () -> new QrSigningService(objectMapper, "smartcbwtf-local-dev-signing-key-2025-not-for-prod"));
    }

    private QrSigningService.SignedPayload signedPayload() {
        return service.generateSignedPayload(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "YELLOW",
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-31T23:59:59Z"));
    }
}
