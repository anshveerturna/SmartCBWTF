package com.smartcbwtf.service;

import com.smartcbwtf.domain.ConsumableCategory;
import com.smartcbwtf.domain.ConsumableItem;
import com.smartcbwtf.domain.ConsumablePricing;
import com.smartcbwtf.domain.ConsumableQuantityReference;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.dto.AddConsumablePricingRequest;
import com.smartcbwtf.dto.CreateConsumableRequest;
import com.smartcbwtf.repository.ConsumableCategoryRepository;
import com.smartcbwtf.repository.ConsumableItemRepository;
import com.smartcbwtf.repository.ConsumablePricingRepository;
import com.smartcbwtf.repository.ConsumableQuantityReferenceRepository;
import com.smartcbwtf.repository.FacilityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumableServiceTest {

    private final ConsumableCategoryRepository categoryRepository = mock(ConsumableCategoryRepository.class);
    private final ConsumableItemRepository itemRepository = mock(ConsumableItemRepository.class);
    private final ConsumablePricingRepository pricingRepository = mock(ConsumablePricingRepository.class);
    private final ConsumableQuantityReferenceRepository referenceRepository = mock(
            ConsumableQuantityReferenceRepository.class);
    private final FacilityRepository facilityRepository = mock(FacilityRepository.class);
    private final ConsumableService service = new ConsumableService(
            categoryRepository,
            itemRepository,
            pricingRepository,
            referenceRepository,
            facilityRepository);

    @Test
    void addPricingRetiresCurrentPriceAndReturnsHistory() {
        UUID facilityId = UUID.randomUUID();
        UUID consumableId = UUID.randomUUID();
        ConsumableItem item = consumable(consumableId);

        ConsumablePricing oldPrice = pricing(item, "100.00", LocalDate.of(2026, 1, 1), true);
        ConsumablePricing newPrice = pricing(item, "125.00", LocalDate.of(2026, 2, 1), true);

        AddConsumablePricingRequest request = new AddConsumablePricingRequest();
        request.setPricePerUnit(new BigDecimal("125.00"));
        request.setGstRate(new BigDecimal("12.00"));
        request.setEffectiveFrom(LocalDate.of(2026, 2, 1));

        when(itemRepository.findByIdAndFacilityId(consumableId, facilityId)).thenReturn(Optional.of(item));
        when(pricingRepository.findActiveByConsumableItemId(consumableId))
                .thenReturn(Optional.of(oldPrice))
                .thenReturn(Optional.of(newPrice));
        when(pricingRepository.save(any(ConsumablePricing.class))).thenAnswer(invocation -> {
            ConsumablePricing saved = invocation.getArgument(0);
            if (saved.getPricePerUnit().compareTo(new BigDecimal("125.00")) == 0) {
                newPrice.setGstRate(saved.getGstRate());
                newPrice.setEffectiveFrom(saved.getEffectiveFrom());
            }
            return saved;
        });
        when(itemRepository.save(item)).thenReturn(item);
        when(pricingRepository.findByConsumableItemIdOrderByCreatedAtDesc(consumableId))
                .thenReturn(List.of(newPrice, oldPrice));

        var result = service.addPricing(consumableId, facilityId, request);

        assertFalse(oldPrice.getIsActive());
        assertEquals(LocalDate.of(2026, 1, 31), oldPrice.getEffectiveTo());
        assertEquals(new BigDecimal("125.00"), result.getActivePrice());
        assertEquals(new BigDecimal("12.00"), result.getActiveGstRate());
        assertEquals(2, result.getPricingHistory().size());
        verify(itemRepository, times(2)).findByIdAndFacilityId(consumableId, facilityId);
    }

    @Test
    void hasReferencedImageRequiresDatabaseImageReference() {
        UUID consumableId = UUID.randomUUID();
        ConsumableItem item = consumable(consumableId);
        item.setImageUrl("/api/cbwtf/consumables/" + consumableId + "/image/view");

        when(itemRepository.findById(consumableId)).thenReturn(Optional.of(item));

        assertTrue(service.hasReferencedImage(consumableId));
    }

    @Test
    void hasReferencedImageRejectsOrphanedFiles() {
        UUID consumableId = UUID.randomUUID();
        ConsumableItem item = consumable(consumableId);
        item.setImageUrl(null);

        when(itemRepository.findById(consumableId)).thenReturn(Optional.of(item));

        assertFalse(service.hasReferencedImage(consumableId));
    }

    @Test
    void requireFacilityItemRejectsItemsOutsideTenantScope() {
        UUID facilityId = UUID.randomUUID();
        UUID consumableId = UUID.randomUUID();
        when(itemRepository.findByIdAndFacilityId(consumableId, facilityId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.requireFacilityItem(consumableId, facilityId));
    }

    @Test
    void createRejectsCategoryOutsideTenantScopeBeforeSaving() {
        UUID facilityId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CreateConsumableRequest request = createRequest(categoryId);
        when(itemRepository.existsByFacilityIdAndConsumableCodeIgnoreCase(facilityId, "BAG_YELLOW"))
                .thenReturn(false);
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility(facilityId)));
        when(categoryRepository.findByIdAndFacilityId(categoryId, facilityId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.create(facilityId, request));

        verify(itemRepository, never()).save(any(ConsumableItem.class));
        verify(pricingRepository, never()).save(any(ConsumablePricing.class));
        verify(referenceRepository, never()).save(any(ConsumableQuantityReference.class));
    }

    @Test
    void createPersistsQuantityReferenceAndRequestedPriceEffectiveDate() {
        UUID facilityId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID consumableId = UUID.randomUUID();
        Facility facility = facility(facilityId);
        ConsumableCategory category = category(categoryId, facility);
        CreateConsumableRequest request = createRequest(categoryId);
        request.setInitialPrice(new BigDecimal("125.50"));
        request.setGstRate(new BigDecimal("12.00"));
        request.setPriceEffectiveFrom("2026-02-15");
        request.setReferenceType("FIXED");
        request.setReferenceQuantity(new BigDecimal("25.00"));

        when(itemRepository.existsByFacilityIdAndConsumableCodeIgnoreCase(facilityId, "BAG_YELLOW"))
                .thenReturn(false);
        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(categoryRepository.findByIdAndFacilityId(categoryId, facilityId)).thenReturn(Optional.of(category));
        when(itemRepository.save(any(ConsumableItem.class))).thenAnswer(invocation -> {
            ConsumableItem saved = invocation.getArgument(0);
            saved.setId(consumableId);
            return saved;
        });
        when(pricingRepository.findActiveByConsumableItemId(consumableId)).thenReturn(Optional.empty());
        when(referenceRepository.findByConsumableItemId(consumableId)).thenReturn(Optional.empty());
        when(referenceRepository.save(any(ConsumableQuantityReference.class))).thenAnswer(invocation -> {
            ConsumableQuantityReference saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        service.create(facilityId, request);

        ArgumentCaptor<ConsumablePricing> pricingCaptor = ArgumentCaptor.forClass(ConsumablePricing.class);
        verify(pricingRepository).save(pricingCaptor.capture());
        assertEquals(new BigDecimal("125.50"), pricingCaptor.getValue().getPricePerUnit());
        assertEquals(new BigDecimal("12.00"), pricingCaptor.getValue().getGstRate());
        assertEquals(LocalDate.of(2026, 2, 15), pricingCaptor.getValue().getEffectiveFrom());

        ArgumentCaptor<ConsumableQuantityReference> referenceCaptor = ArgumentCaptor
                .forClass(ConsumableQuantityReference.class);
        verify(referenceRepository).save(referenceCaptor.capture());
        assertEquals(ConsumableQuantityReference.ReferenceType.FIXED, referenceCaptor.getValue().getReferenceType());
        assertEquals(new BigDecimal("25.00"), referenceCaptor.getValue().getReferenceQuantity());
    }

    @Test
    void listCategoriesCountsActiveItemsWithSingleItemQuery() {
        UUID facilityId = UUID.randomUUID();
        Facility facility = facility(facilityId);
        ConsumableCategory bags = category(UUID.randomUUID(), facility);
        ConsumableCategory bins = category(UUID.randomUUID(), facility);
        bags.setName("Bags");
        bins.setName("Bins");
        ConsumableItem bagItem = consumable(UUID.randomUUID());
        bagItem.setCategory(bags);
        ConsumableItem binItem = consumable(UUID.randomUUID());
        binItem.setCategory(bins);

        when(categoryRepository.findActiveCategoriesByFacility(facilityId)).thenReturn(List.of(bags, bins));
        when(itemRepository.findActiveByFacility(facilityId)).thenReturn(List.of(bagItem, binItem));

        var result = service.listCategories(facilityId);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getItemCount());
        assertEquals(1, result.get(1).getItemCount());
        verify(itemRepository, times(1)).findActiveByFacility(facilityId);
    }

    @Test
    void listByFacilityBatchesActivePricingLookups() {
        UUID facilityId = UUID.randomUUID();
        ConsumableItem firstItem = consumable(UUID.randomUUID());
        ConsumableItem secondItem = consumable(UUID.randomUUID());
        ConsumablePricing firstPrice = pricing(firstItem, "10.00", LocalDate.of(2026, 1, 1), true);
        ConsumablePricing secondPrice = pricing(secondItem, "20.00", LocalDate.of(2026, 1, 1), true);
        when(itemRepository.findActiveByFacility(facilityId)).thenReturn(List.of(firstItem, secondItem));
        when(pricingRepository.findActiveByConsumableItemIdIn(List.of(firstItem.getId(), secondItem.getId())))
                .thenReturn(List.of(firstPrice, secondPrice));

        var result = service.listByFacility(facilityId, false);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("10.00"), result.get(0).getActivePrice());
        assertEquals(new BigDecimal("20.00"), result.get(1).getActivePrice());
        verify(pricingRepository).findActiveByConsumableItemIdIn(List.of(firstItem.getId(), secondItem.getId()));
        verify(pricingRepository, never()).findActiveByConsumableItemId(any(UUID.class));
    }

    private ConsumableItem consumable(UUID id) {
        ConsumableCategory category = category(UUID.randomUUID(), facility(UUID.randomUUID()));

        ConsumableItem item = new ConsumableItem();
        item.setId(id);
        item.setCategory(category);
        item.setConsumableCode("BAG_YELLOW");
        item.setName("Yellow bag");
        item.setUnitOfMeasure("Pcs");
        item.setIsActive(true);
        return item;
    }

    private ConsumablePricing pricing(ConsumableItem item, String price, LocalDate effectiveFrom, boolean active) {
        ConsumablePricing pricing = new ConsumablePricing();
        pricing.setId(UUID.randomUUID());
        pricing.setConsumableItem(item);
        pricing.setPricePerUnit(new BigDecimal(price));
        pricing.setGstRate(new BigDecimal("18.00"));
        pricing.setEffectiveFrom(effectiveFrom);
        pricing.setIsActive(active);
        return pricing;
    }

    private CreateConsumableRequest createRequest(UUID categoryId) {
        CreateConsumableRequest request = new CreateConsumableRequest();
        request.setCategoryId(categoryId.toString());
        request.setConsumableCode("BAG_YELLOW");
        request.setName("Yellow bag");
        request.setUnitOfMeasure("Pcs");
        return request;
    }

    private Facility facility(UUID id) {
        Facility facility = new Facility();
        facility.setId(id);
        facility.setName("Facility");
        return facility;
    }

    private ConsumableCategory category(UUID id, Facility facility) {
        ConsumableCategory category = new ConsumableCategory();
        category.setId(id);
        category.setFacility(facility);
        category.setName("Bags");
        category.setIsActive(true);
        return category;
    }
}
