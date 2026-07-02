package com.smartcbwtf.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

public final class UploadFileValidator {
    public static final long PUBLIC_IMAGE_MAX_BYTES = 2L * 1024L * 1024L;
    public static final long RENT_AGREEMENT_MAX_BYTES = 20L * 1024L * 1024L;
    public static final String RENT_AGREEMENT_PUBLIC_PREFIX = "/uploads/rent-agreements/";

    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp");

    private UploadFileValidator() {
    }

    public static String publicImageExtension(MultipartFile file) {
        return imageExtension(file, PUBLIC_IMAGE_MAX_BYTES);
    }

    public static String publicPngOrJpegExtension(MultipartFile file) {
        ensurePresent(file);
        ensureMaxSize(file, PUBLIC_IMAGE_MAX_BYTES, "Image file");
        String contentType = file.getContentType();
        String extension = switch (contentType == null ? "" : contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> throw new IllegalArgumentException("Only PNG and JPEG images are allowed");
        };
        ensureImageSignature(file, extension);
        return extension;
    }

    public static String imageExtension(MultipartFile file, long maxBytes) {
        ensurePresent(file);
        ensureMaxSize(file, maxBytes, "Image file");
        String extension = IMAGE_EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("Only JPEG, PNG, GIF, and WebP images are allowed");
        }
        ensureImageSignature(file, extension);
        return extension;
    }

    public static String rentAgreementExtension(MultipartFile file) {
        return rentAgreementExtension(file, RENT_AGREEMENT_MAX_BYTES);
    }

    public static String rentAgreementExtension(MultipartFile file, long maxBytes) {
        ensurePresent(file);
        ensureMaxSize(file, maxBytes, "File");
        if ("application/pdf".equals(file.getContentType())) {
            ensurePdfSignature(file);
            return "pdf";
        }
        return imageExtension(file, maxBytes);
    }

    public static String htmlTemplateExtension(MultipartFile file, long maxBytes) {
        ensurePresent(file);
        ensureMaxSize(file, maxBytes, "HTML template");
        String contentType = file.getContentType();
        if (!"text/html".equals(contentType) && !"text/plain".equals(contentType)) {
            throw new IllegalArgumentException("HTML templates must be uploaded as text/html or text/plain");
        }
        ensureTextLike(file);
        return "html";
    }

    public static Path profilePhotoPath(String uploadDir, String profilePhotoUrl) {
        if (profilePhotoUrl == null || !profilePhotoUrl.startsWith("/uploads/profiles/")) {
            throw new IllegalArgumentException("Invalid profile photo URL");
        }
        String filename = profilePhotoUrl.substring("/uploads/profiles/".length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid profile photo filename");
        }
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path resolved = root.resolve(filename).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid profile photo path");
        }
        return resolved;
    }

    public static String optionalProfilePhotoUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        String normalized = publicUrl.trim();
        if (!normalized.startsWith("/uploads/profiles/")) {
            throw new IllegalArgumentException("Invalid profile photo URL");
        }
        String filename = normalized.substring("/uploads/profiles/".length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new IllegalArgumentException("Invalid profile photo URL");
        }
        int dot = filename.lastIndexOf('.');
        String extension = dot >= 0 ? filename.substring(dot + 1).toLowerCase(java.util.Locale.ROOT) : "";
        if (!IMAGE_EXTENSIONS.containsValue(extension)) {
            throw new IllegalArgumentException("Invalid profile photo file type");
        }
        return "/uploads/profiles/" + filename;
    }

    public static void deleteProfilePhotoIfPresent(String uploadDir, String profilePhotoUrl) throws IOException {
        if (profilePhotoUrl == null || profilePhotoUrl.isBlank()) {
            return;
        }
        Files.deleteIfExists(profilePhotoPath(uploadDir, profilePhotoUrl));
    }

    public static Path uploadedAssetPath(String publicUrl, String expectedPublicPrefix) {
        if (publicUrl == null || expectedPublicPrefix == null || expectedPublicPrefix.isBlank()
                || !expectedPublicPrefix.startsWith("/") || !expectedPublicPrefix.endsWith("/")) {
            throw new IllegalArgumentException("Invalid upload path configuration");
        }
        if (!publicUrl.startsWith(expectedPublicPrefix)) {
            throw new IllegalArgumentException("Unexpected uploaded asset URL");
        }
        String relative = publicUrl.substring(expectedPublicPrefix.length());
        if (relative.isBlank() || relative.contains("\\") || relative.contains("..")) {
            throw new IllegalArgumentException("Invalid uploaded asset path");
        }
        Path root = Paths.get(expectedPublicPrefix.substring(1)).toAbsolutePath().normalize();
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid uploaded asset path");
        }
        return resolved;
    }

    public static void deleteUploadedAssetIfPresent(String publicUrl, String expectedPublicPrefix) throws IOException {
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }
        Files.deleteIfExists(uploadedAssetPath(publicUrl, expectedPublicPrefix));
    }

    public static String rentAgreementUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            throw new IllegalArgumentException("Rent agreement URL is required");
        }
        String normalized = publicUrl.trim();
        if (!normalized.startsWith(RENT_AGREEMENT_PUBLIC_PREFIX)) {
            throw new IllegalArgumentException("Invalid rent agreement URL");
        }
        String filename = normalized.substring(RENT_AGREEMENT_PUBLIC_PREFIX.length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new IllegalArgumentException("Invalid rent agreement URL");
        }
        int dot = filename.lastIndexOf('.');
        String extension = dot >= 0 ? filename.substring(dot + 1).toLowerCase(java.util.Locale.ROOT) : "";
        if (!extension.equals("pdf") && !IMAGE_EXTENSIONS.containsValue(extension)) {
            throw new IllegalArgumentException("Invalid rent agreement file type");
        }
        uploadedAssetPath(normalized, RENT_AGREEMENT_PUBLIC_PREFIX);
        return RENT_AGREEMENT_PUBLIC_PREFIX + filename;
    }

    public static String rentAgreementUrlForFacility(String publicUrl, UUID facilityId) {
        if (facilityId == null) {
            throw new IllegalArgumentException("Invalid rent agreement facility context");
        }
        String normalized = rentAgreementUrl(publicUrl);
        String filename = normalized.substring(RENT_AGREEMENT_PUBLIC_PREFIX.length());
        if (!filename.startsWith(facilityId + "_")) {
            throw new IllegalArgumentException("Rent agreement document does not belong to this facility");
        }
        return normalized;
    }

    private static void ensurePresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }
    }

    private static void ensureMaxSize(MultipartFile file, long maxBytes, String label) {
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(label + " must be under " + maxBytes / (1024 * 1024) + "MB");
        }
    }

    private static void ensureImageSignature(MultipartFile file, String extension) {
        byte[] header = readHeader(file, 16);
        boolean valid = switch (extension) {
            case "jpg" -> startsWith(header, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif" -> startsWith(header, 'G', 'I', 'F', '8', '7', 'a')
                    || startsWith(header, 'G', 'I', 'F', '8', '9', 'a');
            case "webp" -> header.length >= 12
                    && startsWith(header, 'R', 'I', 'F', 'F')
                    && header[8] == 'W'
                    && header[9] == 'E'
                    && header[10] == 'B'
                    && header[11] == 'P';
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("File content does not match declared image type");
        }
    }

    private static void ensurePdfSignature(MultipartFile file) {
        if (!startsWith(readHeader(file, 5), '%', 'P', 'D', 'F', '-')) {
            throw new IllegalArgumentException("File content does not match declared PDF type");
        }
    }

    private static void ensureTextLike(MultipartFile file) {
        byte[] header = readHeader(file, 512);
        for (byte value : header) {
            if (value == 0) {
                throw new IllegalArgumentException("HTML template content must be text");
            }
        }
    }

    private static byte[] readHeader(MultipartFile file, int bytes) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read uploaded file", e);
        }
    }

    private static boolean startsWith(byte[] actual, int... expected) {
        if (actual.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((actual[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
