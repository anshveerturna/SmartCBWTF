package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Hcf {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String contactEmail;
    private String contactPhone;
    private Integer numberOfBeds;

    @Column(nullable = false)
    private Double gpsLat;

    @Column(nullable = false)
    private Double gpsLon;

    @Column(nullable = false)
    private String status; // PENDING_APPROVAL / ACTIVE / REJECTED

    // Registration GPS (captured at registration time)
    private Double registrationGpsLat;
    private Double registrationGpsLon;
    private Double registrationGpsAccuracy;

    // Additional registration fields
    private String doctorName;
    private String panNo;
    private String gstNo;
    private String aadharNo;
    private BigDecimal monthlyCharges;
    private Boolean bedded;
    private String pcbAuthorizationNo;
    
    @Column(columnDefinition = "TEXT")
    private String otherNotes;

    @ManyToOne
    @JoinColumn(name = "registered_by_user_id")
    private AppUser registeredByUser;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public Integer getNumberOfBeds() { return numberOfBeds; }
    public void setNumberOfBeds(Integer numberOfBeds) { this.numberOfBeds = numberOfBeds; }
    public Double getGpsLat() { return gpsLat; }
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }
    public Double getGpsLon() { return gpsLon; }
    public void setGpsLon(Double gpsLon) { this.gpsLon = gpsLon; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // New registration fields
    public Double getRegistrationGpsLat() { return registrationGpsLat; }
    public void setRegistrationGpsLat(Double registrationGpsLat) { this.registrationGpsLat = registrationGpsLat; }
    public Double getRegistrationGpsLon() { return registrationGpsLon; }
    public void setRegistrationGpsLon(Double registrationGpsLon) { this.registrationGpsLon = registrationGpsLon; }
    public Double getRegistrationGpsAccuracy() { return registrationGpsAccuracy; }
    public void setRegistrationGpsAccuracy(Double registrationGpsAccuracy) { this.registrationGpsAccuracy = registrationGpsAccuracy; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getPanNo() { return panNo; }
    public void setPanNo(String panNo) { this.panNo = panNo; }
    public String getGstNo() { return gstNo; }
    public void setGstNo(String gstNo) { this.gstNo = gstNo; }
    public String getAadharNo() { return aadharNo; }
    public void setAadharNo(String aadharNo) { this.aadharNo = aadharNo; }
    public BigDecimal getMonthlyCharges() { return monthlyCharges; }
    public void setMonthlyCharges(BigDecimal monthlyCharges) { this.monthlyCharges = monthlyCharges; }
    public Boolean getBedded() { return bedded; }
    public void setBedded(Boolean bedded) { this.bedded = bedded; }
    public String getPcbAuthorizationNo() { return pcbAuthorizationNo; }
    public void setPcbAuthorizationNo(String pcbAuthorizationNo) { this.pcbAuthorizationNo = pcbAuthorizationNo; }
    public String getOtherNotes() { return otherNotes; }
    public void setOtherNotes(String otherNotes) { this.otherNotes = otherNotes; }
    public AppUser getRegisteredByUser() { return registeredByUser; }
    public void setRegisteredByUser(AppUser registeredByUser) { this.registeredByUser = registeredByUser; }
}
