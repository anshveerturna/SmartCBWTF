package com.smartcbwtf.dto;

import com.smartcbwtf.domain.QrAuthorization;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for QR authorization details.
 */
public record QrDetailDTO(
        UUID id,
        UUID agreementId,
        String agreementNumber,
        UUID hcfId,
        String hcfName,
        String wasteCategory,
        Instant validFrom,
        Instant validTo,
        String status,
        Instant createdAt,
        Instant usedAt,
        Instant verifiedAt,
        String qrPayloadJson) {
    public static QrDetailDTO from(QrAuthorization qr) {
        return new QrDetailDTO(
                qr.getId(),
                qr.getAgreement().getId(),
                qr.getAgreement().getAgreementNumber(),
                qr.getHcf().getId(),
                qr.getHcf().getName(),
                qr.getWasteCategory(),
                qr.getValidFrom(),
                qr.getValidTo(),
                qr.getStatus(),
                qr.getCreatedAt(),
                qr.getUsedAt(),
                qr.getVerifiedAt(),
                qr.getQrPayload());
    }
}
