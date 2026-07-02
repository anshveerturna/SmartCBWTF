package com.smartcbwtf.dto;

import com.smartcbwtf.domain.QrAuthorization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.UUID;

/**
 * Request to generate a new QR code for waste pickup authorization.
 */
public class QrGenerateRequest {

    @NotNull(message = "HCF ID is required")
    private UUID hcfId;

    @NotBlank(message = "Waste category is required")
    @Pattern(regexp = QrAuthorization.WASTE_CATEGORY_PATTERN, message = "Waste category must be one of YELLOW, RED, BLUE, WHITE")
    private String wasteCategory; // YELLOW, RED, BLUE, WHITE

    @NotNull(message = "Valid from date is required")
    private Instant validFrom;

    @NotNull(message = "Valid to date is required")
    private Instant validTo;

    // Getters and Setters
    public UUID getHcfId() {
        return hcfId;
    }

    public void setHcfId(UUID hcfId) {
        this.hcfId = hcfId;
    }

    public String getWasteCategory() {
        return wasteCategory;
    }

    public void setWasteCategory(String wasteCategory) {
        this.wasteCategory = wasteCategory;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public void setValidTo(Instant validTo) {
        this.validTo = validTo;
    }
}
