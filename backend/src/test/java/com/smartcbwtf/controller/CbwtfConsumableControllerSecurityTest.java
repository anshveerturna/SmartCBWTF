package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.dto.CreateConsumableRequest;
import com.smartcbwtf.dto.ConsumableItemDTO;
import com.smartcbwtf.dto.UpdateConsumableRequest;
import com.smartcbwtf.service.ConsumableImageService;
import com.smartcbwtf.service.ConsumableService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CbwtfConsumableControllerSecurityTest {

    private final ConsumableService consumableService = mock(ConsumableService.class);
    private final ConsumableImageService imageService = mock(ConsumableImageService.class);
    private final CbwtfConsumableController controller = new CbwtfConsumableController(
            consumableService,
            imageService);

    @TempDir
    Path tempDir;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void uploadImageChecksFacilityOwnershipBeforeWritingFile() throws Exception {
        UUID facilityId = UUID.randomUUID();
        UUID consumableId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        MockMultipartFile file = new MockMultipartFile("file", "bag.png", "image/png", pngBytes());
        org.mockito.Mockito.doThrow(new EntityNotFoundException("Consumable not found"))
                .when(consumableService).requireFacilityItem(consumableId, facilityId);

        assertThrows(EntityNotFoundException.class, () -> controller.uploadImage(consumableId, file));

        verify(consumableService).requireFacilityItem(consumableId, facilityId);
        verify(imageService, never()).storeImage(consumableId, file);
    }

    @Test
    void deleteImageChecksFacilityOwnershipBeforeTouchingFiles() {
        UUID facilityId = UUID.randomUUID();
        UUID consumableId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        org.mockito.Mockito.doThrow(new EntityNotFoundException("Consumable not found"))
                .when(consumableService).requireFacilityItem(consumableId, facilityId);

        assertThrows(EntityNotFoundException.class, () -> controller.deleteImage(consumableId));

        verify(consumableService).requireFacilityItem(consumableId, facilityId);
        verifyNoInteractions(imageService);
    }

    @Test
    void publicImageViewAddsNoSniffHeaderAndShortCacheWindow() throws Exception {
        UUID consumableId = UUID.randomUUID();
        String filename = consumableId + ".png";
        Path imagePath = tempDir.resolve(filename);
        Files.write(imagePath, pngBytes());
        when(consumableService.hasReferencedImage(consumableId)).thenReturn(true);
        when(imageService.imageExists(filename)).thenReturn(true);
        when(imageService.getImagePath(filename)).thenReturn(imagePath);

        ResponseEntity<Resource> response = controller.viewImage(consumableId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("image/png", response.getHeaders().getContentType().toString());
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("max-age=86400", response.getHeaders().getCacheControl());
    }

    @Test
    void detailAndPricingHistoryAreNotCacheable() {
        UUID facilityId = UUID.randomUUID();
        UUID consumableId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        ConsumableItemDTO detail = new ConsumableItemDTO();
        when(consumableService.getDetail(consumableId, facilityId)).thenReturn(detail);
        when(consumableService.getPricingHistory(consumableId, facilityId))
                .thenReturn(List.of(new ConsumableItemDTO.PricingHistoryItem()));

        var detailResponse = controller.getConsumableDetail(consumableId);
        var pricingResponse = controller.getPricingHistory(consumableId);

        assertEquals("no-store", detailResponse.getHeaders().getCacheControl());
        assertEquals("no-cache", detailResponse.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertEquals("no-store", pricingResponse.getHeaders().getCacheControl());
        assertEquals("no-cache", pricingResponse.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    @Test
    void requestValidationRejectsUnsafeConsumablePayloads() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        CreateConsumableRequest create = new CreateConsumableRequest();
        create.setCategoryId("not-a-uuid");
        create.setConsumableCode("bad code");
        create.setName("");
        create.setDescription("x".repeat(2001));
        create.setHsnCode("bad hsn");
        create.setUnitOfMeasure("");
        create.setInitialPrice(new BigDecimal("-0.01"));
        create.setGstRate(new BigDecimal("101.00"));
        create.setPriceEffectiveFrom("01-02-2026");
        create.setReferenceType("PER_DAY");
        create.setReferenceQuantity(BigDecimal.ZERO);

        var createViolations = validator.validate(create);

        assertTrue(createViolations.stream().anyMatch(v -> "categoryId".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream()
                .anyMatch(v -> "consumableCode".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream().anyMatch(v -> "name".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream()
                .anyMatch(v -> "description".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream().anyMatch(v -> "hsnCode".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream()
                .anyMatch(v -> "unitOfMeasure".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream()
                .anyMatch(v -> "initialPrice".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream().anyMatch(v -> "gstRate".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream()
                .anyMatch(v -> "priceEffectiveFrom".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream()
                .anyMatch(v -> "referenceType".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream()
                .anyMatch(v -> "referenceQuantity".contentEquals(v.getPropertyPath().toString())));

        UpdateConsumableRequest update = new UpdateConsumableRequest();
        update.setCategoryId("not-a-uuid");
        update.setName("x".repeat(201));
        update.setHsnCode("bad hsn");

        var updateViolations = validator.validate(update);

        assertTrue(updateViolations.stream().anyMatch(v -> "categoryId".contentEquals(v.getPropertyPath().toString())));
        assertTrue(updateViolations.stream().anyMatch(v -> "name".contentEquals(v.getPropertyPath().toString())));
        assertTrue(updateViolations.stream().anyMatch(v -> "hsnCode".contentEquals(v.getPropertyPath().toString())));
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
    }
}
