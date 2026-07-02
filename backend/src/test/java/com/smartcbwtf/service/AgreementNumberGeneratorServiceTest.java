package com.smartcbwtf.service;

import com.smartcbwtf.domain.AgreementNumberResetFrequency;
import com.smartcbwtf.domain.AgreementNumberSequence;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.AgreementNumberSequenceRepository;
import com.smartcbwtf.repository.AgreementRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgreementNumberGeneratorServiceTest {

    @Test
    void previewUsesExistingAprilAgreementToReturnSecondAprilNumber() {
        AgreementNumberSequenceRepository sequenceRepository = mock(AgreementNumberSequenceRepository.class);
        AgreementRepository agreementRepository = mock(AgreementRepository.class);
        Facility facility = facility("GLOBAL");

        when(sequenceRepository.findByFacilityIdAndYearAndPeriodMonth(facility.getId(), 2026, 4))
                .thenReturn(Optional.empty());
        when(agreementRepository.findAgreementNumbersByFacilityId(facility.getId()))
                .thenReturn(List.of("001 APRIL 2026"));

        AgreementNumberGeneratorService service = new AgreementNumberGeneratorService(
                sequenceRepository,
                agreementRepository,
                fixedClock("2026-04-10T00:00:00Z"));

        String preview = service.previewNextAgreementNumber(
                facility,
                "",
                " ",
                3,
                false,
                false,
                "{{sequence}} {{month}} {{year}}",
                AgreementNumberResetFrequency.MONTHLY);

        assertEquals("002 APRIL 2026", preview);
    }

    @Test
    void previewInMarchIgnoresExistingAprilAgreementAndStartsAtOne() {
        AgreementNumberSequenceRepository sequenceRepository = mock(AgreementNumberSequenceRepository.class);
        AgreementRepository agreementRepository = mock(AgreementRepository.class);
        Facility facility = facility("GLOBAL");

        when(sequenceRepository.findByFacilityIdAndYearAndPeriodMonth(facility.getId(), 2026, 3))
                .thenReturn(Optional.empty());
        when(agreementRepository.findAgreementNumbersByFacilityId(facility.getId()))
                .thenReturn(List.of("001 APRIL 2026"));

        AgreementNumberGeneratorService service = new AgreementNumberGeneratorService(
                sequenceRepository,
                agreementRepository,
                fixedClock("2026-03-14T00:00:00Z"));

        String preview = service.previewNextAgreementNumber(
                facility,
                "",
                " ",
                3,
                false,
                false,
                "{{sequence}} {{month}} {{year}}",
                AgreementNumberResetFrequency.MONTHLY);

        assertEquals("001 MARCH 2026", preview);
    }

    @Test
    void generateResetsInMayAndStartsAtOne() {
        AgreementNumberSequenceRepository sequenceRepository = mock(AgreementNumberSequenceRepository.class);
        AgreementRepository agreementRepository = mock(AgreementRepository.class);
        Facility facility = facility("GLOBAL");

        when(sequenceRepository.findByFacilityIdAndYearAndPeriodMonthForUpdate(facility.getId(), 2026, 5))
                .thenReturn(Optional.empty());
        when(agreementRepository.findAgreementNumbersByFacilityId(facility.getId()))
                .thenReturn(List.of("001 APRIL 2026"));
        when(sequenceRepository.save(any(AgreementNumberSequence.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgreementNumberGeneratorService service = new AgreementNumberGeneratorService(
                sequenceRepository,
                agreementRepository,
                fixedClock("2026-05-02T00:00:00Z"));

        String generated = service.generateNextAgreementNumberWithSettings(
                facility,
                "",
                " ",
                3,
                false,
                false,
                "{{sequence}} {{month}} {{year}}",
                AgreementNumberResetFrequency.MONTHLY);

        assertEquals("001 MAY 2026", generated);
    }

    @Test
    void previewRejectsOversizedSequenceDigits() {
        AgreementNumberGeneratorService service = serviceWithNoExistingAgreements();

        assertThrows(IllegalArgumentException.class, () -> service.previewNextAgreementNumber(
                facility("GLOBAL"),
                "HCF",
                "-",
                100,
                true,
                true,
                null,
                AgreementNumberResetFrequency.YEARLY));
    }

    @Test
    void previewRejectsTemplateWithoutSequenceToken() {
        AgreementNumberGeneratorService service = serviceWithNoExistingAgreements();

        assertThrows(IllegalArgumentException.class, () -> service.previewNextAgreementNumber(
                facility("GLOBAL"),
                "HCF",
                "-",
                3,
                true,
                true,
                "{{month}} {{year}}",
                AgreementNumberResetFrequency.MONTHLY));
    }

    private AgreementNumberGeneratorService serviceWithNoExistingAgreements() {
        AgreementNumberSequenceRepository sequenceRepository = mock(AgreementNumberSequenceRepository.class);
        AgreementRepository agreementRepository = mock(AgreementRepository.class);
        when(agreementRepository.findAgreementNumbersByFacilityId(any()))
                .thenReturn(List.of());
        return new AgreementNumberGeneratorService(
                sequenceRepository,
                agreementRepository,
                fixedClock("2026-05-02T00:00:00Z"));
    }

    private Facility facility(String code) {
        Facility facility = new Facility();
        facility.setId(UUID.randomUUID());
        facility.setCode(code);
        return facility;
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }
}
