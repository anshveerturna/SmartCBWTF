package com.smartcbwtf.dto;

import java.time.LocalDate;

public class AgreementVerificationDTO {
    private String status; // ACTIVE, EXPIRED, TERMINATED, INVALID
    private boolean valid;
    private String hcfName;
    private String hcfCode;
    private String hcfAddress;
    private String hcfState;
    private String hcfPincode;
    private String hcfCategory;
    private String hcfEmail;
    private String hcfDoctorName;
    private String hcfContactNumber;
    private Integer numberOfBeds;
    private String agreementNumber;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String facilityName;
    private String facilityAddress;
    private String facilityContact;
    private String billingModel;
    private java.time.Instant createdAt;

    // Constructors
    public AgreementVerificationDTO() {
    }

    public AgreementVerificationDTO(String status, boolean valid, String hcfName, String hcfCode, String hcfAddress,
            String hcfState, String hcfPincode, String hcfCategory, String hcfEmail, String hcfDoctorName,
            String hcfContactNumber, Integer numberOfBeds,
            String agreementNumber, LocalDate validFrom, LocalDate validUntil,
            String facilityName, String facilityAddress, String facilityContact, String billingModel,
            java.time.Instant createdAt) {
        this.status = status;
        this.valid = valid;
        this.hcfName = hcfName;
        this.hcfCode = hcfCode;
        this.hcfAddress = hcfAddress;
        this.hcfState = hcfState;
        this.hcfPincode = hcfPincode;
        this.hcfCategory = hcfCategory;
        this.hcfEmail = hcfEmail;
        this.hcfDoctorName = hcfDoctorName;
        this.hcfContactNumber = hcfContactNumber;
        this.numberOfBeds = numberOfBeds;
        this.agreementNumber = agreementNumber;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.facilityName = facilityName;
        this.facilityAddress = facilityAddress;
        this.facilityContact = facilityContact;
        this.billingModel = billingModel;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getHcfName() {
        return hcfName;
    }

    public void setHcfName(String hcfName) {
        this.hcfName = hcfName;
    }

    public String getHcfCode() {
        return hcfCode;
    }

    public void setHcfCode(String hcfCode) {
        this.hcfCode = hcfCode;
    }

    public String getHcfAddress() {
        return hcfAddress;
    }

    public void setHcfAddress(String hcfAddress) {
        this.hcfAddress = hcfAddress;
    }

    public String getHcfState() {
        return hcfState;
    }

    public void setHcfState(String hcfState) {
        this.hcfState = hcfState;
    }

    public String getHcfPincode() {
        return hcfPincode;
    }

    public void setHcfPincode(String hcfPincode) {
        this.hcfPincode = hcfPincode;
    }

    public String getHcfCategory() {
        return hcfCategory;
    }

    public void setHcfCategory(String hcfCategory) {
        this.hcfCategory = hcfCategory;
    }

    public String getHcfEmail() {
        return hcfEmail;
    }

    public void setHcfEmail(String hcfEmail) {
        this.hcfEmail = hcfEmail;
    }

    public String getHcfDoctorName() {
        return hcfDoctorName;
    }

    public void setHcfDoctorName(String hcfDoctorName) {
        this.hcfDoctorName = hcfDoctorName;
    }

    public String getHcfContactNumber() {
        return hcfContactNumber;
    }

    public void setHcfContactNumber(String hcfContactNumber) {
        this.hcfContactNumber = hcfContactNumber;
    }

    public Integer getNumberOfBeds() {
        return numberOfBeds;
    }

    public void setNumberOfBeds(Integer numberOfBeds) {
        this.numberOfBeds = numberOfBeds;
    }

    public String getAgreementNumber() {
        return agreementNumber;
    }

    public void setAgreementNumber(String agreementNumber) {
        this.agreementNumber = agreementNumber;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getFacilityAddress() {
        return facilityAddress;
    }

    public void setFacilityAddress(String facilityAddress) {
        this.facilityAddress = facilityAddress;
    }

    public String getFacilityContact() {
        return facilityContact;
    }

    public void setFacilityContact(String facilityContact) {
        this.facilityContact = facilityContact;
    }

    public String getBillingModel() {
        return billingModel;
    }

    public void setBillingModel(String billingModel) {
        this.billingModel = billingModel;
    }

    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
        this.createdAt = createdAt;
    }
}
