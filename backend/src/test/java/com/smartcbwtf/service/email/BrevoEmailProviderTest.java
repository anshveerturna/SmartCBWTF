package com.smartcbwtf.service.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrevoEmailProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveAllowedAttachmentPathAcceptsGeneratedFileFormats() throws Exception {
        BrevoEmailProvider provider = providerWithAttachmentRoot(tempDir);
        Path agreementDir = tempDir.resolve("agreements");
        Files.createDirectories(agreementDir);
        Path file = agreementDir.resolve("agreement.pdf");
        Files.writeString(file, "pdf");

        assertEquals(file.normalize(), provider.resolveAllowedAttachmentPath("/files/agreements/agreement.pdf"));
        assertEquals(file.normalize(), provider.resolveAllowedAttachmentPath("files/agreements/agreement.pdf"));
        assertEquals(file.normalize(), provider.resolveAllowedAttachmentPath(file.toString()));
    }

    @Test
    void resolveAllowedAttachmentPathRejectsTraversalAndExternalFiles() {
        BrevoEmailProvider provider = providerWithAttachmentRoot(tempDir);

        assertThrows(IllegalArgumentException.class,
                () -> provider.resolveAllowedAttachmentPath("/files/../pom.xml"));
        assertThrows(IllegalArgumentException.class,
                () -> provider.resolveAllowedAttachmentPath(tempDir.getParent().resolve("outside.pdf").toString()));
        assertThrows(IllegalArgumentException.class,
                () -> provider.resolveAllowedAttachmentPath("files\\agreements\\agreement.pdf"));
    }

    private BrevoEmailProvider providerWithAttachmentRoot(Path root) {
        BrevoEmailProvider provider = new BrevoEmailProvider();
        ReflectionTestUtils.setField(provider, "attachmentRoot", root.toString());
        return provider;
    }
}
