package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.exception.IllegalTransitionException;
import com.smartcbwtf.repository.AgreementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing agreement status transitions.
 * Enforces strict transition rules to prevent resurrection of dead contracts.
 */
@Service
public class AgreementTransitionService {

    private static final Logger log = LoggerFactory.getLogger(AgreementTransitionService.class);

    // Allowed status transitions
    private static final Map<Agreement.Status, Set<Agreement.Status>> ALLOWED_TRANSITIONS = Map.of(
            Agreement.Status.ACTIVE, Set.of(
                    Agreement.Status.EXPIRED,
                    Agreement.Status.TERMINATED,
                    Agreement.Status.DISPUTED),
            Agreement.Status.DISPUTED, Set.of(
                    Agreement.Status.EXPIRED)
    // EXPIRED and TERMINATED are terminal states - no transitions allowed
    );

    private final AgreementRepository agreementRepo;
    private final AuditLogService auditLog;

    public AgreementTransitionService(AgreementRepository agreementRepo, AuditLogService auditLog) {
        this.agreementRepo = agreementRepo;
        this.auditLog = auditLog;
    }

    /**
     * Transition agreement to new status.
     * Validates transition is allowed before applying.
     */
    @Transactional
    public Agreement transition(UUID agreementId, Agreement.Status toStatus, String reason, UUID actorId) {
        Agreement agreement = agreementRepo.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found: " + agreementId));

        Agreement.Status fromStatus = agreement.getStatusEnum();

        // Validate transition
        assertValidTransition(fromStatus, toStatus);

        // Apply transition
        agreement.setStatusEnum(toStatus);

        // Set termination fields if applicable
        if (toStatus == Agreement.Status.TERMINATED) {
            agreement.setTerminationReason(reason);
            agreement.setTerminatedAt(Instant.now());
            agreement.setTerminatedBy(actorId);
        }

        Agreement saved = agreementRepo.save(agreement);

        // Audit log
        auditLog.log("AGREEMENT", agreementId, "AGREEMENT_STATUS_CHANGED", actorId,
                String.format("%s → %s (reason: %s)", fromStatus, toStatus, reason));

        log.info("Agreement {} transitioned: {} → {}", agreementId, fromStatus, toStatus);
        return saved;
    }

    /**
     * Assert that status transition is valid.
     * Throws IllegalTransitionException if not allowed.
     */
    public void assertValidTransition(Agreement.Status from, Agreement.Status to) {
        Set<Agreement.Status> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            log.warn("Illegal transition attempted: {} → {}", from, to);
            throw new IllegalTransitionException(from, to);
        }
    }

    /**
     * Check if transition is valid (non-throwing version).
     */
    public boolean isValidTransition(Agreement.Status from, Agreement.Status to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * Expire agreement (convenience method for scheduled jobs).
     */
    @Transactional
    public Agreement expire(UUID agreementId, UUID actorId) {
        return transition(agreementId, Agreement.Status.EXPIRED, "End date reached", actorId);
    }

    /**
     * Terminate agreement (convenience method for manual termination).
     */
    @Transactional
    public Agreement terminate(UUID agreementId, String reason, UUID actorId) {
        return transition(agreementId, Agreement.Status.TERMINATED, reason, actorId);
    }
}
