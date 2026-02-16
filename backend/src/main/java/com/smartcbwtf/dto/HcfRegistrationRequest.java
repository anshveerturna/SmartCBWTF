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
    @NotBlank(message = "Address is required")
    private String address;

    // Optional address details
    private String pincode;
    private String state;

    private String doctorName;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^[0-9+\\-\\s()]+$", message = "Invalid phone number format")
    private String contactPhone;

    @Email(message = "Invalid email format")
    private String contactEmail;

    private String panNo;

    private String gstNo;

    private String aadharNo;

    private BigDecimal monthlyCharges;

    private Double occupancy;

    @NotNull(message = "Bedded status is required")
    private Boolean bedded;

    private Integer numberOfBeds; // Required if bedded == true

    // New HCF category fields
    private String hcfType; // HOSPITAL, DENTAL, CLINIC, PATHOLOGY_COLLECTION, PATHOLOGY_STORAGE
    private String city;
    private Integer seatCount; // For Dental/Clinic types

    private String pcbAuthorizationNo;

    // Ownership type: OWNED or RENTED
    @NotBlank(message = "Ownership type is required")
    private String ownershipType;

    // Rent agreement URL (required if rented)
    private String rentAgreementUrl;

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
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getPanNo() {
        return panNo;
    }

    public void setPanNo(String panNo) {
        this.panNo = panNo;
    }

    public String getGstNo() {
        return gstNo;
    }

    public void setGstNo(String gstNo) {
        this.gstNo = gstNo;
    }

    public String getAadharNo() {
        return aadharNo;
    }

    public void setAadharNo(String aadharNo) {
        this.aadharNo = aadharNo;
    }

    public BigDecimal getMonthlyCharges() {
        return monthlyCharges;
    }

    public void setMonthlyCharges(BigDecimal monthlyCharges) {
        this.monthlyCharges = monthlyCharges;
    }

    public Boolean getBedded() {
        return bedded;
    }

    public void setBedded(Boolean bedded) {
        this.bedded = bedded;
    }

    public Integer getNumberOfBeds() {
        return numberOfBeds;
    }

    public void setNumberOfBeds(Integer numberOfBeds) {
        this.numberOfBeds = numberOfBeds;
    }

    public String getPcbAuthorizationNo() {
        return pcbAuthorizationNo;
    }

    public void setPcbAuthorizationNo(String pcbAuthorizationNo) {
        this.pcbAuthorizationNo = pcbAuthorizationNo;
    }

    public Double getOccupancy() {
        return occupancy;
    }

    public void setOccupancy(Double occupancy) {
        this.occupancy = occupancy;
    }

    public String getOwnershipType() {
        return ownershipType;
    }

    public void setOwnershipType(String ownershipType) {
        this.ownershipType = ownershipType;
    }

    public String getRentAgreementUrl() {
        return rentAgreementUrl;
    }

    public void setRentAgreementUrl(String rentAgreementUrl) {
        this.rentAgreementUrl = rentAgreementUrl;
    }

    public LocalDate getAgreementStartDate() {
        return agreementStartDate;
    }

    public void setAgreementStartDate(LocalDate agreementStartDate) {
        this.agreementStartDate = agreementStartDate;
    }

    public LocalDate getAgreementEndDate() {
        return agreementEndDate;
    }

    public void setAgreementEndDate(LocalDate agreementEndDate) {
        this.agreementEndDate = agreementEndDate;
    }

    public String getOtherNotes() {
        return otherNotes;
    }

    public void setOtherNotes(String otherNotes) {
        this.otherNotes = otherNotes;
    }

    public Double getRegistrationGpsLat() {
        return registrationGpsLat;
    }

    public void setRegistrationGpsLat(Double registrationGpsLat) {
        this.registrationGpsLat = registrationGpsLat;
    }

    public Double getRegistrationGpsLon() {
        return registrationGpsLon;
    }

    public void setRegistrationGpsLon(Double registrationGpsLon) {
        this.registrationGpsLon = registrationGpsLon;
    }

    public Double getRegistrationGpsAccuracy() {
        return registrationGpsAccuracy;
    }

    public void setRegistrationGpsAccuracy(Double registrationGpsAccuracy) {
        this.registrationGpsAccuracy = registrationGpsAccuracy;
    }

    public UUID getRegisteredByUserId() {
        return registeredByUserId;
    }

    public void setRegisteredByUserId(UUID registeredByUserId) {
        this.registeredByUserId = registeredByUserId;
    }

    public Boolean getTermsAccepted() {
        return termsAccepted;
    }

    public void setTermsAccepted(Boolean termsAccepted) {
        this.termsAccepted = termsAccepted;
    }

    public String getTermsVersion() {
        return termsVersion;
    }

    public void setTermsVersion(String termsVersion) {
        this.termsVersion = termsVersion;
    }

    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
    }

    public String getHcfType() {
        return hcfType;
    }

    public void setHcfType(String hcfType) {
        this.hcfType = hcfType;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }
}
