package com.smartcbwtf.domain.enums;

import java.util.Arrays;

public enum TemplateCode {
    HCF_WELCOME(TemplateCategory.REGISTRATION),
    HCF_CREDENTIALS(TemplateCategory.REGISTRATION),
    AGREEMENT_SUBMITTED(TemplateCategory.REGISTRATION),
    AGREEMENT_APPROVED(TemplateCategory.REGISTRATION),
    AGREEMENT_REJECTED(TemplateCategory.REGISTRATION),

    INVOICE_GENERATED(TemplateCategory.BILLING),

    PAYMENT_REMINDER(TemplateCategory.PAYMENT),
    PAYMENT_OVERDUE(TemplateCategory.PAYMENT),
    PAYMENT_RECEIVED(TemplateCategory.PAYMENT),

    AGREEMENT_EXPIRY(TemplateCategory.COMPLIANCE);

    private final TemplateCategory category;

    TemplateCode(TemplateCategory category) {
        this.category = category;
    }

    public TemplateCategory getCategory() {
        return category;
    }
}
