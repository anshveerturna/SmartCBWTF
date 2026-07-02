package com.smartcbwtf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for storing and retrieving consumable images.
 * Uses local file system storage. In production, this should be replaced with
 * S3/GCS.
 */
@Service
public class ConsumableImageService {

    private static final Logger log = LoggerFactory.getLogger(ConsumableImageService.class);
    private static final long MAX_IMAGE_BYTES = 2L * 1024L * 1024L;

    private final Path storageLocation;

    public ConsumableImageService(
            @Value("${app.upload.dir:uploads/consumables}") String uploadDir) {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
            log.info("Consumable image storage initialized at: {}", this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
    }

    /**
     * Store an image file and return the stored filename.
     */
    public String storeImage(UUID consumableId, MultipartFile file) throws IOException {
        String extension = "." + UploadFileValidator.imageExtension(file, MAX_IMAGE_BYTES);
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        // Bug fix: Delete any existing images for this consumable to prevent conflicts
        // e.g., if ID.jpg exists and we upload ID.png, we must delete ID.jpg first.
        String[] supportedExtensions = { ".jpg", ".jpeg", ".png", ".gif", ".webp" };
        for (String ext : supportedExtensions) {
            String existingFilename = consumableId.toString() + ext;
            Path existingPath = safeResolve(existingFilename);
            if (Files.exists(existingPath)) {
                try {
                    Files.delete(existingPath);
                    log.info("Deleted old image variant: {}", existingFilename);
                } catch (IOException e) {
                    log.warn("Failed to delete stale image: {}", existingFilename, e);
                }
            }
        }

        // Use consumable ID as filename to ensure uniqueness and easy lookup
        String filename = consumableId.toString() + extension;
        Path targetLocation = safeResolve(filename);

        log.info("Processing upload for file: '{}', content type: '{}', saving as: '{}'",
                originalFilename, contentType, filename);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored image for consumable {}: {} ({} bytes)", consumableId, filename, file.getSize());

        return filename;
    }

    /**
     * Get the path to a stored image.
     */
    public Path getImagePath(String filename) {
        return safeResolve(filename);
    }

    /**
     * Check if an image exists.
     */
    public boolean imageExists(String filename) {
        Path path = getImagePath(filename);
        return Files.exists(path) && Files.isReadable(path);
    }

    /**
     * Delete an image if it exists.
     */
    public void deleteImage(String filename) throws IOException {
        Path path = getImagePath(filename);
        Files.deleteIfExists(path);
        log.info("Deleted image: {}", filename);
    }

    private Path safeResolve(String filename) {
        Path resolved = this.storageLocation.resolve(filename).normalize();
        if (!resolved.startsWith(this.storageLocation)) {
            throw new IllegalArgumentException("Invalid image filename");
        }
        return resolved;
    }
}
