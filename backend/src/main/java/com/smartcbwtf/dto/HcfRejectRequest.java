package com.smartcbwtf.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for rejecting an HCF.
 */
public record HcfRejectRequest(
        @NotBlank(message = "Rejection reason is required") String reason) {
}
