package com.smartcbwtf.dto;

import java.io.Serializable;

public class ContactRequestDTO implements Serializable {

    private String name;
    private String email;
    private String phone;
    private String organization;
    private String message;

    public ContactRequestDTO() {
    }

    public ContactRequestDTO(String name, String email, String phone, String organization, String message) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.organization = organization;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
