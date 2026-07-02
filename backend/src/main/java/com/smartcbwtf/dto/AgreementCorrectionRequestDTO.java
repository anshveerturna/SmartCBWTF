package com.smartcbwtf.dto;

public class AgreementCorrectionRequestDTO {
    private String fieldName;
    private String requestedValue;
    private String reason;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getRequestedValue() {
        return requestedValue;
    }

    public void setRequestedValue(String requestedValue) {
        this.requestedValue = requestedValue;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
