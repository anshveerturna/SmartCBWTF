package com.smartcbwtf.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public class ContactRequestDTO implements Serializable {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 180, message = "Email must be 180 characters or less")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^(?=(?:\\D*\\d){10,15}\\D*$)[+()\\-\\s0-9]+$", message = "Invalid phone number")
    @Size(max = 20, message = "Phone must be 20 characters or less")
    private String phone;

    @NotBlank(message = "Organization is required")
    @Size(min = 2, max = 180, message = "Organization must be between 2 and 180 characters")
    private String organization;

    @NotBlank(message = "Contact category is required")
    @Size(max = 80, message = "Contact category must be 80 characters or less")
    private String organizationType;

    @NotBlank(message = "Inquiry type is required")
    @Size(max = 80, message = "Inquiry type must be 80 characters or less")
    private String inquiryType;

    @NotBlank(message = "Message is required")
    @Size(min = 10, max = 2000, message = "Message must be between 10 and 2000 characters")
    private String message;

    @Size(max = 200, message = "Website must be 200 characters or less")
    private String website;

    public ContactRequestDTO() {
    }

    public ContactRequestDTO(
            String name,
            String email,
            String phone,
            String organization,
            String organizationType,
            String inquiryType,
            String message) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.organization = organization;
        this.organizationType = organizationType;
        this.inquiryType = inquiryType;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getOrganizationType() {
        return organizationType;
    }

    public void setOrganizationType(String organizationType) {
        this.organizationType = organizationType;
    }

    public String getInquiryType() {
        return inquiryType;
    }

    public void setInquiryType(String inquiryType) {
        this.inquiryType = inquiryType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}
