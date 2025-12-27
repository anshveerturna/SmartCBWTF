package com.smartcbwtf.dto;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.AgreementBillingConfig;
import com.smartcbwtf.domain.Hcf;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for HCF detail view in CBWTF admin portal.
 * Includes HCF profile, agreement details (read-only), billing config, and
 * summary.
 */
public class HcfDetailDTO {

    // HCF Profile
    private UUID id;
    private String code;
    private String name;
    private String address;
    private String contactPhone;
    private String contactEmail;
    private Integer numberOfBeds;
    private String hcfStatus;
    private Double gpsLat;
    private Double gpsLon;
    private String doctorName;
    private String panNo;
    private String gstNo;
    private String pcbAuthorizationNo;
    private String aadharNo;
    private BigDecimal monthlyCharges;
    private Boolean bedded;
    private String otherNotes;
    private Double registrationGpsLat;
    private Double registrationGpsLon;
    private Double registrationGpsAccuracy;
    private String registeredByUsername;
    private Instant createdAt;
    private Instant updatedAt;

    // Ownership information
    private String ownershipType;
    private String rentAgreementUrl;

    // Agreement (READ-ONLY)
    private AgreementInfo agreement;

    // Billing Config (editable if active)
    private BillingConfigInfo billingConfig;

    // Operational Summary
    private OperationalSummary summary;

    // Inner class for Agreement info
    public static class AgreementInfo {
        private UUID id;
        private String agreementNumber;
        private String status;
        private String duesStatus;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal perBedPerDayRate;
        private Instant createdAt;

        public static AgreementInfo from(Agreement agreement) {
            if (agreement == null)
                return null;
            AgreementInfo info = new AgreementInfo();
            info.id = agreement.getId();
            info.agreementNumber = agreement.getAgreementNumber();
            info.status = agreement.getStatus();
            info.duesStatus = agreement.getDuesStatus();
            info.startDate = agreement.getStartDate();
            info.endDate = agreement.getEndDate();
            info.perBedPerDayRate = agreement.getPerBedPerDayRate();
            info.createdAt = agreement.getCreatedAt();
            return info;
        }

        // Getters
        public UUID getId() {
            return id;
        }

        public String getAgreementNumber() {
            return agreementNumber;
        }

        public String getStatus() {
            return status;
        }

        public String getDuesStatus() {
            return duesStatus;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public BigDecimal getPerBedPerDayRate() {
            return perBedPerDayRate;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }
    }

    // Inner class for Billing Config
    public static class BillingConfigInfo {
        private UUID id;
        private Integer baseGramsPerBedPerDay;
        private BigDecimal baseRatePerBedPerDay;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private boolean active;

        // Global excess rate (from Facility)
        private BigDecimal globalExcessRatePerKg;
        private LocalDate globalExcessRateEffectiveFrom;

        public static BillingConfigInfo from(AgreementBillingConfig config, com.smartcbwtf.domain.Facility facility) {
            if (config == null)
                return null;
            BillingConfigInfo info = new BillingConfigInfo();
            info.id = config.getId();
            info.baseGramsPerBedPerDay = config.getBaseGramsPerBedPerDay();
            info.baseRatePerBedPerDay = config.getBaseRatePerBedPerDay();
            info.effectiveFrom = config.getEffectiveFrom();
            info.effectiveTo = config.getEffectiveTo();
            info.active = config.isActive();

            // Populate global excess rate from facility
            if (facility != null) {
                info.globalExcessRatePerKg = facility.getExcessRatePerKg();
                info.globalExcessRateEffectiveFrom = facility.getExcessRateEffectiveFrom();
            }
            return info;
        }

        // Getters
        public UUID getId() {
            return id;
        }

        public Integer getBaseGramsPerBedPerDay() {
            return baseGramsPerBedPerDay;
        }

        public BigDecimal getBaseRatePerBedPerDay() {
            return baseRatePerBedPerDay;
        }

        public LocalDate getEffectiveFrom() {
            return effectiveFrom;
        }

        public LocalDate getEffectiveTo() {
            return effectiveTo;
        }

        public boolean isActive() {
            return active;
        }

        public BigDecimal getGlobalExcessRatePerKg() {
            return globalExcessRatePerKg;
        }

        public LocalDate getGlobalExcessRateEffectiveFrom() {
            return globalExcessRateEffectiveFrom;
        }
    }

    // Inner class for Operational Summary
    public static class OperationalSummary {
        private int totalPickups;
        private int totalAttendanceMarks;
        private Instant lastPickupAt;
        private Instant lastAttendanceAt;
        private BigDecimal totalWasteKg;

        // Getters and Setters
        public int getTotalPickups() {
            return totalPickups;
        }

        public void setTotalPickups(int totalPickups) {
            this.totalPickups = totalPickups;
        }

        public int getTotalAttendanceMarks() {
            return totalAttendanceMarks;
        }

        public void setTotalAttendanceMarks(int totalAttendanceMarks) {
            this.totalAttendanceMarks = totalAttendanceMarks;
        }

        public Instant getLastPickupAt() {
            return lastPickupAt;
        }

        public void setLastPickupAt(Instant lastPickupAt) {
            this.lastPickupAt = lastPickupAt;
        }

        public Instant getLastAttendanceAt() {
            return lastAttendanceAt;
        }

        public void setLastAttendanceAt(Instant lastAttendanceAt) {
            this.lastAttendanceAt = lastAttendanceAt;
        }

        public BigDecimal getTotalWasteKg() {
            return totalWasteKg;
        }

        public void setTotalWasteKg(BigDecimal totalWasteKg) {
            this.totalWasteKg = totalWasteKg;
        }
    }

    // Static factory method
    public static HcfDetailDTO from(Hcf hcf, Agreement agreement, AgreementBillingConfig billingConfig,
            OperationalSummary summary) {
        HcfDetailDTO dto = new HcfDetailDTO();
        dto.id = hcf.getId();
        dto.code = hcf.getCode();
        dto.name = hcf.getName();
        dto.address = hcf.getAddress();
        dto.contactPhone = hcf.getContactPhone();
        dto.contactEmail = hcf.getContactEmail();
        dto.numberOfBeds = hcf.getNumberOfBeds();
        dto.hcfStatus = hcf.getStatus();
        dto.gpsLat = hcf.getGpsLat();
        dto.gpsLon = hcf.getGpsLon();
        dto.doctorName = hcf.getDoctorName();
        dto.panNo = hcf.getPanNo();
        dto.gstNo = hcf.getGstNo();
        dto.pcbAuthorizationNo = hcf.getPcbAuthorizationNo();
        dto.pcbAuthorizationNo = hcf.getPcbAuthorizationNo();
        dto.aadharNo = hcf.getAadharNo();
        dto.monthlyCharges = hcf.getMonthlyCharges();
        dto.bedded = hcf.getBedded();
        dto.otherNotes = hcf.getOtherNotes();
        dto.registrationGpsLat = hcf.getRegistrationGpsLat();
        dto.registrationGpsLon = hcf.getRegistrationGpsLon();
        dto.registrationGpsAccuracy = hcf.getRegistrationGpsAccuracy();
        if (hcf.getRegisteredByUser() != null) {
            dto.registeredByUsername = hcf.getRegisteredByUser().getUsername();
        }
        dto.createdAt = hcf.getCreatedAt();
        dto.updatedAt = hcf.getUpdatedAt();
        dto.ownershipType = hcf.getOwnershipType();
        dto.rentAgreementUrl = hcf.getRentAgreementUrl();
        dto.agreement = AgreementInfo.from(agreement);
        dto.billingConfig = BillingConfigInfo.from(billingConfig, agreement != null ? agreement.getFacility() : null);
        dto.summary = summary;
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

    public String getHcfStatus() {
        return hcfStatus;
    }

    public void setHcfStatus(String hcfStatus) {
        this.hcfStatus = hcfStatus;
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

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
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

    public String getPcbAuthorizationNo() {
        return pcbAuthorizationNo;
    }

    public void setPcbAuthorizationNo(String pcbAuthorizationNo) {
        this.pcbAuthorizationNo = pcbAuthorizationNo;
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

    public String getRegisteredByUsername() {
        return registeredByUsername;
    }

    public void setRegisteredByUsername(String registeredByUsername) {
        this.registeredByUsername = registeredByUsername;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public AgreementInfo getAgreement() {
        return agreement;
    }

    public void setAgreement(AgreementInfo agreement) {
        this.agreement = agreement;
    }

    public BillingConfigInfo getBillingConfig() {
        return billingConfig;
    }

    public void setBillingConfig(BillingConfigInfo billingConfig) {
        this.billingConfig = billingConfig;
    }

    public OperationalSummary getSummary() {
        return summary;
    }

    public void setSummary(OperationalSummary summary) {
        this.summary = summary;
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
}
