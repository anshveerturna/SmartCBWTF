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

class ConsumableImageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeImageUsesTrustedContentTypeForExtension() throws Exception {
        ConsumableImageService service = new ConsumableImageService(tempDir.toString());
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "image/png",
                pngBytes());

        String filename = service.storeImage(id, file);

        assertEquals(id + ".png", filename);
        assertTrue(Files.exists(tempDir.resolve(filename)));
    }

    @Test
    void storeImageRejectsUnsupportedContentType() {
        ConsumableImageService service = new ConsumableImageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.svg",
                "image/svg+xml",
                new byte[] { 1, 2, 3 });

        assertThrows(IllegalArgumentException.class, () -> service.storeImage(UUID.randomUUID(), file));
    }

    @Test
    void storeImageRejectsOversizedFiles() {
        ConsumableImageService service = new ConsumableImageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                new byte[(2 * 1024 * 1024) + 1]);

        assertThrows(IllegalArgumentException.class, () -> service.storeImage(UUID.randomUUID(), file));
    }

    @Test
    void getImagePathRejectsTraversalOutsideStorageRoot() {
        ConsumableImageService service = new ConsumableImageService(tempDir.toString());

        assertThrows(IllegalArgumentException.class, () -> service.getImagePath("../secret.png"));
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
    }
}
