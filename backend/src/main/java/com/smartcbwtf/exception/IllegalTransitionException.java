package com.smartcbwtf.exception;

import com.smartcbwtf.domain.Agreement;

/**
 * Exception thrown when attempting an illegal agreement status transition.
 */
public class IllegalTransitionException extends RuntimeException {

    private final Agreement.Status from;
    private final Agreement.Status to;

    public IllegalTransitionException(Agreement.Status from, Agreement.Status to) {
        super(String.format("Illegal agreement status transition: %s → %s", from, to));
        this.from = from;
        this.to = to;
    }

    public Agreement.Status getFrom() {
        return from;
    }

    public Agreement.Status getTo() {
        return to;
    }
}
