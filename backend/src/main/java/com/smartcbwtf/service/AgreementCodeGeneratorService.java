package com.smartcbwtf.service;

import com.smartcbwtf.domain.AgreementCodeSequence;
import com.smartcbwtf.repository.AgreementCodeSequenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Service for generating human-readable agreement codes.
 * Format: AGR-YYYY-NNNNN (e.g., AGR-2024-00142)
 * 
 * Uses pessimistic locking for atomicity in concurrent environments.
 */
@Service
public class AgreementCodeGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(AgreementCodeGeneratorService.class);
    private static final String CODE_PREFIX = "AGR";

    private final AgreementCodeSequenceRepository sequenceRepo;

    public AgreementCodeGeneratorService(AgreementCodeSequenceRepository sequenceRepo) {
        this.sequenceRepo = sequenceRepo;
    }

    /**
     * Generate the next agreement code.
     * Thread-safe implementation using pessimistic locking.
     * 
     * @return Agreement code in format AGR-YYYY-NNNNN
     */
    @Transactional
    public String generateNextCode() {
        int currentYear = Year.now().getValue();

        // Get sequence with lock, or create if not exists
        AgreementCodeSequence sequence = sequenceRepo.findByYearForUpdate(currentYear)
                .orElseGet(() -> {
                    log.info("Creating new agreement code sequence for year {}", currentYear);
                    AgreementCodeSequence newSeq = new AgreementCodeSequence(currentYear, 0);
                    return sequenceRepo.save(newSeq);
                });

        // Increment and save
        int nextValue = sequence.incrementAndGet();
        sequenceRepo.save(sequence);

        // Format code
        String code = String.format("%s-%d-%05d", CODE_PREFIX, currentYear, nextValue);
        log.debug("Generated agreement code: {}", code);

        return code;
    }

    /**
     * Get the last generated sequence value for a year (for reporting).
     */
    public int getLastSequenceValue(int year) {
        return sequenceRepo.findById(year)
                .map(AgreementCodeSequence::getLastValue)
                .orElse(0);
    }
}
