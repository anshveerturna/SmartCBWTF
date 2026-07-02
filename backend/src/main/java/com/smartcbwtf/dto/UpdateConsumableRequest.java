package com.smartcbwtf.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateConsumableRequest {
    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    private static final String OPTIONAL_HSN_PATTERN = "^$|^[A-Za-z0-9.-]+$";

    @Pattern(regexp = UUID_PATTERN, message = "Category ID must be a valid UUID")
    private String categoryId;

    @Size(max = 200, message = "Name must be 200 characters or less")
    private String name;

    @Size(max = 2000, message = "Description must be 2000 characters or less")
    private String description;

    @Size(max = 20, message = "HSN code must be 20 characters or less")
    @Pattern(regexp = OPTIONAL_HSN_PATTERN, message = "HSN code contains invalid characters")
    private String hsnCode;

    @Size(max = 50, message = "Unit of measure must be 50 characters or less")
    private String unitOfMeasure;

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHsnCode() {
        return hsnCode;
    }

    public void setHsnCode(String hsnCode) {
        this.hsnCode = hsnCode;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
}
