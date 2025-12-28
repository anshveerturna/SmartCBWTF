package com.smartcbwtf.dto;

import java.util.UUID;

/**
 * Response containing generated QR code details.
 */
public record QrGenerateResponse(
        UUID qrId,
        String qrPayloadJson // Full JSON payload for QR code generation
) {
}
