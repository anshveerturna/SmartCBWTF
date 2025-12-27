package com.smartcbwtf.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for updating HCF profile.
 * Only editable fields are accepted.
 */
public class UpdateHcfRequest {

    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    private String name;

    @Email(message = "Invalid email format")
    private String contactEmail;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String contactPhone;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    private Integer numberOfBeds;

    private String doctorName;
    private String gstNo;
    private String panNo;
    private String aadharNo;
    private String pcbAuthorizationNo;
    private BigDecimal monthlyCharges;
    private Boolean bedded;
    private String otherNotes;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getNumberOfBeds() {
        return numberOfBeds;
    }

    public void setNumberOfBeds(Integer numberOfBeds) {
        this.numberOfBeds = numberOfBeds;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getGstNo() {
        return gstNo;
    }

    public void setGstNo(String gstNo) {
        this.gstNo = gstNo;
    }

    public String getPanNo() {
        return panNo;
    }

    public void setPanNo(String panNo) {
        this.panNo = panNo;
    }

    public String getAadharNo() {
        return aadharNo;
    }

    public void setAadharNo(String aadharNo) {
        this.aadharNo = aadharNo;
    }

    public String getPcbAuthorizationNo() {
        return pcbAuthorizationNo;
    }

    public void setPcbAuthorizationNo(String pcbAuthorizationNo) {
        this.pcbAuthorizationNo = pcbAuthorizationNo;
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

    public String getOtherNotes() {
        return otherNotes;
    }

    public void setOtherNotes(String otherNotes) {
        this.otherNotes = otherNotes;
    }
}
