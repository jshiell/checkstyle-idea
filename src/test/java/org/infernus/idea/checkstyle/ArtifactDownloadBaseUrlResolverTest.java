package org.infernus.idea.checkstyle;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ArtifactDownloadBaseUrlResolverTest {

    private static final String DEFAULT_BASE_URL = "https://repo1.maven.org/maven2/";

    @Test
    void returnsOverrideWhenValidHttpUrlSet() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                () -> "https://override.example.com/repo/",
                () -> {
                    fail("Maven mirror should not be consulted when a valid override is set");
                    return Optional.empty();
                });

        assertEquals("https://override.example.com/repo/", resolver.resolve());
    }

    @Test
    void fallsBackToMavenMirrorWhenOverrideUnset() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                () -> null,
                () -> Optional.of("https://mirror.example.com/repo/"));

        assertEquals("https://mirror.example.com/repo/", resolver.resolve());
    }

    @Test
    void fallsBackToMavenMirrorWhenOverrideBlank() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                () -> "   ",
                () -> Optional.of("https://mirror.example.com/repo/"));

        assertEquals("https://mirror.example.com/repo/", resolver.resolve());
    }

    @Test
    void fallsBackToDefaultWhenNeitherOverrideNorMirrorPresent() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                () -> null,
                Optional::empty);

        assertEquals(DEFAULT_BASE_URL, resolver.resolve());
    }

    @Test
    void fallsBackToMavenMirrorWhenOverrideIsMalformed() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                () -> "not a url",
                () -> Optional.of("https://mirror.example.com/repo/"));

        assertEquals("https://mirror.example.com/repo/", resolver.resolve());
    }

    @Test
    void fallsBackToDefaultWhenOverrideIsNotHttpScheme() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                () -> "ftp://mirror.example.com/repo/",
                Optional::empty);

        assertEquals(DEFAULT_BASE_URL, resolver.resolve());
    }

    @Test
    void fallsBackToDefaultWhenMavenMirrorResolutionThrows() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                () -> null,
                () -> {
                    throw new NoClassDefFoundError("Maven plugin classes unavailable");
                });

        assertEquals(DEFAULT_BASE_URL, resolver.resolve());
    }
}
