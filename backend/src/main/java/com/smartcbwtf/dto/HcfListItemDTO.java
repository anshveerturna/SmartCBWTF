package com.smartcbwtf.dto;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Hcf;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for HCF list view in CBWTF admin portal.
 */
public class HcfListItemDTO {

    private UUID id;
    private String code;
    private String name;
    private String address;
    private String contactPhone;
    private String contactEmail;
    private Integer numberOfBeds;

    // New filter fields
    private String city;
    private String state;
    private String hcfType;
    private String hcfTypeDisplay;
    private Integer seatCount;

    // Agreement info
    private UUID agreementId;
    private String agreementNumber;
    private String agreementStatus;
    private String duesStatus;
    private LocalDate agreementStartDate;
    private LocalDate agreementEndDate;

    // Bed Access Category (regulatory classification)
    private String bedAccessCategory;
    private String bedAccessCategoryDisplay;
    private boolean portalEligible;

    // Operational
    private Instant lastPickupAt;
    private Instant createdAt;

    // Static factory method
    public static HcfListItemDTO from(Hcf hcf, Agreement agreement, Instant lastPickupAt) {
        HcfListItemDTO dto = new HcfListItemDTO();
        dto.id = hcf.getId();
        dto.code = hcf.getCode();
        dto.name = hcf.getName();
        dto.address = hcf.getAddress();
        dto.contactPhone = hcf.getContactPhone();
        dto.contactEmail = hcf.getContactEmail();
        dto.numberOfBeds = hcf.getNumberOfBeds();
        dto.city = hcf.getCity();
        dto.state = hcf.getState();
        dto.seatCount = hcf.getSeatCount();
        if (hcf.getHcfType() != null) {
            dto.hcfType = hcf.getHcfType().name();
            dto.hcfTypeDisplay = hcf.getHcfType().getDisplayName();
        }
        dto.createdAt = hcf.getCreatedAt();
        dto.lastPickupAt = lastPickupAt;

        if (agreement != null) {
            dto.agreementId = agreement.getId();
            dto.agreementNumber = agreement.getAgreementNumber();
            dto.agreementStatus = agreement.getStatus();
            dto.duesStatus = agreement.getDuesStatus();
            dto.agreementStartDate = agreement.getStartDate();
            dto.agreementEndDate = agreement.getEndDate();
        }

        // Bed access category - null-safe mapping
        if (hcf.getBedAccessCategory() != null) {
            dto.bedAccessCategory = hcf.getBedAccessCategory().name();
            dto.bedAccessCategoryDisplay = hcf.getBedAccessCategory().getDisplayName();
            dto.portalEligible = hcf.isPortalEligible();
        } else {
            // Default for legacy HCFs without category set
            dto.bedAccessCategory = null;
            dto.bedAccessCategoryDisplay = null;
            dto.portalEligible = false;
        }

        return dto;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

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

    public Integer getNumberOfBeds() {
        return numberOfBeds;
    }

    public void setNumberOfBeds(Integer numberOfBeds) {
        this.numberOfBeds = numberOfBeds;
    }

    public UUID getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(UUID agreementId) {
        this.agreementId = agreementId;
    }

    public String getAgreementNumber() {
        return agreementNumber;
    }

    public void setAgreementNumber(String agreementNumber) {
        this.agreementNumber = agreementNumber;
    }

    public String getAgreementStatus() {
        return agreementStatus;
    }

    public void setAgreementStatus(String agreementStatus) {
        this.agreementStatus = agreementStatus;
    }

    public String getDuesStatus() {
        return duesStatus;
    }

    public void setDuesStatus(String duesStatus) {
        this.duesStatus = duesStatus;
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

    public Instant getLastPickupAt() {
        return lastPickupAt;
    }

    public void setLastPickupAt(Instant lastPickupAt) {
        this.lastPickupAt = lastPickupAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getBedAccessCategory() {
        return bedAccessCategory;
    }

    public void setBedAccessCategory(String bedAccessCategory) {
        this.bedAccessCategory = bedAccessCategory;
    }

    public String getBedAccessCategoryDisplay() {
        return bedAccessCategoryDisplay;
    }

    public void setBedAccessCategoryDisplay(String bedAccessCategoryDisplay) {
        this.bedAccessCategoryDisplay = bedAccessCategoryDisplay;
    }

    public boolean isPortalEligible() {
        return portalEligible;
    }

    public void setPortalEligible(boolean portalEligible) {
        this.portalEligible = portalEligible;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getHcfType() {
        return hcfType;
    }

    public void setHcfType(String hcfType) {
        this.hcfType = hcfType;
    }

    public String getHcfTypeDisplay() {
        return hcfTypeDisplay;
    }

    public void setHcfTypeDisplay(String hcfTypeDisplay) {
        this.hcfTypeDisplay = hcfTypeDisplay;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }
}
