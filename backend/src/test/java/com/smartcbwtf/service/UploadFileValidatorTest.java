package com.smartcbwtf.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadFileValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void publicImageExtensionAllowsOnlyKnownRasterTypes() {
        assertEquals("jpg", UploadFileValidator.publicImageExtension(file("photo.jpg", "image/jpeg", jpgBytes())));
        assertEquals("png", UploadFileValidator.publicImageExtension(file("photo.png", "image/png", pngBytes())));
        assertEquals("gif", UploadFileValidator.publicImageExtension(file("photo.gif", "image/gif", gifBytes())));
        assertEquals("webp", UploadFileValidator.publicImageExtension(file("photo.webp", "image/webp", webpBytes())));

        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.publicImageExtension(file("script.svg", "image/svg+xml", new byte[] { 1 })));
    }

    @Test
    void publicImageExtensionRejectsSpoofedImageContent() {
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.publicImageExtension(
                        file("fake.png", "image/png", "%PDF-1.7".getBytes())));
    }

    @Test
    void publicImageExtensionRejectsOversizedFiles() {
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.publicImageExtension(
                        file("large.png", "image/png", (2 * 1024 * 1024) + 1)));
    }

    @Test
    void rentAgreementExtensionAllowsPdfAndSafeImages() {
        assertEquals("pdf",
                UploadFileValidator.rentAgreementExtension(file("agreement.pdf", "application/pdf", pdfBytes())));
        assertEquals("png", UploadFileValidator.rentAgreementExtension(file("agreement.png", "image/png", pngBytes())));

        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.rentAgreementExtension(
                        file("agreement.svg", "image/svg+xml", new byte[] { 1 })));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.rentAgreementExtension(
                        file("agreement.pdf", "application/pdf", pngBytes())));
    }

    @Test
    void publicPngOrJpegExtensionKeepsBrandingAssetsPdfCompatible() {
        assertEquals("jpg", UploadFileValidator.publicPngOrJpegExtension(file("logo.jpg", "image/jpeg", jpgBytes())));
        assertEquals("png", UploadFileValidator.publicPngOrJpegExtension(file("logo.png", "image/png", pngBytes())));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.publicPngOrJpegExtension(file("logo.webp", "image/webp", webpBytes())));
    }

    @Test
    void rentAgreementExtensionHonorsCustomSizeLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.rentAgreementExtension(
                        file("agreement.pdf", "application/pdf", 11 * 1024 * 1024),
                        10L * 1024L * 1024L));
    }

    @Test
    void htmlTemplateExtensionAllowsTextHtmlOnlyWithinLimit() {
        assertEquals("html", UploadFileValidator.htmlTemplateExtension(
                file("template.html", "text/html", "<h1>Hello</h1>".getBytes()), 1024));

        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.htmlTemplateExtension(
                        file("template.html", "application/pdf", pdfBytes()), 1024));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.htmlTemplateExtension(
                        file("template.html", "text/html", new byte[] { '<', 'h', 0, '>' }), 1024));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.htmlTemplateExtension(
                        file("template.html", "text/html", 1025), 1024));
    }

    @Test
    void profilePhotoPathResolvesOnlyFlatProfileFilenames() {
        Path path = UploadFileValidator.profilePhotoPath(tempDir.toString(), "/uploads/profiles/user.png");

        assertTrue(path.startsWith(tempDir.toAbsolutePath().normalize()));
        assertEquals("user.png", path.getFileName().toString());
    }

    @Test
    void profilePhotoPathRejectsTraversalAndWrongPrefixes() {
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.profilePhotoPath(tempDir.toString(), "/uploads/profiles/../secret.png"));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.profilePhotoPath(tempDir.toString(), "/uploads/branding/logo.png"));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.profilePhotoPath(tempDir.toString(), "/uploads/profiles/nested/photo.png"));
    }

    @Test
    void optionalProfilePhotoUrlAllowsOnlyFlatPublicProfileImages() {
        assertEquals("/uploads/profiles/user.jpg",
                UploadFileValidator.optionalProfilePhotoUrl(" /uploads/profiles/user.jpg "));
        assertEquals("/uploads/profiles/user.webp",
                UploadFileValidator.optionalProfilePhotoUrl("/uploads/profiles/user.webp"));
        assertEquals(null, UploadFileValidator.optionalProfilePhotoUrl(" "));

        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.optionalProfilePhotoUrl("https://example.com/avatar.png"));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.optionalProfilePhotoUrl("/uploads/profiles/../secret.png"));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.optionalProfilePhotoUrl("/uploads/profiles/user.svg"));
    }

    @Test
    void deleteProfilePhotoIfPresentDeletesOnlyValidatedProfileAsset() throws Exception {
        Path photo = tempDir.resolve("user.png");
        Files.write(photo, pngBytes());

        UploadFileValidator.deleteProfilePhotoIfPresent(tempDir.toString(), "/uploads/profiles/user.png");

        assertTrue(Files.notExists(photo));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.deleteProfilePhotoIfPresent(
                        tempDir.toString(),
                        "/uploads/profiles/../secret.png"));
    }

    @Test
    void uploadedAssetPathResolvesOnlyExpectedUploadPrefix() {
        Path path = UploadFileValidator.uploadedAssetPath(
                "/uploads/branding/facility/logo.png",
                "/uploads/branding/");

        assertTrue(path.startsWith(Path.of("uploads", "branding").toAbsolutePath().normalize()));
        assertEquals("logo.png", path.getFileName().toString());
    }

    @Test
    void uploadedAssetPathRejectsWrongPrefixesAndTraversal() {
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.uploadedAssetPath(
                        "/uploads/profiles/user.png",
                        "/uploads/branding/"));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.uploadedAssetPath(
                        "/uploads/branding/../secret.png",
                        "/uploads/branding/"));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.uploadedAssetPath(
                        "/uploads/branding/facility/../../secret.png",
                        "/uploads/branding/"));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.uploadedAssetPath(
                        "/uploads/branding/facility\\secret.png",
                        "/uploads/branding/"));
    }

    @Test
    void rentAgreementUrlAllowsOnlyFlatRentAgreementAssets() {
        assertEquals("/uploads/rent-agreements/agreement.pdf",
                UploadFileValidator.rentAgreementUrl(" /uploads/rent-agreements/agreement.pdf "));

        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.rentAgreementUrl("/uploads/branding/agreement.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.rentAgreementUrl("/uploads/rent-agreements/../secret.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.rentAgreementUrl("/uploads/rent-agreements/nested/agreement.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.rentAgreementUrl("/uploads/rent-agreements/agreement.exe"));
    }

    @Test
    void rentAgreementUrlForFacilityRequiresFacilityOwnedFilename() {
        UUID facilityId = UUID.randomUUID();
        assertEquals("/uploads/rent-agreements/" + facilityId + "_abc12345.pdf",
                UploadFileValidator.rentAgreementUrlForFacility(
                        "/uploads/rent-agreements/" + facilityId + "_abc12345.pdf",
                        facilityId));

        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.rentAgreementUrlForFacility(
                        "/uploads/rent-agreements/" + UUID.randomUUID() + "_abc12345.pdf",
                        facilityId));
        assertThrows(IllegalArgumentException.class,
                () -> UploadFileValidator.rentAgreementUrlForFacility(
                        "/uploads/rent-agreements/agreement.pdf",
                        facilityId));
    }

    private MockMultipartFile file(String name, String contentType, int size) {
        return new MockMultipartFile("file", name, contentType, new byte[size]);
    }

    private MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    private byte[] jpgBytes() {
        return new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 };
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
    }

    private byte[] gifBytes() {
        return "GIF89a".getBytes();
    }

    private byte[] webpBytes() {
        return new byte[] {
                'R', 'I', 'F', 'F',
                0x00, 0x00, 0x00, 0x00,
                'W', 'E', 'B', 'P'
        };
    }

    private byte[] pdfBytes() {
        return "%PDF-1.7".getBytes();
    }
}
