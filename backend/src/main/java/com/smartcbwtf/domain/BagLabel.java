package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class BagLabel {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    private Hcf hcf;

    @ManyToOne(optional = false)
    private Facility facility;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String serialNo;

    @Column(nullable = false, unique = true)
    private String qrCode;

    @Column(nullable = false)
    private String status; // ISSUED / USED / VOID

    private Instant issuedAt = Instant.now();
    private Instant usedAt;
    private String voidReason;

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Hcf getHcf() { return hcf; }
    public void setHcf(Hcf hcf) { this.hcf = hcf; }
    public Facility getFacility() { return facility; }
    public void setFacility(Facility facility) { this.facility = facility; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
}
