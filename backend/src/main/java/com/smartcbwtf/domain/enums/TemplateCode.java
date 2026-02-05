package com.smartcbwtf.domain.enums;

import java.util.Arrays;

public enum TemplateCode {
    // Registration & Onboarding
    CBWTF_WELCOME(TemplateCategory.REGISTRATION),
    HCF_WELCOME(TemplateCategory.REGISTRATION),
    HCF_REGISTRATION_RECEIVED(TemplateCategory.REGISTRATION),
    HCF_CREDENTIALS(TemplateCategory.REGISTRATION),
    HCF_APPROVED(TemplateCategory.REGISTRATION),
    HCF_REJECTED(TemplateCategory.REGISTRATION),
    STAFF_CREDENTIALS(TemplateCategory.REGISTRATION),

    // Agreement Lifecycle
    AGREEMENT_SUBMITTED(TemplateCategory.REGISTRATION),
    AGREEMENT_APPROVED(TemplateCategory.REGISTRATION),
    AGREEMENT_REJECTED(TemplateCategory.REGISTRATION),
    AGREEMENT_EXPIRY_WARNING(TemplateCategory.COMPLIANCE),
    AGREEMENT_RENEWED(TemplateCategory.COMPLIANCE),

    // Orders
    ORDER_PLACED_HCF(TemplateCategory.ORDER),
    ORDER_PLACED_CBWTF(TemplateCategory.ORDER),
    ORDER_STATUS_UPDATE(TemplateCategory.ORDER),
    ORDER_CANCELLED(TemplateCategory.ORDER),

    // Billing & Payments
    INVOICE_GENERATED(TemplateCategory.BILLING),
    PAYMENT_REMINDER(TemplateCategory.PAYMENT),
    PAYMENT_OVERDUE(TemplateCategory.PAYMENT),
    PAYMENT_RECEIVED(TemplateCategory.PAYMENT),
    BILL_ADJUSTMENT(TemplateCategory.BILLING),

    // Security
    PASSWORD_RESET(TemplateCategory.SECURITY),
    ACCOUNT_LOCKED(TemplateCategory.SECURITY),
    ACCOUNT_DEACTIVATED(TemplateCategory.SECURITY),
    NEW_USER_CREATED(TemplateCategory.SECURITY),

    // System
    SYSTEM_ALERT(TemplateCategory.SYSTEM);

    private final TemplateCategory category;

    TemplateCode(TemplateCategory category) {
        this.category = category;
    }

    public TemplateCategory getCategory() {
        return category;
    }
}
