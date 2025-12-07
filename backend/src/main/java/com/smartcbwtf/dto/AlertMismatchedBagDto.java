package com.smartcbwtf.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AlertMismatchedBagDto {
    private UUID bagLabelId;
    private String qrCode;
    private String category;
    private String hcfName;
    private BigDecimal hcfWeightKg;
    private BigDecimal cbtwfWeightKg;
    private BigDecimal deltaKg;
    private Instant verificationTs;

    public AlertMismatchedBagDto(UUID bagLabelId, String qrCode, String category, String hcfName, BigDecimal hcfWeightKg, BigDecimal cbtwfWeightKg, BigDecimal deltaKg, Instant verificationTs) {
        this.bagLabelId = bagLabelId;
        this.qrCode = qrCode;
        this.category = category;
        this.hcfName = hcfName;
        this.hcfWeightKg = hcfWeightKg;
        this.cbtwfWeightKg = cbtwfWeightKg;
        this.deltaKg = deltaKg;
        this.verificationTs = verificationTs;
    }

    public UUID getBagLabelId() { return bagLabelId; }
    public void setBagLabelId(UUID bagLabelId) { this.bagLabelId = bagLabelId; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getHcfName() { return hcfName; }
    public void setHcfName(String hcfName) { this.hcfName = hcfName; }
    public BigDecimal getHcfWeightKg() { return hcfWeightKg; }
    public void setHcfWeightKg(BigDecimal hcfWeightKg) { this.hcfWeightKg = hcfWeightKg; }
    public BigDecimal getCbtwfWeightKg() { return cbtwfWeightKg; }
    public void setCbtwfWeightKg(BigDecimal cbtwfWeightKg) { this.cbtwfWeightKg = cbtwfWeightKg; }
    public BigDecimal getDeltaKg() { return deltaKg; }
    public void setDeltaKg(BigDecimal deltaKg) { this.deltaKg = deltaKg; }
    public Instant getVerificationTs() { return verificationTs; }
    public void setVerificationTs(Instant verificationTs) { this.verificationTs = verificationTs; }
}
