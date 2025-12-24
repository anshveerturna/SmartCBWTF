package com.smartcbwtf.exception;

import com.smartcbwtf.domain.Agreement;

import java.util.UUID;

/**
 * Exception thrown when an operation requires an active agreement but the
 * agreement is not active.
 */
public class AgreementNotActiveException extends RuntimeException {

    private final UUID agreementId;
    private final Agreement.Status currentStatus;

    public AgreementNotActiveException(UUID agreementId, Agreement.Status currentStatus) {
        super(String.format("Agreement %s is not active (current status: %s)", agreementId, currentStatus));
        this.agreementId = agreementId;
        this.currentStatus = currentStatus;
    }

    public AgreementNotActiveException(UUID agreementId, Agreement.Status currentStatus, String operation) {
        super(String.format("Operation '%s' requires active agreement. Agreement %s has status: %s",
                operation, agreementId, currentStatus));
        this.agreementId = agreementId;
        this.currentStatus = currentStatus;
    }

    public UUID getAgreementId() {
        return agreementId;
    }

    public Agreement.Status getCurrentStatus() {
        return currentStatus;
    }
}
