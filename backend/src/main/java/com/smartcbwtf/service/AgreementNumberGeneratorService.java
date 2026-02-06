package com.smartcbwtf.service;

import com.smartcbwtf.domain.AgreementNumberSequence;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.AgreementNumberSequenceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;

/**
 * Service for generating unique agreement numbers atomically.
 * 
 * Format is configurable via application properties:
 * - app.agreement.number.prefix: Middle prefix (default: "HCF")
 * - app.agreement.number.separator: Separator character (default: "-")
 * - app.agreement.number.sequence-digits: Number of digits for sequence (default: 5)
 * - app.agreement.number.include-facility-code: Whether to include facility code (default: true)
 * - app.agreement.number.include-year: Whether to include year (default: true)
 * 
 * Default format: <FACILITY_CODE>-HCF-<YYYY>-<5-digit-sequence>
 * Example: GUT-HCF-2025-00023
 */
@Service
public class AgreementNumberGeneratorService {

    private final AgreementNumberSequenceRepository sequenceRepository;
    
    @Value("${app.agreement.number.prefix:HCF}")
    private String prefix;
    
    @Value("${app.agreement.number.separator:-}")
    private String separator;
    
    @Value("${app.agreement.number.sequence-digits:5}")
    private int sequenceDigits;
    
    @Value("${app.agreement.number.include-facility-code:true}")
    private boolean includeFacilityCode;
    
    @Value("${app.agreement.number.include-year:true}")
    private boolean includeYear;

    public AgreementNumberGeneratorService(AgreementNumberSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Generate the next agreement number for a facility.
     * Uses pessimistic locking to ensure atomicity.
     *
     * @param facility The CBWTF facility
     * @return Unique agreement number
     */
    @Transactional
    public String generateNextAgreementNumber(Facility facility) {
        int currentYear = Year.now().getValue();
        
        // Try to find and lock existing sequence
        AgreementNumberSequence sequence = sequenceRepository
                .findByFacilityIdAndYearForUpdate(facility.getId(), currentYear)
                .orElseGet(() -> createNewSequence(facility, currentYear));

        // Increment sequence
        int nextSeq = sequence.getLastSequence() + 1;
        sequence.setLastSequence(nextSeq);
        sequence.setUpdatedAt(Instant.now());
        sequenceRepository.save(sequence);

        // Build agreement number based on configuration
        return buildAgreementNumber(facility.getCode(), currentYear, nextSeq);
    }
    
    /**
     * Build the agreement number string based on configuration.
     */
    private String buildAgreementNumber(String facilityCode, int year, int sequence) {
        StringBuilder sb = new StringBuilder();
        
        if (includeFacilityCode) {
            sb.append(facilityCode.toUpperCase());
            sb.append(separator);
        }
        
        sb.append(prefix);
        
        if (includeYear) {
            sb.append(separator);
            sb.append(year);
        }
        
        sb.append(separator);
        sb.append(String.format("%0" + sequenceDigits + "d", sequence));
        
        return sb.toString();
    }
    
    /**
     * Generate agreement number with custom format (overrides configuration).
     * Useful for facility-specific custom formats.
     * 
     * @param facility The CBWTF facility
     * @param customPrefix Custom prefix to use instead of configured one
     * @return Unique agreement number
     */
    @Transactional
    public String generateNextAgreementNumber(Facility facility, String customPrefix) {
        int currentYear = Year.now().getValue();
        
        AgreementNumberSequence sequence = sequenceRepository
                .findByFacilityIdAndYearForUpdate(facility.getId(), currentYear)
                .orElseGet(() -> createNewSequence(facility, currentYear));

        int nextSeq = sequence.getLastSequence() + 1;
        sequence.setLastSequence(nextSeq);
        sequence.setUpdatedAt(Instant.now());
        sequenceRepository.save(sequence);

        // Use custom prefix
        StringBuilder sb = new StringBuilder();
        if (includeFacilityCode) {
            sb.append(facility.getCode().toUpperCase());
            sb.append(separator);
        }
        sb.append(customPrefix);
        if (includeYear) {
            sb.append(separator);
            sb.append(currentYear);
        }
        sb.append(separator);
        sb.append(String.format("%0" + sequenceDigits + "d", nextSeq));
        
        return sb.toString();
    }

    /**
     * Create a new sequence entry for a facility/year combination.
     */
    private AgreementNumberSequence createNewSequence(Facility facility, int year) {
        AgreementNumberSequence sequence = new AgreementNumberSequence();
        sequence.setFacility(facility);
        sequence.setYear(year);
        sequence.setLastSequence(0);
        sequence.setCreatedAt(Instant.now());
        sequence.setUpdatedAt(Instant.now());
        return sequenceRepository.save(sequence);
    }

    /**
     * Get the current sequence number (for display purposes only).
     */
    public int getCurrentSequenceNumber(Facility facility, int year) {
        return sequenceRepository
                .findByFacilityIdAndYear(facility.getId(), year)
                .map(AgreementNumberSequence::getLastSequence)
                .orElse(0);
    }
    
    /**
     * Preview what the next agreement number would look like without actually generating it.
     */
    public String previewNextAgreementNumber(Facility facility) {
        int currentYear = Year.now().getValue();
        int nextSeq = getCurrentSequenceNumber(facility, currentYear) + 1;
        return buildAgreementNumber(facility.getCode(), currentYear, nextSeq);
    }

    /**
     * Preview next agreement number using per-facility format settings.
     */
    public String previewNextAgreementNumber(Facility facility,
            String customPrefix, String customSeparator, int customDigits,
            boolean customIncludeFacilityCode, boolean customIncludeYear) {
        int currentYear = Year.now().getValue();
        int nextSeq = getCurrentSequenceNumber(facility, currentYear) + 1;
        return buildCustomAgreementNumber(facility.getCode(), currentYear, nextSeq,
                customPrefix, customSeparator, customDigits, customIncludeFacilityCode, customIncludeYear);
    }

    /**
     * Generate next agreement number using per-facility format settings from FacilitySettings.
     * Falls back to application-level defaults if parameters are null.
     */
    @Transactional
    public String generateNextAgreementNumberWithSettings(Facility facility,
            String customPrefix, String customSeparator, Integer customDigits,
            Boolean customIncludeFacilityCode, Boolean customIncludeYear) {
        int currentYear = Year.now().getValue();

        AgreementNumberSequence sequence = sequenceRepository
                .findByFacilityIdAndYearForUpdate(facility.getId(), currentYear)
                .orElseGet(() -> createNewSequence(facility, currentYear));

        int nextSeq = sequence.getLastSequence() + 1;
        sequence.setLastSequence(nextSeq);
        sequence.setUpdatedAt(Instant.now());
        sequenceRepository.save(sequence);

        String effectivePrefix = customPrefix != null ? customPrefix : prefix;
        String effectiveSeparator = customSeparator != null ? customSeparator : separator;
        int effectiveDigits = customDigits != null ? customDigits : sequenceDigits;
        boolean effectiveIncludeFacilityCode = customIncludeFacilityCode != null ? customIncludeFacilityCode : includeFacilityCode;
        boolean effectiveIncludeYear = customIncludeYear != null ? customIncludeYear : includeYear;

        return buildCustomAgreementNumber(facility.getCode(), currentYear, nextSeq,
                effectivePrefix, effectiveSeparator, effectiveDigits,
                effectiveIncludeFacilityCode, effectiveIncludeYear);
    }

    /**
     * Build agreement number with custom format parameters.
     */
    private String buildCustomAgreementNumber(String facilityCode, int year, int sequence,
            String customPrefix, String customSeparator, int customDigits,
            boolean customIncludeFacilityCode, boolean customIncludeYear) {
        StringBuilder sb = new StringBuilder();

        if (customIncludeFacilityCode) {
            sb.append(facilityCode.toUpperCase());
            sb.append(customSeparator);
        }

        sb.append(customPrefix);

        if (customIncludeYear) {
            sb.append(customSeparator);
            sb.append(year);
        }

        sb.append(customSeparator);
        sb.append(String.format("%0" + customDigits + "d", sequence));

        return sb.toString();
    }
}
