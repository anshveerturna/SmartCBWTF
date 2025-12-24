package com.smartcbwtf.exception;

import java.util.UUID;

/**
 * Exception thrown when agreement creation is blocked.
 * Contains machine-readable reason for UI/API consumption.
 */
public class AgreementBlockedException extends RuntimeException {

    public enum BlockReason {
        ACTIVE_AGREEMENT_EXISTS, // HCF has active agreement elsewhere
        UNPAID_DUES, // Previous dues not cleared
        DISPUTE_OPEN, // Open dispute on previous agreement
        BLACKLISTED // HCF on blacklist/watchlist
    }

    private final BlockReason reason;
    private final UUID blockingAgreementId;

    public AgreementBlockedException(BlockReason reason) {
        super(formatMessage(reason, null));
        this.reason = reason;
        this.blockingAgreementId = null;
    }

    public AgreementBlockedException(BlockReason reason, UUID blockingAgreementId) {
        super(formatMessage(reason, blockingAgreementId));
        this.reason = reason;
        this.blockingAgreementId = blockingAgreementId;
    }

    public BlockReason getReason() {
        return reason;
    }

    public UUID getBlockingAgreementId() {
        return blockingAgreementId;
    }

    private static String formatMessage(BlockReason reason, UUID agreementId) {
        return switch (reason) {
            case ACTIVE_AGREEMENT_EXISTS ->
                "HCF has an active agreement" + (agreementId != null ? " (ID: " + agreementId + ")" : "");
            case UNPAID_DUES ->
                "HCF has unpaid dues from previous agreement"
                        + (agreementId != null ? " (ID: " + agreementId + ")" : "");
            case DISPUTE_OPEN ->
                "HCF has an open dispute" + (agreementId != null ? " (ID: " + agreementId + ")" : "");
            case BLACKLISTED ->
                "HCF is blacklisted";
        };
    }
}
