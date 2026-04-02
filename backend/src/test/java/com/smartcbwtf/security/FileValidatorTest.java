package com.smartcbwtf.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class FileValidatorTest {

    @Test
    void validateImage_shouldPassForValidJpeg() throws Exception {
        // JPEG magic bytes: FF D8 FF
        byte[] content = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        assertDoesNotThrow(() -> FileValidator.validateImage(file));
        assertEquals("image/jpeg", FileValidator.validateImage(file));
    }

    @Test
    void validateImage_shouldFailForTextFile() throws Exception {
        byte[] content = "This is a text file".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", content);

        assertThrows(IllegalArgumentException.class, () -> FileValidator.validateImage(file));
    }

    @Test
    void validateStrictImage_shouldFailForGif() throws Exception {
        // GIF magic bytes: GIF89a
        byte[] content = "GIF89a".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.gif", "image/gif", content);

        assertThrows(IllegalArgumentException.class, () -> FileValidator.validateStrictImage(file));
    }
}
