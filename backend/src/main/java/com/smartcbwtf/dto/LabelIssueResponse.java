package com.smartcbwtf.dto;

import java.util.List;
import java.util.UUID;

public class LabelIssueResponse {
    private UUID hcfId;
    private UUID facilityId;
    private String category;
    private int quantity;
    private List<String> qrCodes;
    private String pdfUrl;

    public LabelIssueResponse(UUID hcfId, UUID facilityId, String category, int quantity, List<String> qrCodes, String pdfUrl) {
        this.hcfId = hcfId;
        this.facilityId = facilityId;
        this.category = category;
        this.quantity = quantity;
        this.qrCodes = qrCodes;
        this.pdfUrl = pdfUrl;
    }

    public UUID getHcfId() { return hcfId; }
    public void setHcfId(UUID hcfId) { this.hcfId = hcfId; }
    public UUID getFacilityId() { return facilityId; }
    public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public List<String> getQrCodes() { return qrCodes; }
    public void setQrCodes(List<String> qrCodes) { this.qrCodes = qrCodes; }
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
}
