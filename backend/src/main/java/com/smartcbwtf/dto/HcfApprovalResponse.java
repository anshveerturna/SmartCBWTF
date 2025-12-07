package com.smartcbwtf.dto;

import java.util.UUID;

public class HcfApprovalResponse {
    private UUID hcfId;
    private String status;
    private String agreementNumber;

    public HcfApprovalResponse(UUID hcfId, String status, String agreementNumber) {
        this.hcfId = hcfId;
        this.status = status;
        this.agreementNumber = agreementNumber;
    }

    public UUID getHcfId() { return hcfId; }
    public void setHcfId(UUID hcfId) { this.hcfId = hcfId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAgreementNumber() { return agreementNumber; }
    public void setAgreementNumber(String agreementNumber) { this.agreementNumber = agreementNumber; }
}
