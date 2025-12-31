package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Branding configuration for facility documents and emails.
 * Controls logo, colors, and footer text appearance.
 */
@Entity
@Table(name = "facility_branding")
public class FacilityBranding {

    @Id
    @Column(name = "facility_id")
    private UUID facilityId;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "logo_checksum", length = 64)
    private String logoChecksum;

    @Column(name = "primary_color", length = 7)
    private String primaryColor = "#1976d2";

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor = "#424242";

    @Column(name = "invoice_footer_text", columnDefinition = "TEXT")
    private String invoiceFooterText;

    @Column(name = "receipt_footer_text", columnDefinition = "TEXT")
    private String receiptFooterText;

    @Column(name = "show_logo_on_invoice")
    private Boolean showLogoOnInvoice = true;

    @Column(name = "show_logo_on_receipt")
    private Boolean showLogoOnReceipt = true;

    @Column(name = "show_logo_on_email")
    private Boolean showLogoOnEmail = true;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // Getters and Setters
    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
    }

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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
