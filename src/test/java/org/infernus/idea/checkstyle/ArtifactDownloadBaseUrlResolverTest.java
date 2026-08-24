package org.infernus.idea.checkstyle;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ArtifactDownloadBaseUrlResolverTest {

    private static final String DEFAULT_BASE_URL = "https://repo1.maven.org/maven2/";

    @Test
    void returnsOverrideWhenValidHttpUrlSet() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                () -> Optional.of(new ArtifactRepositoryLocation("https://override.example.com/repo/", Optional.empty())),
                () -> {
                    fail("Maven mirror should not be consulted when a valid override is set");
                    return Optional.empty();
                });

        assertEquals("https://override.example.com/repo/", resolver.resolve().baseUrl());
    }

    @Test
    void returnsOverrideCredentialsWhenOverrideValid() {
        ArtifactRepositoryCredentials credentials = new ArtifactRepositoryCredentials("user", "pass");
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                () -> Optional.of(new ArtifactRepositoryLocation("https://override.example.com/repo/", Optional.of(credentials))),
                Optional::empty);

        assertEquals(Optional.of(credentials), resolver.resolve().credentials());
    }

    @Test
    void fallsBackToMavenMirrorWhenOverrideUnset() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                Optional::empty,
                () -> Optional.of(new ArtifactRepositoryLocation("https://mirror.example.com/repo/", Optional.empty())));

        assertEquals("https://mirror.example.com/repo/", resolver.resolve().baseUrl());
    }

    @Test
    void fallsBackToMavenMirrorWhenOverrideBaseUrlBlank() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                () -> Optional.of(new ArtifactRepositoryLocation("   ", Optional.empty())),
                () -> Optional.of(new ArtifactRepositoryLocation("https://mirror.example.com/repo/", Optional.empty())));

        assertEquals("https://mirror.example.com/repo/", resolver.resolve().baseUrl());
    }

    @Test
    void fallsBackToDefaultWhenNeitherOverrideNorMirrorPresent() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                Optional::empty,
                Optional::empty);

        ArtifactRepositoryLocation result = resolver.resolve();
        assertEquals(DEFAULT_BASE_URL, result.baseUrl());
        assertEquals(Optional.empty(), result.credentials());
    }

    @Test
    void fallsBackToMavenMirrorWhenOverrideIsMalformed() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                () -> Optional.of(new ArtifactRepositoryLocation("not a url", Optional.empty())),
                () -> Optional.of(new ArtifactRepositoryLocation("https://mirror.example.com/repo/", Optional.empty())));

        assertEquals("https://mirror.example.com/repo/", resolver.resolve().baseUrl());
    }

    @Test
    void fallsBackToDefaultWhenOverrideIsNotHttpScheme() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                () -> Optional.of(new ArtifactRepositoryLocation("ftp://mirror.example.com/repo/", Optional.empty())),
                Optional::empty);

        assertEquals(DEFAULT_BASE_URL, resolver.resolve().baseUrl());
    }

    @Test
    void fallsBackToDefaultWhenMavenMirrorResolutionThrows() {
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                Optional::empty,
                () -> {
                    throw new NoClassDefFoundError("Maven plugin classes unavailable");
                });

        assertEquals(DEFAULT_BASE_URL, resolver.resolve().baseUrl());
    }

    @Test
    void invalidOverrideDoesNotLeakCredentialsIntoMirrorBranchResult() {
        ArtifactRepositoryCredentials overrideCredentials = new ArtifactRepositoryCredentials("override-user", "override-pass");
        ArtifactDownloadBaseUrlResolver resolver = new ArtifactDownloadBaseUrlResolver(
                () -> Optional.of(new ArtifactRepositoryLocation("not a url", Optional.of(overrideCredentials))),
                () -> Optional.of(new ArtifactRepositoryLocation("https://mirror.example.com/repo/", Optional.empty())));

        ArtifactRepositoryLocation result = resolver.resolve();

        assertEquals("https://mirror.example.com/repo/", result.baseUrl());
        assertTrue(result.credentials().isEmpty());
    }
}
