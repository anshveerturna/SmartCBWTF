package com.smartcbwtf.dto;

import java.util.UUID;

public class HcfRegistrationResponse {
    private String status;
    private UUID hcfId;
    private String hcfCode;

    public HcfRegistrationResponse(String status, UUID hcfId, String hcfCode) {
        this.status = status;
        this.hcfId = hcfId;
        this.hcfCode = hcfCode;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getHcfId() { return hcfId; }
    public void setHcfId(UUID hcfId) { this.hcfId = hcfId; }
    public String getHcfCode() { return hcfCode; }
    public void setHcfCode(String hcfCode) { this.hcfCode = hcfCode; }
}
