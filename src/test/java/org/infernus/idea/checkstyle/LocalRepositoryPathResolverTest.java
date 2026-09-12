package org.infernus.idea.checkstyle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalRepositoryPathResolverTest {

    @Test
    void returnsSuppliedPathWhenPresent(@TempDir final Path suppliedPath) {
        LocalRepositoryPathResolver resolver = new LocalRepositoryPathResolver(() -> Optional.of(suppliedPath));

        assertEquals(suppliedPath, resolver.resolve());
    }

    @Test
    void fallsBackToDefaultM2RootWhenSupplierReturnsEmpty() {
        LocalRepositoryPathResolver resolver = new LocalRepositoryPathResolver(Optional::empty);

        assertEquals(CheckstyleArtifactDownloader.defaultM2Root(), resolver.resolve());
    }

    @Test
    void fallsBackToDefaultM2RootWhenSupplierThrows() {
        LocalRepositoryPathResolver resolver = new LocalRepositoryPathResolver(() -> {
            throw new NoClassDefFoundError("Maven plugin classes unavailable");
        });

        assertEquals(CheckstyleArtifactDownloader.defaultM2Root(), resolver.resolve());
    }
}
