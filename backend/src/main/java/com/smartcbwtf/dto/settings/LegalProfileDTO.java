package com.smartcbwtf.dto.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for Section 1: Legal & Entity Profile settings.
 */
public record LegalProfileDTO(
        @Size(max = 255) String legalName,
        @Size(max = 255) String tradeName,
        @Size(max = 100) String authorizationNumber,
        @Size(max = 255) String spcbName,
        @Size(max = 100) String spcbState,
        @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$|^$", message = "Invalid GSTIN format") @Size(max = 20) String gstin,
        @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$|^$", message = "Invalid PAN format") @Size(max = 20) String pan,
        String registeredAddress,
        @Size(max = 100) String registeredState,
        @Pattern(regexp = "^[0-9]{6}$|^$", message = "Invalid pincode format") @Size(max = 10) String registeredPincode,
        @Email @Size(max = 255) String officialEmail,
        @Size(max = 20) String officialPhone,
        @Size(max = 512) String logoUrl,
        @Size(max = 64) String logoChecksum,
        @Size(max = 512) String signatureUrl,
        @Size(max = 64) String signatureChecksum) {
}
