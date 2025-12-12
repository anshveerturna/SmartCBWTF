package com.smartcbwtf.dto;

import java.util.UUID;

public class HcfRegistrationResponse {
    private String status;
    private UUID hcfId;
    private String hcfCode;
    private UUID agreementId;
    private String agreementNumber;
    private String pdfUrl;
    private String message;
    private TemplateInfo templateUsed;

    // Default constructor
    public HcfRegistrationResponse() {}

    public HcfRegistrationResponse(String status, UUID hcfId, String hcfCode) {
        this.status = status;
        this.hcfId = hcfId;
        this.hcfCode = hcfCode;
    }

    // Builder-style setters for fluent construction
    public HcfRegistrationResponse withAgreement(UUID agreementId, String agreementNumber) {
        this.agreementId = agreementId;
        this.agreementNumber = agreementNumber;
        return this;
    }

    public HcfRegistrationResponse withPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
        return this;
    }

    public HcfRegistrationResponse withMessage(String message) {
        this.message = message;
        return this;
    }

    public HcfRegistrationResponse withTemplate(TemplateInfo templateUsed) {
        this.templateUsed = templateUsed;
        return this;
    }

    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getHcfId() { return hcfId; }
    public void setHcfId(UUID hcfId) { this.hcfId = hcfId; }
    public String getHcfCode() { return hcfCode; }
    public void setHcfCode(String hcfCode) { this.hcfCode = hcfCode; }
    public UUID getAgreementId() { return agreementId; }
    public void setAgreementId(UUID agreementId) { this.agreementId = agreementId; }
    public String getAgreementNumber() { return agreementNumber; }
    public void setAgreementNumber(String agreementNumber) { this.agreementNumber = agreementNumber; }
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public TemplateInfo getTemplateUsed() { return templateUsed; }
    public void setTemplateUsed(TemplateInfo templateUsed) { this.templateUsed = templateUsed; }

    // Nested class for template info
    public static class TemplateInfo {
        private UUID templateId;
        private String version;
        private UUID facilityId;

        public TemplateInfo() {}

        public TemplateInfo(UUID templateId, String version, UUID facilityId) {
            this.templateId = templateId;
            this.version = version;
            this.facilityId = facilityId;
        }

        public UUID getTemplateId() { return templateId; }
        public void setTemplateId(UUID templateId) { this.templateId = templateId; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public UUID getFacilityId() { return facilityId; }
        public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }
    }
}
