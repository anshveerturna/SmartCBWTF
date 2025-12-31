package com.smartcbwtf.dto.settings;

/**
 * DTO for facility branding configuration.
 */
public class BrandingDTO {
    private String logoUrl;
    private String logoChecksum;
    private String primaryColor;
    private String secondaryColor;
    private String invoiceFooterText;
    private String receiptFooterText;
    private Boolean showLogoOnInvoice;
    private Boolean showLogoOnReceipt;
    private Boolean showLogoOnEmail;

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getLogoChecksum() {
        return logoChecksum;
    }

    public void setLogoChecksum(String logoChecksum) {
        this.logoChecksum = logoChecksum;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getInvoiceFooterText() {
        return invoiceFooterText;
    }

    public void setInvoiceFooterText(String invoiceFooterText) {
        this.invoiceFooterText = invoiceFooterText;
    }

    public String getReceiptFooterText() {
        return receiptFooterText;
    }

    public void setReceiptFooterText(String receiptFooterText) {
        this.receiptFooterText = receiptFooterText;
    }

    public Boolean getShowLogoOnInvoice() {
        return showLogoOnInvoice;
    }

    public void setShowLogoOnInvoice(Boolean showLogoOnInvoice) {
        this.showLogoOnInvoice = showLogoOnInvoice;
    }

    public Boolean getShowLogoOnReceipt() {
        return showLogoOnReceipt;
    }

    public void setShowLogoOnReceipt(Boolean showLogoOnReceipt) {
        this.showLogoOnReceipt = showLogoOnReceipt;
    }

    public Boolean getShowLogoOnEmail() {
        return showLogoOnEmail;
    }

    public void setShowLogoOnEmail(Boolean showLogoOnEmail) {
        this.showLogoOnEmail = showLogoOnEmail;
    }
}
