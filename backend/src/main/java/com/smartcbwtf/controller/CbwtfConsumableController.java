package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.dto.ConsumableCategoryDTO;
import com.smartcbwtf.dto.ConsumableItemDTO;
import com.smartcbwtf.service.ConsumableImageService;
import com.smartcbwtf.service.ConsumableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Controller for CBWTF Admin Consumables Management.
 */
@RestController
@RequestMapping("/api/cbwtf/consumables")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfConsumableController {

    private static final Logger log = LoggerFactory.getLogger(CbwtfConsumableController.class);

    private final ConsumableService consumableService;
    private final ConsumableImageService imageService;

    public CbwtfConsumableController(ConsumableService consumableService, ConsumableImageService imageService) {
        this.consumableService = consumableService;
        this.imageService = imageService;
    }

    /**
     * List all consumables for the facility.
     */
    @GetMapping
    public ResponseEntity<List<ConsumableItemDTO>> listConsumables(
            @RequestParam(name = "includeInactive", required = false, defaultValue = "false") boolean includeInactive) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(consumableService.listByFacility(facilityId, includeInactive));
    }

    /**
     * Get consumable detail with pricing history.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConsumableItemDTO> getConsumableDetail(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(consumableService.getDetail(id, facilityId));
    }

    /**
     * List categories for the facility.
     */
    @GetMapping("/categories")
    public ResponseEntity<List<ConsumableCategoryDTO>> listCategories() {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(consumableService.listCategories(facilityId));
    }

    /**
     * Create a new consumable.
     */
    @PostMapping
    public ResponseEntity<ConsumableItemDTO> createConsumable(
            @RequestBody com.smartcbwtf.dto.CreateConsumableRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(consumableService.create(facilityId, request));
    }

    /**
     * Update an existing consumable.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConsumableItemDTO> updateConsumable(
            @PathVariable("id") UUID id,
            @RequestBody com.smartcbwtf.dto.UpdateConsumableRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        return ResponseEntity.ok(consumableService.update(id, facilityId, request));
    }

    /**
     * Deactivate a consumable (soft delete).
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateConsumable(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        consumableService.deactivate(id, facilityId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activate a consumable.
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateConsumable(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        consumableService.activate(id, facilityId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Upload consumable image.
     */
    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
    public ResponseEntity<ConsumableItemDTO> uploadImage(
            @PathVariable("id") UUID id,
            @RequestParam(name = "file") MultipartFile file) {
        UUID facilityId = TenantContext.getTenantId();

        try {
            // Store the actual file
            String filename = imageService.storeImage(id, file);

            // Build the URL to serve the image
            String imageUrl = "/api/cbwtf/consumables/" + id + "/image/view";
            log.info("Image upload for consumable {}: {} bytes, stored as {}", id, file.getSize(), filename);

            return ResponseEntity.ok(consumableService.updateImage(id, facilityId, imageUrl));
        } catch (IOException e) {
            log.error("Failed to store image for consumable {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete consumable image.
     */
    @DeleteMapping("/{id}/image")
    public ResponseEntity<ConsumableItemDTO> deleteImage(@PathVariable("id") UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        try {
            // Delete physical file
            // Try common extensions as stored in ImageService logic
            String[] extensions = { ".jpg", ".jpeg", ".png", ".gif", ".webp" };
            for (String ext : extensions) {
                String filename = id.toString() + ext;
                if (imageService.imageExists(filename)) {
                    imageService.deleteImage(filename);
                }
            }
            // Clear DB record
            return ResponseEntity.ok(consumableService.deleteImage(id, facilityId));
        } catch (IOException e) {
            log.error("Failed to delete image for consumable {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Serve consumable image.
     */
    @GetMapping("/{id}/image/view")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Resource> viewImage(@PathVariable("id") UUID id) {
        try {
            // Try common extensions
            String[] extensions = { ".jpg", ".jpeg", ".png", ".gif", ".webp" };
            for (String ext : extensions) {
                String filename = id.toString() + ext;
                if (imageService.imageExists(filename)) {
                    Path imagePath = imageService.getImagePath(filename);
                    Resource resource = new UrlResource(imagePath.toUri());

                    String contentType = "image/jpeg";
                    if (ext.equals(".png"))
                        contentType = "image/png";
                    else if (ext.equals(".gif"))
                        contentType = "image/gif";
                    else if (ext.equals(".webp"))
                        contentType = "image/webp";

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                            .body(resource);
                }
            }

            // No image found
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to serve image for consumable {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
