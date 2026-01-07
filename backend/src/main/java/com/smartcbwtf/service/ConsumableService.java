package com.smartcbwtf.service;

import com.smartcbwtf.domain.ConsumableCategory;
import com.smartcbwtf.domain.ConsumableItem;
import com.smartcbwtf.domain.ConsumablePricing;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.dto.ConsumableCategoryDTO;
import com.smartcbwtf.dto.ConsumableItemDTO;
import com.smartcbwtf.repository.ConsumableCategoryRepository;
import com.smartcbwtf.repository.ConsumableItemRepository;
import com.smartcbwtf.repository.ConsumablePricingRepository;
import com.smartcbwtf.repository.FacilityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConsumableService {

    private final ConsumableCategoryRepository categoryRepository;
    private final ConsumableItemRepository itemRepository;
    private final ConsumablePricingRepository pricingRepository;
    private final FacilityRepository facilityRepository;

    public ConsumableService(
            ConsumableCategoryRepository categoryRepository,
            ConsumableItemRepository itemRepository,
            ConsumablePricingRepository pricingRepository,
            FacilityRepository facilityRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.pricingRepository = pricingRepository;
        this.facilityRepository = facilityRepository;
    }

    @Transactional(readOnly = true)
    public List<ConsumableItemDTO> listByFacility(UUID facilityId, boolean includeInactive) {
        List<ConsumableItem> items = includeInactive
                ? itemRepository.findByFacilityIdOrderByName(facilityId)
                : itemRepository.findActiveByFacility(facilityId);

        return items.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public ConsumableItemDTO create(UUID facilityId, com.smartcbwtf.dto.CreateConsumableRequest request) {
        // Validate unique code
        if (itemRepository.existsByFacilityIdAndConsumableCodeIgnoreCase(facilityId, request.getConsumableCode())) {
            throw new IllegalArgumentException("Consumable code already exists: " + request.getConsumableCode());
        }

        // Get facility
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Facility not found: " + facilityId));

        // Get category
        ConsumableCategory category = categoryRepository.findById(UUID.fromString(request.getCategoryId()))
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + request.getCategoryId()));

        // Create item
        ConsumableItem item = new ConsumableItem();
        item.setFacility(facility);
        item.setCategory(category);
        item.setConsumableCode(request.getConsumableCode());
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setHsnCode(request.getHsnCode());
        item.setUnitOfMeasure(request.getUnitOfMeasure());
        item.setIsActive(true);

        ConsumableItem saved = itemRepository.save(item);

        // Create initial pricing if provided
        if (request.getInitialPrice() != null) {
            ConsumablePricing pricing = new ConsumablePricing();
            pricing.setConsumableItem(saved);
            pricing.setPricePerUnit(request.getInitialPrice());
            pricing.setGstRate(request.getGstRate() != null ? request.getGstRate() : new BigDecimal("18.00"));
            pricing.setEffectiveFrom(LocalDate.now());
            pricing.setIsActive(true);
            pricingRepository.save(pricing);
        }

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public ConsumableItemDTO getDetail(UUID id, UUID facilityId) {
        ConsumableItem item = itemRepository.findByIdAndFacilityId(id, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Consumable not found: " + id));

        ConsumableItemDTO dto = toDTO(item);
        List<ConsumablePricing> pricing = pricingRepository.findByConsumableItemIdOrderByCreatedAtDesc(id);
        dto.setPricingHistory(pricing.stream().map(this::toPricingDTO).collect(Collectors.toList()));

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ConsumableCategoryDTO> listCategories(UUID facilityId) {
        List<ConsumableCategory> categories = categoryRepository.findActiveCategoriesByFacility(facilityId);

        return categories.stream().map(cat -> {
            // Count items using repository instead of lazy loading
            long count = itemRepository.findActiveByFacility(facilityId).stream()
                    .filter(i -> i.getCategory() != null && i.getCategory().getId().equals(cat.getId()))
                    .count();

            ConsumableCategoryDTO dto = new ConsumableCategoryDTO();
            dto.setId(cat.getId().toString());
            dto.setName(cat.getName());
            dto.setDisplayOrder(cat.getDisplayOrder());
            dto.setIsActive(cat.getIsActive());
            dto.setCreatedAt(cat.getCreatedAt());
            dto.setItemCount((int) count);
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void deactivate(UUID id, UUID facilityId) {
        ConsumableItem item = itemRepository.findByIdAndFacilityId(id, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Consumable not found: " + id));
        item.setIsActive(false);
        itemRepository.save(item);
    }

    @Transactional
    public void activate(UUID id, UUID facilityId) {
        ConsumableItem item = itemRepository.findByIdAndFacilityId(id, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Consumable not found: " + id));
        item.setIsActive(true);
        itemRepository.save(item);
    }

    @Transactional
    public ConsumableItemDTO update(UUID id, UUID facilityId, com.smartcbwtf.dto.UpdateConsumableRequest request) {
        ConsumableItem item = itemRepository.findByIdAndFacilityId(id, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Consumable not found: " + id));

        // Update category if changed
        if (request.getCategoryId() != null && !request.getCategoryId().equals(item.getCategory().getId().toString())) {
            ConsumableCategory category = categoryRepository.findById(UUID.fromString(request.getCategoryId()))
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + request.getCategoryId()));
            item.setCategory(category);
        }

        if (request.getName() != null)
            item.setName(request.getName());
        if (request.getDescription() != null)
            item.setDescription(request.getDescription());
        if (request.getHsnCode() != null)
            item.setHsnCode(request.getHsnCode());
        if (request.getUnitOfMeasure() != null)
            item.setUnitOfMeasure(request.getUnitOfMeasure());

        ConsumableItem saved = itemRepository.save(item);
        return toDTO(saved);
    }

    @Transactional
    public ConsumableItemDTO updateImage(UUID id, UUID facilityId, String imageUrl) {
        ConsumableItem item = itemRepository.findByIdAndFacilityId(id, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Consumable not found: " + id));
        item.setImageUrl(imageUrl);
        item.setUpdatedAt(java.time.LocalDateTime.now()); // Force update for cache busting
        itemRepository.save(item);
        return toDTO(item);
    }

    @Transactional
    public ConsumableItemDTO deleteImage(UUID id, UUID facilityId) {
        ConsumableItem item = itemRepository.findByIdAndFacilityId(id, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Consumable not found: " + id));

        // Remove physical file implicitly handled by controller via calling
        // ImageService directly?
        // No, better to encapsulate here if possible, but ImageService is in Controller
        // scope.
        // Actually, Controller knows about ImageService. Let's let Controller handle
        // the physical file
        // using the Service's lookup, or pass ImageService into ConsumableService.
        // Given current architecture, Controller calls ImageService.storeImage.
        // So Controller should probably call ImageService.deleteImage.
        // Use this method to just clear the DB record.

        item.setImageUrl(null);
        item.setUpdatedAt(java.time.LocalDateTime.now());
        itemRepository.save(item);
        return toDTO(item);
    }

    private ConsumableItemDTO toDTO(ConsumableItem item) {
        ConsumablePricing activePrice = pricingRepository.findActiveByConsumableItemId(item.getId()).orElse(null);

        ConsumableItemDTO dto = new ConsumableItemDTO();
        dto.setId(item.getId().toString());
        dto.setConsumableCode(item.getConsumableCode());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setHsnCode(item.getHsnCode());
        dto.setUnitOfMeasure(item.getUnitOfMeasure());
        dto.setImageUrl(item.getImageUrl());
        dto.setIsActive(item.getIsActive());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        dto.setCategoryId(item.getCategory().getId().toString());
        dto.setCategoryName(item.getCategory().getName());

        if (activePrice != null) {
            dto.setActivePrice(activePrice.getPricePerUnit());
            dto.setActiveGstRate(activePrice.getGstRate());
            dto.setPriceEffectiveFrom(activePrice.getEffectiveFrom());
        }

        return dto;
    }

    private ConsumableItemDTO.PricingHistoryItem toPricingDTO(ConsumablePricing pricing) {
        ConsumableItemDTO.PricingHistoryItem dto = new ConsumableItemDTO.PricingHistoryItem();
        dto.setId(pricing.getId().toString());
        dto.setPricePerUnit(pricing.getPricePerUnit());
        dto.setGstRate(pricing.getGstRate());
        dto.setEffectiveFrom(pricing.getEffectiveFrom());
        dto.setEffectiveTo(pricing.getEffectiveTo());
        dto.setIsActive(pricing.getIsActive());
        dto.setCreatedAt(pricing.getCreatedAt());
        return dto;
    }
}
