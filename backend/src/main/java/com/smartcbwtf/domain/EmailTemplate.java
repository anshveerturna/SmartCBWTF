package com.smartcbwtf.domain;

/**
 * Email template codes - code-defined, versioned.
 * No user modification allowed.
 */
public enum EmailTemplate {
    HCF_WELCOME("hcf_welcome", 1, "Welcome to SmartCBWTF"),
    HCF_CREDENTIALS("hcf_credentials", 1, "Your Login Credentials"),
    AGREEMENT_EXPIRING("agreement_expiring", 1, "Agreement Expiring Soon"),
    INVOICE_GENERATED("invoice_generated", 1, "New Invoice Generated"),
    PAYMENT_REMINDER("payment_reminder", 1, "Payment Reminder"),
    PAYMENT_OVERDUE("payment_overdue", 1, "Payment Overdue Notice");

    private final String code;
    private final int version;
    private final String defaultSubject;

    EmailTemplate(String code, int version, String defaultSubject) {
        this.code = code;
        this.version = version;
        this.defaultSubject = defaultSubject;
    }

    public String getCode() {
        return code;
    }

    public int getVersion() {
        return version;
    }

    public String getDefaultSubject() {
        return defaultSubject;
    }

    public static EmailTemplate fromCode(String code) {
        for (EmailTemplate t : values()) {
            if (t.code.equals(code))
                return t;
        }
        throw new IllegalArgumentException("Unknown template: " + code);
    }
}
