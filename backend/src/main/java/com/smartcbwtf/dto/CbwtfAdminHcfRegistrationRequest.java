package com.smartcbwtf.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for CBWTF Admin to directly register an HCF.
 * Unlike mobile registration, this does NOT require:
 * - GPS accuracy (admin sets location via map)
 * - Terms acceptance (admin action implies acceptance)
 * - Approval workflow (auto-approved)
 */
public class CbwtfAdminHcfRegistrationRequest {

    // HCF Information
    @NotBlank(message = "HCF name is required")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Doctor/Owner name is required")
    private String doctorName;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^[0-9+\\-\\s()]+$", message = "Invalid phone number format")
    private String contactPhone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String contactEmail;

    // Government IDs (Optional)
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN format")
    private String panNo;

    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z]Z[0-9A-Z]$", message = "Invalid GST format")
    private String gstNo;

    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhar must be 12 digits")
    private String aadharNo;

    // Ownership
    @NotBlank(message = "Ownership type is required")
    @Pattern(regexp = "^(OWNED|RENTED)$", message = "Ownership type must be OWNED or RENTED")
    private String ownershipType;

    // Rent agreement URL (required if RENTED - validated in service)
    private String rentAgreementUrl;

    // Facility Type
    @NotNull(message = "Bedded status is required")
    private Boolean bedded;

    private Integer numberOfBeds; // Required if bedded == true

    // New HCF category fields
    private String hcfType; // HOSPITAL, DENTAL, CLINIC, PATHOLOGY_COLLECTION, PATHOLOGY_STORAGE
    private String city;
    private Integer seatCount; // For Dental/Clinic types

    private BigDecimal monthlyCharges;

    private String otherNotes;

    // Location (set via map picker)
    @NotNull(message = "Latitude is required")
    private Double gpsLat;

    @NotNull(message = "Longitude is required")
    private Double gpsLon;

    // Agreement Period
    @NotNull(message = "Agreement start date is required")
    private LocalDate agreementStartDate;

    @NotNull(message = "Agreement end date is required")
    private LocalDate agreementEndDate;

    // Per-bed rate for billing
    @NotNull(message = "Per bed per day rate is required")
    private BigDecimal perBedPerDayRate;

    // Tax rate percentage (e.g. 18.0 for 18% GST). Defaults to 18.0 if not provided.
    private Double taxRate;

    // Optional custom agreement number (overrides auto-generation)
    private String customAgreementNumber;

    // Getters and Setters
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

    public BigDecimal getMonthlyCharges() {
        return monthlyCharges;
    }

    public void setMonthlyCharges(BigDecimal monthlyCharges) {
        this.monthlyCharges = monthlyCharges;
    }

    public String getOtherNotes() {
        return otherNotes;
    }

    public void setOtherNotes(String otherNotes) {
        this.otherNotes = otherNotes;
    }

    public Double getGpsLat() {
        return gpsLat;
    }

    public void setGpsLat(Double gpsLat) {
        this.gpsLat = gpsLat;
    }

    public Double getGpsLon() {
        return gpsLon;
    }

    public void setGpsLon(Double gpsLon) {
        this.gpsLon = gpsLon;
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

    public BigDecimal getPerBedPerDayRate() {
        return perBedPerDayRate;
    }

    public void setPerBedPerDayRate(BigDecimal perBedPerDayRate) {
        this.perBedPerDayRate = perBedPerDayRate;
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

    public Double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(Double taxRate) {
        this.taxRate = taxRate;
    }

    public String getCustomAgreementNumber() {
        return customAgreementNumber;
    }

    public void setCustomAgreementNumber(String customAgreementNumber) {
        this.customAgreementNumber = customAgreementNumber;
    }
}
