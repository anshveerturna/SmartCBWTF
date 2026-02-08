package com.smartcbwtf.controller;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Public endpoint for agreement verification.
 * No authentication required — accessed via QR code scan.
 * Returns limited, non-sensitive agreement details to confirm authenticity.
 */
@RestController
@RequestMapping("/api/public/agreement")
public class AgreementVerificationController {

    private final AgreementRepository agreementRepository;

    public AgreementVerificationController(AgreementRepository agreementRepository) {
        this.agreementRepository = agreementRepository;
    }

    @GetMapping("/verify/{id}")
    public ResponseEntity<AgreementVerificationDTO> verifyAgreement(@PathVariable("id") UUID id) {
        return agreementRepository.findById(id)
                .map(agreement -> ResponseEntity.ok(AgreementVerificationDTO.from(agreement)))
                .orElse(ResponseEntity.ok(AgreementVerificationDTO.notFound(id)));
    }

    /**
     * Public DTO — limited info for verification only.
     * Does NOT expose sensitive data like rates, PAN, GST, emails, etc.
     */
    public record AgreementVerificationDTO(
            boolean verified,
            UUID agreementId,
            String agreementNumber,
            String status,
            String facilityName,
            String facilityCode,
            String hcfName,
            String hcfCode,
            LocalDate startDate,
            LocalDate endDate,
            Integer version,
            Instant createdAt,
            String message) {

        public static AgreementVerificationDTO from(Agreement agreement) {
            Facility facility = agreement.getFacility();
            Hcf hcf = agreement.getHcf();

            boolean isActive = "ACTIVE".equals(agreement.getStatus());

            return new AgreementVerificationDTO(
                    true,
                    agreement.getId(),
                    agreement.getAgreementNumber(),
                    agreement.getStatus(),
                    facility != null ? facility.getName() : null,
                    facility != null ? facility.getCode() : null,
                    hcf != null ? hcf.getName() : null,
                    hcf != null ? hcf.getCode() : null,
                    agreement.getStartDate(),
                    agreement.getEndDate(),
                    agreement.getVersion(),
                    agreement.getCreatedAt(),
                    isActive
                            ? "This agreement is verified and currently ACTIVE."
                            : "This agreement is verified but has status: " + agreement.getStatus() + ".");
        }

        public static AgreementVerificationDTO notFound(UUID id) {
            return new AgreementVerificationDTO(
                    false, id, null, null, null, null, null, null,
                    null, null, null, null,
                    "No agreement found with the given ID. This may be an invalid or tampered QR code.");
        }
    }
}
