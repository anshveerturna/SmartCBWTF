package com.smartcbwtf.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class LabelIssueRequest {
    @NotNull
    private UUID hcfId;

    @NotNull
    private UUID facilityId;

    @NotBlank
    private String category;

    @Min(1)
    private int quantity;

    public UUID getHcfId() { return hcfId; }
    public void setHcfId(UUID hcfId) { this.hcfId = hcfId; }
    public UUID getFacilityId() { return facilityId; }
    public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
