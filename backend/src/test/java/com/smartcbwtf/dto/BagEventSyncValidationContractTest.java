package com.smartcbwtf.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BagEventSyncValidationContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void collectedByUserIdIsOptionalBecauseAuthenticatedTokenIsAuthoritative() {
        BagEventSyncItem item = validItem();
        item.setCollectedByUserId(null);

        assertTrue(validator.validate(item).isEmpty());
    }

    @Test
    void bagEventSyncItemBoundsClientControlledFields() {
        assertFieldViolation(itemWith(q -> q.setQrCode("")), "qrCode");
        assertFieldViolation(itemWith(q -> q.setQrCode("x".repeat(4097))), "qrCode");
        assertFieldViolation(itemWith(q -> q.setEventType("PLANT_APPROVAL")), "eventType");
        assertTrue(validator.validate(itemWith(q -> q.setEventType("cbwtf_verification"))).isEmpty());
        assertFieldViolation(itemWith(q -> q.setGpsLat(91.0)), "gpsLat");
        assertFieldViolation(itemWith(q -> q.setGpsLon(181.0)), "gpsLon");
        assertFieldViolation(itemWith(q -> q.setWeightKg(BigDecimal.ZERO)), "weightKg");
        assertFieldViolation(itemWith(q -> q.setWeightKg(BigDecimal.valueOf(10000.1))), "weightKg");
        assertFieldViolation(itemWith(q -> q.setGpsAccuracyM(-1.0)), "gpsAccuracyM");
        assertFieldViolation(itemWith(q -> q.setGpsAccuracyM(5000.1)), "gpsAccuracyM");
        assertFieldViolation(itemWith(q -> q.setAppDeviceId("x".repeat(129))), "appDeviceId");
        assertFieldViolation(itemWith(q -> q.setNotes("x".repeat(1001))), "notes");
    }

    @Test
    void bagEventSyncRequestRequiresAtLeastOneValidEvent() {
        BagEventSyncRequest request = new BagEventSyncRequest();
        request.setEvents(List.of());

        assertFieldViolation(request, "events");

        request.setEvents(List.of(validItem()));
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void bagEventSyncRequestCapsBatchSize() {
        BagEventSyncRequest request = new BagEventSyncRequest();
        request.setEvents(java.util.stream.Stream.generate(this::validItem)
                .limit(501)
                .toList());

        assertFieldViolation(request, "events");
    }

    private BagEventSyncItem validItem() {
        BagEventSyncItem item = new BagEventSyncItem();
        item.setQrCode("{\"qrId\":\"abc\"}");
        item.setEventType("HCF_COLLECTION");
        item.setEventTs(Instant.now());
        item.setGpsLat(28.6140);
        item.setGpsLon(77.2091);
        item.setWeightKg(BigDecimal.valueOf(2.5));
        item.setGpsAccuracyM(8.25);
        item.setAppDeviceId("android-device-1");
        item.setNotes("ok");
        return item;
    }

    private BagEventSyncItem itemWith(java.util.function.Consumer<BagEventSyncItem> mutator) {
        BagEventSyncItem item = validItem();
        mutator.accept(item);
        return item;
    }

    private void assertFieldViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field + " on " + request.getClass().getSimpleName());
    }
}
