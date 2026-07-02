package com.smartcbwtf.service;

import com.smartcbwtf.domain.ConsumableCategory;
import com.smartcbwtf.domain.ConsumableItem;
import com.smartcbwtf.domain.ConsumablePricing;
import com.smartcbwtf.domain.ConsumableQuantityReference;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.dto.ConsumableCategoryDTO;
import com.smartcbwtf.dto.ConsumableItemDTO;
import com.smartcbwtf.repository.ConsumableCategoryRepository;
import com.smartcbwtf.repository.ConsumableItemRepository;
import com.smartcbwtf.repository.ConsumablePricingRepository;
import com.smartcbwtf.repository.ConsumableQuantityReferenceRepository;
import com.smartcbwtf.repository.FacilityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConsumableService {

    private static final String CONSUMABLE_CODE_PATTERN = "^[A-Za-z0-9._-]+$";
    private static final String OPTIONAL_HSN_PATTERN = "^[A-Za-z0-9.-]+$";
    private static final BigDecimal GST_MIN = new BigDecimal("0.00");
    private static final BigDecimal GST_MAX = new BigDecimal("100.00");

    private final ConsumableCategoryRepository categoryRepository;
    private final ConsumableItemRepository itemRepository;
    private final ConsumablePricingRepository pricingRepository;
    private final ConsumableQuantityReferenceRepository referenceRepository;
    private final FacilityRepository facilityRepository;

    public ConsumableService(
            ConsumableCategoryRepository categoryRepository,
            ConsumableItemRepository itemRepository,
            ConsumablePricingRepository pricingRepository,
            ConsumableQuantityReferenceRepository referenceRepository,
            FacilityRepository facilityRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.pricingRepository = pricingRepository;
        this.referenceRepository = referenceRepository;
        this.facilityRepository = facilityRepository;
    }

    @Transactional(readOnly = true)
    public List<ConsumableItemDTO> listByFacility(UUID facilityId, boolean includeInactive) {
        List<ConsumableItem> items = includeInactive
                ? itemRepository.findByFacilityIdOrderByName(facilityId)
                : itemRepository.findActiveByFacility(facilityId);
        Map<UUID, ConsumablePricing> activePricingByItemId = activePricingByItemId(items);

        return items.stream()
                .map(item -> toDTO(item, activePricingByItemId.get(item.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public ConsumableItemDTO create(UUID facilityId, com.smartcbwtf.dto.CreateConsumableRequest request) {
        String consumableCode = cleanLineRequired(request.getConsumableCode(), "Consumable code", 50);
        requirePattern(consumableCode, CONSUMABLE_CODE_PATTERN, "Consumable code contains invalid characters");
        String name = cleanLineRequired(request.getName(), "Name", 200);
        String description = optionalCleanText(request.getDescription(), "Description", 2000);
        String hsnCode = optionalCleanLine(request.getHsnCode(), "HSN code", 20);
        if (hsnCode != null) {
            requirePattern(hsnCode, OPTIONAL_HSN_PATTERN, "HSN code contains invalid characters");
        }
        String unitOfMeasure = cleanLineRequired(request.getUnitOfMeasure(), "Unit of measure", 50);
        requireNonNegative(request.getInitialPrice(), "Initial price");
        requireBetween(request.getGstRate(), "GST rate", GST_MIN, GST_MAX);

        // Validate unique code
        if (itemRepository.existsByFacilityIdAndConsumableCodeIgnoreCase(facilityId, consumableCode)) {
            throw new IllegalArgumentException("Consumable code already exists: " + consumableCode);
        }

        // Get facility
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Facility not found: " + facilityId));

        ConsumableCategory category = getFacilityCategory(facilityId, request.getCategoryId());

        // Create item
        ConsumableItem item = new ConsumableItem();
        item.setFacility(facility);
        item.setCategory(category);
        item.setConsumableCode(consumableCode);
        item.setName(name);
        item.setDescription(description);
        item.setHsnCode(hsnCode);
        item.setUnitOfMeasure(unitOfMeasure);
        item.setIsActive(true);

        ConsumableItem saved = itemRepository.save(item);

        // Create initial pricing if provided
        if (request.getInitialPrice() != null) {
            ConsumablePricing pricing = new ConsumablePricing();
            pricing.setConsumableItem(saved);
            pricing.setPricePerUnit(request.getInitialPrice());
            pricing.setGstRate(request.getGstRate() != null ? request.getGstRate() : new BigDecimal("18.00"));
            pricing.setEffectiveFrom(parseEffectiveFrom(request.getPriceEffectiveFrom()));
            pricing.setIsActive(true);
            pricingRepository.save(pricing);
        }

        upsertQuantityReference(saved, request.getReferenceType(), request.getReferenceQuantity());
        return toDTOWithReference(saved);
    }

    @Transactional(readOnly = true)
    public ConsumableItemDTO getDetail(UUID id, UUID facilityId) {
        ConsumableItem item = itemRepository.findByIdAndFacilityId(id, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Consumable not found: " + id));

        ConsumableItemDTO dto = toDTO(item);
        List<ConsumablePricing> pricing = pricingRepository.findByConsumableItemIdOrderByCreatedAtDesc(id);
        dto.setPricingHistory(pricing.stream().map(this::toPricingDTO).collect(Collectors.toList()));
        applyQuantityReference(dto, id);

        return dto;
    }

    @Transactional(readOnly = true)
    public void requireFacilityItem(UUID id, UUID facilityId) {
        itemRepository.findByIdAndFacilityId(id, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Consumable not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ConsumableCategoryDTO> listCategories(UUID facilityId) {
        List<ConsumableCategory> categories = categoryRepository.findActiveCategoriesByFacility(facilityId);
        List<ConsumableItem> activeItems = itemRepository.findActiveByFacility(facilityId);

        return categories.stream().map(cat -> {
            long count = activeItems.stream()
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
            ConsumableCategory category = getFacilityCategory(facilityId, request.getCategoryId());
            item.setCategory(category);
        }

        if (request.getName() != null)
            item.setName(cleanLineRequired(request.getName(), "Name", 200));
        if (request.getDescription() != null)
            item.setDescription(optionalCleanText(request.getDescription(), "Description", 2000));
        if (request.getHsnCode() != null) {
            String hsnCode = optionalCleanLine(request.getHsnCode(), "HSN code", 20);
            if (hsnCode != null) {
                requirePattern(hsnCode, OPTIONAL_HSN_PATTERN, "HSN code contains invalid characters");
            }
            item.setHsnCode(hsnCode);
        }
        if (request.getUnitOfMeasure() != null)
            item.setUnitOfMeasure(cleanLineRequired(request.getUnitOfMeasure(), "Unit of measure", 50));

        ConsumableItem saved = itemRepository.save(item);
        return toDTOWithReference(saved);
    }

    @Transactional
    public ConsumableItemDTO addPricing(UUID id, UUID facilityId,
            com.smartcbwtf.dto.AddConsumablePricingRequest request) {
        ConsumableItem item = itemRepository.findByIdAndFacilityId(id, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Consumable not found: " + id));

        LocalDate effectiveFrom = request.getEffectiveFrom() != null ? request.getEffectiveFrom() : LocalDate.now();
        BigDecimal gstRate = request.getGstRate() != null ? request.getGstRate() : new BigDecimal("18.00");

        pricingRepository.findActiveByConsumableItemId(id).ifPresent(active -> {
            active.setIsActive(false);
            LocalDate effectiveTo = effectiveFrom.minusDays(1);
            if (active.getEffectiveFrom() != null && effectiveTo.isBefore(active.getEffectiveFrom())) {
                effectiveTo = active.getEffectiveFrom();
            }
            active.setEffectiveTo(effectiveTo);
            pricingRepository.save(active);
        });

        ConsumablePricing pricing = new ConsumablePricing();
        pricing.setConsumableItem(item);
        pricing.setPricePerUnit(request.getPricePerUnit());
        pricing.setGstRate(gstRate);
        pricing.setEffectiveFrom(effectiveFrom);
        pricing.setIsActive(true);
        pricingRepository.save(pricing);

        item.setUpdatedAt(java.time.LocalDateTime.now());
        itemRepository.save(item);
        return getDetail(id, facilityId);
    }

    @Transactional(readOnly = true)
    public List<ConsumableItemDTO.PricingHistoryItem> getPricingHistory(UUID id, UUID facilityId) {
        itemRepository.findByIdAndFacilityId(id, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Consumable not found: " + id));

        return pricingRepository.findByConsumableItemIdOrderByCreatedAtDesc(id)
                .stream()
                .map(this::toPricingDTO)
                .collect(Collectors.toList());
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

    @Transactional(readOnly = true)
    public boolean hasReferencedImage(UUID id) {
        return itemRepository.findById(id)
                .map(item -> ("/api/cbwtf/consumables/" + id + "/image/view").equals(item.getImageUrl()))
                .orElse(false);
    }

    private ConsumableCategory getFacilityCategory(UUID facilityId, String categoryIdValue) {
        UUID categoryId = parseUuid(cleanLineRequired(categoryIdValue, "Category", 36), "Category ID");
        return categoryRepository.findByIdAndFacilityId(categoryId, facilityId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId));
    }

    private LocalDate parseEffectiveFrom(String value) {
        String cleaned = optionalCleanLine(value, "Price effective date", 10);
        if (cleaned == null) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(cleaned);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Price effective date must be yyyy-MM-dd");
        }
    }

    private void upsertQuantityReference(ConsumableItem item, String referenceTypeValue, BigDecimal referenceQuantity) {
        String referenceType = optionalCleanLine(referenceTypeValue, "Reference type", 30);
        if (referenceType == null && referenceQuantity == null) {
            return;
        }
        if (referenceType == null || referenceQuantity == null) {
            throw new IllegalArgumentException("Reference type and quantity must be provided together");
        }
        requirePositive(referenceQuantity, "Reference quantity");

        ConsumableQuantityReference reference = referenceRepository.findByConsumableItemId(item.getId())
                .orElseGet(ConsumableQuantityReference::new);
        reference.setConsumableItem(item);
        reference.setReferenceType(parseReferenceType(referenceType));
        reference.setReferenceQuantity(referenceQuantity);
        referenceRepository.save(reference);
    }

    private ConsumableQuantityReference.ReferenceType parseReferenceType(String referenceType) {
        try {
            return ConsumableQuantityReference.ReferenceType.valueOf(referenceType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Reference type must be PER_100_BEDS_PER_YEAR, PER_MONTH, or FIXED");
        }
    }

    private ConsumableItemDTO toDTOWithReference(ConsumableItem item) {
        ConsumableItemDTO dto = toDTO(item);
        applyQuantityReference(dto, item.getId());
        return dto;
    }

    private void applyQuantityReference(ConsumableItemDTO dto, UUID itemId) {
        referenceRepository.findByConsumableItemId(itemId).ifPresent(reference -> {
            dto.setReferenceType(reference.getReferenceType().name());
            dto.setReferenceQuantity(reference.getReferenceQuantity());
            dto.setReferenceDisplayText(referenceDisplayText(reference));
        });
    }

    private String referenceDisplayText(ConsumableQuantityReference reference) {
        String quantity = reference.getReferenceQuantity().stripTrailingZeros().toPlainString();
        return switch (reference.getReferenceType()) {
            case PER_100_BEDS_PER_YEAR -> quantity + " per 100 beds/year";
            case PER_MONTH -> quantity + " per month";
            case FIXED -> quantity + " fixed quantity";
        };
    }

    private static UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid UUID");
        }
    }

    private static String cleanLineRequired(String value, String fieldName, int maxLength) {
        String cleaned = cleanLine(value);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        requireMaxLength(cleaned, fieldName, maxLength);
        return cleaned;
    }

    private static String optionalCleanLine(String value, String fieldName, int maxLength) {
        String cleaned = cleanLine(value);
        if (cleaned.isBlank()) {
            return null;
        }
        requireMaxLength(cleaned, fieldName, maxLength);
        return cleaned;
    }

    private static String optionalCleanText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        if (cleaned.isBlank()) {
            return null;
        }
        requireMaxLength(cleaned, fieldName, maxLength);
        return cleaned;
    }

    private static String cleanLine(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("[\\r\\n\\t]+", " ");
    }

    private static void requireMaxLength(String value, String fieldName, int maxLength) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or less");
        }
    }

    private static void requirePattern(String value, String pattern, String message) {
        if (!value.matches(pattern)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNonNegative(BigDecimal value, String fieldName) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }

    private static void requirePositive(BigDecimal value, String fieldName) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    private static void requireBetween(BigDecimal value, String fieldName, BigDecimal min, BigDecimal max) {
        if (value != null && (value.compareTo(min) < 0 || value.compareTo(max) > 0)) {
            throw new IllegalArgumentException(fieldName + " must be between " + min + " and " + max);
        }
    }

    private ConsumableItemDTO toDTO(ConsumableItem item) {
        ConsumablePricing activePrice = pricingRepository.findActiveByConsumableItemId(item.getId()).orElse(null);
        return toDTO(item, activePrice);
    }

    private ConsumableItemDTO toDTO(ConsumableItem item, ConsumablePricing activePrice) {
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

    private Map<UUID, ConsumablePricing> activePricingByItemId(List<ConsumableItem> items) {
        if (items.isEmpty()) {
            return Map.of();
        }
        return pricingRepository.findActiveByConsumableItemIdIn(items.stream().map(ConsumableItem::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        pricing -> pricing.getConsumableItem().getId(),
                        pricing -> pricing,
                        (first, ignored) -> first));
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
