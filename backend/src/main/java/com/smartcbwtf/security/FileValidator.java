package com.smartcbwtf.security;

import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class FileValidator {

    private static final Tika tika = new Tika();

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private static final Set<String> ALLOWED_STRICT_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );

    /**
     * Validates that the file is a valid image (JPEG, PNG, GIF, WEBP).
     * Returns the detected MIME type.
     * Throws IllegalArgumentException if invalid.
     */
    public static String validateImage(MultipartFile file) throws IOException {
        return validate(file, ALLOWED_IMAGE_TYPES);
    }

    /**
     * Validates that the file is a valid image (JPEG, PNG only).
     * Returns the detected MIME type.
     * Throws IllegalArgumentException if invalid.
     */
    public static String validateStrictImage(MultipartFile file) throws IOException {
        return validate(file, ALLOWED_STRICT_IMAGE_TYPES);
    }

    private static String validate(MultipartFile file, Set<String> allowedTypes) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        try (InputStream is = file.getInputStream()) {
            String detectedType = tika.detect(is);
            if (!allowedTypes.contains(detectedType)) {
                throw new IllegalArgumentException("Invalid file type: " + detectedType + ". Allowed: " + allowedTypes);
            }
            return detectedType;
        }
    }
}
