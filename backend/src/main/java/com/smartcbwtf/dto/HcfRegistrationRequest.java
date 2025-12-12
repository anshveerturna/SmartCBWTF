package com.smartcbwtf.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class HcfRegistrationRequest {
    @NotBlank(message = "HCF name is required")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    private String doctorName;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^[0-9+\\-\\s()]+$", message = "Invalid phone number format")
    private String contactPhone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String contactEmail;

    @NotBlank(message = "PAN number is required")
    private String panNo;

    @NotBlank(message = "GST number is required")
    private String gstNo;

    @NotBlank(message = "Aadhar number is required")
    private String aadharNo;

    private BigDecimal monthlyCharges;

    @NotNull(message = "Bedded status is required")
    private Boolean bedded;

    private Integer numberOfBeds; // Required if bedded == true

    private String pcbAuthorizationNo;

    private LocalDate agreementStartDate;
    private LocalDate agreementEndDate;

    private String otherNotes;

    // GPS coordinates (mandatory, auto-captured)
    @NotNull(message = "GPS latitude is required")
    private Double registrationGpsLat;

    @NotNull(message = "GPS longitude is required")
    private Double registrationGpsLon;

    @NotNull(message = "GPS accuracy is required")
    private Double registrationGpsAccuracy;

    // User who submitted the registration
    private UUID registeredByUserId;

    // Terms acceptance (must be true)
    @NotNull(message = "Terms acceptance is required")
    private Boolean termsAccepted;

    private String termsVersion;

    // Facility ID (CBWTF) - usually derived from authenticated user
    private UUID facilityId;

    // getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
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
    public Integer getNumberOfBeds() { return numberOfBeds; }
    public void setNumberOfBeds(Integer numberOfBeds) { this.numberOfBeds = numberOfBeds; }
    public String getPcbAuthorizationNo() { return pcbAuthorizationNo; }
    public void setPcbAuthorizationNo(String pcbAuthorizationNo) { this.pcbAuthorizationNo = pcbAuthorizationNo; }
    public LocalDate getAgreementStartDate() { return agreementStartDate; }
    public void setAgreementStartDate(LocalDate agreementStartDate) { this.agreementStartDate = agreementStartDate; }
    public LocalDate getAgreementEndDate() { return agreementEndDate; }
    public void setAgreementEndDate(LocalDate agreementEndDate) { this.agreementEndDate = agreementEndDate; }
    public String getOtherNotes() { return otherNotes; }
    public void setOtherNotes(String otherNotes) { this.otherNotes = otherNotes; }
    public Double getRegistrationGpsLat() { return registrationGpsLat; }
    public void setRegistrationGpsLat(Double registrationGpsLat) { this.registrationGpsLat = registrationGpsLat; }
    public Double getRegistrationGpsLon() { return registrationGpsLon; }
    public void setRegistrationGpsLon(Double registrationGpsLon) { this.registrationGpsLon = registrationGpsLon; }
    public Double getRegistrationGpsAccuracy() { return registrationGpsAccuracy; }
    public void setRegistrationGpsAccuracy(Double registrationGpsAccuracy) { this.registrationGpsAccuracy = registrationGpsAccuracy; }
    public UUID getRegisteredByUserId() { return registeredByUserId; }
    public void setRegisteredByUserId(UUID registeredByUserId) { this.registeredByUserId = registeredByUserId; }
    public Boolean getTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(Boolean termsAccepted) { this.termsAccepted = termsAccepted; }
    public String getTermsVersion() { return termsVersion; }
    public void setTermsVersion(String termsVersion) { this.termsVersion = termsVersion; }
    public UUID getFacilityId() { return facilityId; }
    public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }
}
