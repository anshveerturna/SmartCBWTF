package com.smartcbwtf.service;

import com.smartcbwtf.domain.AgreementNumberResetFrequency;
import com.smartcbwtf.domain.AgreementNumberSequence;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.AgreementNumberSequenceRepository;
import com.smartcbwtf.repository.AgreementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for generating unique agreement numbers atomically.
 *
 * Supports both the legacy format builder and a template-based format such as:
 * {{sequence}} {{month}} {{year}}
 */
@Service
public class AgreementNumberGeneratorService {

    private static final Pattern TEMPLATE_TOKEN_PATTERN = Pattern.compile("\\{\\{([a-zA-Z]+)\\}\\}");
    private static final int MAX_PREFIX_LENGTH = 20;
    private static final int MAX_SEPARATOR_LENGTH = 5;
    private static final int MAX_SEQUENCE_DIGITS = 10;
    private static final int MAX_TEMPLATE_LENGTH = 120;

    private final AgreementNumberSequenceRepository sequenceRepository;
    private final AgreementRepository agreementRepository;
    private final Clock clock;

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

    public AgreementNumberGeneratorService(
            AgreementNumberSequenceRepository sequenceRepository,
            AgreementRepository agreementRepository,
            Clock clock) {
        this.sequenceRepository = sequenceRepository;
        this.agreementRepository = agreementRepository;
        this.clock = clock;
    }

    @Transactional
    public String generateNextAgreementNumber(Facility facility) {
        return generateNextAgreementNumberWithSettings(facility, null, null, null, null, null, null, null);
    }

    @Transactional
    public String generateNextAgreementNumber(Facility facility, String customPrefix) {
        return generateNextAgreementNumberWithSettings(facility, customPrefix, null, null, null, null, null, null);
    }

    public String previewNextAgreementNumber(Facility facility) {
        return previewNextAgreementNumber(facility, null, null, null, null, null, null, null);
    }

    public String previewNextAgreementNumber(
            Facility facility,
            String customPrefix,
            String customSeparator,
            int customDigits,
            boolean customIncludeFacilityCode,
            boolean customIncludeYear) {
        return previewNextAgreementNumber(
                facility,
                customPrefix,
                customSeparator,
                customDigits,
                customIncludeFacilityCode,
                customIncludeYear,
                null,
                null);
    }

    public String previewNextAgreementNumber(
            Facility facility,
            String customPrefix,
            String customSeparator,
            Integer customDigits,
            Boolean customIncludeFacilityCode,
            Boolean customIncludeYear,
            String customTemplate,
            AgreementNumberResetFrequency resetFrequency) {
        AgreementNumberFormatSpec spec = resolveFormatSpec(
                customPrefix,
                customSeparator,
                customDigits,
                customIncludeFacilityCode,
                customIncludeYear,
                customTemplate,
                resetFrequency);

        LocalDate today = LocalDate.now(clock);
        AgreementSequencePeriod period = resolveSequencePeriod(today, spec.resetFrequency());
        int currentSequence = getCurrentSequenceNumber(facility, period, spec, today);
        return buildAgreementNumber(spec, facility.getCode(), today, currentSequence + 1);
    }

    @Transactional
    public String generateNextAgreementNumberWithSettings(
            Facility facility,
            String customPrefix,
            String customSeparator,
            Integer customDigits,
            Boolean customIncludeFacilityCode,
            Boolean customIncludeYear) {
        return generateNextAgreementNumberWithSettings(
                facility,
                customPrefix,
                customSeparator,
                customDigits,
                customIncludeFacilityCode,
                customIncludeYear,
                null,
                null);
    }

    @Transactional
    public String generateNextAgreementNumberWithSettings(
            Facility facility,
            String customPrefix,
            String customSeparator,
            Integer customDigits,
            Boolean customIncludeFacilityCode,
            Boolean customIncludeYear,
            String customTemplate,
            AgreementNumberResetFrequency resetFrequency) {
        AgreementNumberFormatSpec spec = resolveFormatSpec(
                customPrefix,
                customSeparator,
                customDigits,
                customIncludeFacilityCode,
                customIncludeYear,
                customTemplate,
                resetFrequency);

        LocalDate today = LocalDate.now(clock);
        AgreementSequencePeriod period = resolveSequencePeriod(today, spec.resetFrequency());

        AgreementNumberSequence sequence = sequenceRepository
                .findByFacilityIdAndYearAndPeriodMonthForUpdate(facility.getId(), period.year(), period.periodMonth())
                .orElseGet(() -> createNewSequence(
                        facility,
                        period.year(),
                        period.periodMonth(),
                        resolveExistingSequenceBaseline(facility, spec, today)));

        int nextSequence = findLowestAvailableSequenceNumber(facility, spec, today, sequence.getLastSequence());

        // Only increase the tracked max sequence if we aren't filling a gap
        if (nextSequence > sequence.getLastSequence()) {
            sequence.setLastSequence(nextSequence);
            sequence.setUpdatedAt(Instant.now(clock));
            sequenceRepository.save(sequence);
        }

        return buildAgreementNumber(spec, facility.getCode(), today, nextSequence);
    }

    public int getCurrentSequenceNumber(Facility facility, int year) {
        return sequenceRepository
                .findByFacilityIdAndYearAndPeriodMonth(facility.getId(), year, 0)
                .map(AgreementNumberSequence::getLastSequence)
                .orElse(0);
    }

    private int getCurrentSequenceNumber(
            Facility facility,
            AgreementSequencePeriod period,
            AgreementNumberFormatSpec spec,
            LocalDate today) {
        return sequenceRepository
                .findByFacilityIdAndYearAndPeriodMonth(facility.getId(), period.year(), period.periodMonth())
                .map(AgreementNumberSequence::getLastSequence)
                .orElseGet(() -> resolveExistingSequenceBaseline(facility, spec, today));
    }

    private AgreementNumberSequence createNewSequence(Facility facility, int year, int periodMonth, int lastSequence) {
        AgreementNumberSequence sequence = new AgreementNumberSequence();
        sequence.setFacility(facility);
        sequence.setYear(year);
        sequence.setPeriodMonth(periodMonth);
        sequence.setLastSequence(lastSequence);
        sequence.setCreatedAt(Instant.now(clock));
        sequence.setUpdatedAt(Instant.now(clock));
        return sequenceRepository.save(sequence);
    }

    private int resolveExistingSequenceBaseline(
            Facility facility,
            AgreementNumberFormatSpec spec,
            LocalDate today) {
        if (!canInferResetPeriodFromFormat(spec)) {
            return 0;
        }

        Pattern sequencePattern = buildAgreementNumberPattern(spec, facility.getCode(), today);
        List<String> agreementNumbers = agreementRepository.findAgreementNumbersByFacilityId(facility.getId());

        int maxSequence = 0;
        for (String agreementNumber : agreementNumbers) {
            Matcher matcher = sequencePattern.matcher(agreementNumber);
            if (matcher.matches()) {
                maxSequence = Math.max(maxSequence, Integer.parseInt(matcher.group(1)));
            }
        }
        return maxSequence;
    }

    private int findLowestAvailableSequenceNumber(Facility facility, AgreementNumberFormatSpec spec, LocalDate today, int currentMaxTracked) {
        if (!canInferResetPeriodFromFormat(spec)) {
            return currentMaxTracked + 1;
        }

        Pattern sequencePattern = buildAgreementNumberPattern(spec, facility.getCode(), today);
        List<String> agreementNumbers = agreementRepository.findAgreementNumbersByFacilityId(facility.getId());

        java.util.Set<Integer> usedSequences = new java.util.HashSet<>();
        int maxFound = currentMaxTracked;

        for (String agreementNumber : agreementNumbers) {
            Matcher matcher = sequencePattern.matcher(agreementNumber);
            if (matcher.matches()) {
                int seq = Integer.parseInt(matcher.group(1));
                usedSequences.add(seq);
                maxFound = Math.max(maxFound, seq);
            }
        }

        for (int i = 1; i <= maxFound; i++) {
            if (!usedSequences.contains(i)) {
                return i; // Found a gap!
            }
        }

        return maxFound + 1;
    }

    private boolean canInferResetPeriodFromFormat(AgreementNumberFormatSpec spec) {
        return switch (spec.resetFrequency()) {
            case NEVER -> true;
            case YEARLY -> spec.template() == null
                    ? spec.includeYear()
                    : spec.template().contains("{{year}}");
            case MONTHLY -> spec.template() != null
                    && spec.template().contains("{{month}}")
                    && spec.template().contains("{{year}}");
        };
    }

    private String buildAgreementNumber(
            AgreementNumberFormatSpec spec,
            String facilityCode,
            LocalDate today,
            int sequence) {
        if (spec.template() != null) {
            return renderTemplate(spec.template(), tokenValues(spec, facilityCode, today, sequence), spec.sequenceDigits(), false);
        }

        StringBuilder sb = new StringBuilder();

        if (spec.includeFacilityCode()) {
            sb.append(facilityCode.toUpperCase(Locale.ENGLISH));
            sb.append(spec.separator());
        }

        sb.append(spec.prefix());

        if (spec.includeYear()) {
            sb.append(spec.separator());
            sb.append(today.getYear());
        }

        sb.append(spec.separator());
        sb.append(String.format("%0" + spec.sequenceDigits() + "d", sequence));
        return sb.toString();
    }

    private Pattern buildAgreementNumberPattern(
            AgreementNumberFormatSpec spec,
            String facilityCode,
            LocalDate today) {
        if (spec.template() != null) {
            String regex = renderTemplate(spec.template(), tokenValues(spec, facilityCode, today, 1), spec.sequenceDigits(), true);
            return Pattern.compile("^" + regex + "$");
        }

        StringBuilder regex = new StringBuilder();
        if (spec.includeFacilityCode()) {
            regex.append(Pattern.quote(facilityCode.toUpperCase(Locale.ENGLISH)));
            regex.append(Pattern.quote(spec.separator()));
        }
        regex.append(Pattern.quote(spec.prefix()));
        if (spec.includeYear()) {
            regex.append(Pattern.quote(spec.separator()));
            regex.append(Pattern.quote(String.valueOf(today.getYear())));
        }
        regex.append(Pattern.quote(spec.separator()));
        regex.append("(\\d{").append(spec.sequenceDigits()).append("})");
        return Pattern.compile("^" + regex + "$");
    }

    private String renderTemplate(
            String template,
            Map<String, String> values,
            int digits,
            boolean regexMode) {
        Matcher matcher = TEMPLATE_TOKEN_PATTERN.matcher(template);
        StringBuilder output = new StringBuilder();
        int cursor = 0;

        while (matcher.find()) {
            String literal = template.substring(cursor, matcher.start());
            output.append(regexMode ? Pattern.quote(literal) : literal);

            String token = matcher.group(1);
            if ("sequence".equals(token)) {
                output.append(regexMode ? "(\\d{" + digits + "})" : values.get("sequence"));
            } else {
                String resolved = values.getOrDefault(token, matcher.group());
                output.append(regexMode ? Pattern.quote(resolved) : resolved);
            }
            cursor = matcher.end();
        }

        String trailing = template.substring(cursor);
        output.append(regexMode ? Pattern.quote(trailing) : trailing);
        return output.toString();
    }

    private Map<String, String> tokenValues(
            AgreementNumberFormatSpec spec,
            String facilityCode,
            LocalDate today,
            int sequence) {
        return Map.of(
                "facilityCode", facilityCode.toUpperCase(Locale.ENGLISH),
                "prefix", spec.prefix(),
                "year", String.valueOf(today.getYear()),
                "month", today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase(Locale.ENGLISH),
                "sequence", String.format("%0" + spec.sequenceDigits() + "d", sequence));
    }

    private AgreementSequencePeriod resolveSequencePeriod(LocalDate today, AgreementNumberResetFrequency resetFrequency) {
        return switch (resetFrequency) {
            case NEVER -> new AgreementSequencePeriod(0, 0);
            case YEARLY -> new AgreementSequencePeriod(today.getYear(), 0);
            case MONTHLY -> new AgreementSequencePeriod(today.getYear(), today.getMonthValue());
        };
    }

    private AgreementNumberFormatSpec resolveFormatSpec(
            String customPrefix,
            String customSeparator,
            Integer customDigits,
            Boolean customIncludeFacilityCode,
            Boolean customIncludeYear,
            String customTemplate,
            AgreementNumberResetFrequency customResetFrequency) {
        String effectiveTemplate = normalizeTemplate(customTemplate);
        if (effectiveTemplate != null && !effectiveTemplate.contains("{{sequence}}")) {
            throw new IllegalArgumentException("Agreement number template must include {{sequence}}");
        }

        String effectivePrefix = customPrefix != null ? customPrefix : prefix;
        String effectiveSeparator = customSeparator != null ? customSeparator : separator;
        int effectiveDigits = customDigits != null ? customDigits : sequenceDigits;
        validateFormatPart("Agreement number prefix", effectivePrefix, MAX_PREFIX_LENGTH);
        validateFormatPart("Agreement number separator", effectiveSeparator, MAX_SEPARATOR_LENGTH);
        validateFormatPart("Agreement number template", effectiveTemplate, MAX_TEMPLATE_LENGTH);
        if (effectiveDigits < 1 || effectiveDigits > MAX_SEQUENCE_DIGITS) {
            throw new IllegalArgumentException("Agreement number sequence digits must be between 1 and 10");
        }

        return new AgreementNumberFormatSpec(
                effectivePrefix,
                effectiveSeparator,
                effectiveDigits,
                customIncludeFacilityCode != null ? customIncludeFacilityCode : includeFacilityCode,
                customIncludeYear != null ? customIncludeYear : includeYear,
                effectiveTemplate,
                customResetFrequency != null ? customResetFrequency : AgreementNumberResetFrequency.YEARLY);
    }

    private String normalizeTemplate(String template) {
        if (template == null) {
            return null;
        }
        String trimmed = template.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateFormatPart(String label, String value, int maxLength) {
        if (value == null) {
            return;
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(label + " must be at most " + maxLength + " characters");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                throw new IllegalArgumentException(label + " cannot contain control characters");
            }
        }
    }

    private record AgreementNumberFormatSpec(
            String prefix,
            String separator,
            int sequenceDigits,
            boolean includeFacilityCode,
            boolean includeYear,
            String template,
            AgreementNumberResetFrequency resetFrequency) {
    }

    private record AgreementSequencePeriod(int year, int periodMonth) {
    }
}
