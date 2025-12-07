package com.smartcbwtf.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class HcfRegistrationRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String address;

    @Email
    private String contactEmail;

    @NotBlank
    private String contactPhone;

    @NotNull
    private Integer numberOfBeds;

    @NotNull
    private Double gpsLat;

    @NotNull
    private Double gpsLon;

    // getters and setters
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
}
