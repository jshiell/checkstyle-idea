package org.infernus.idea.checkstyle;

import org.infernus.idea.checkstyle.exception.CheckstyleDownloadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class ManifestBasedArtifactResolverTest {

    private static final String VERSION = "10.26.1";
    private static final String SHA256 = sha256Of(new byte[]{1, 2, 3});

    @TempDir
    Path m2Root;

    private ManifestBasedArtifactResolver.JarDownloader mockDownloader;
    private DownloadManifest manifest;
    private ManifestBasedArtifactResolver resolver;

    @BeforeEach
    void setUp() {
        mockDownloader = mock(ManifestBasedArtifactResolver.JarDownloader.class);
        manifest = DownloadManifest.fromString(VERSION + " = com.puppycrawl.tools:checkstyle:" + VERSION + "::" + SHA256 + "\n");
        resolver = new ManifestBasedArtifactResolver(manifest, m2Root, mockDownloader,
                () -> new ArtifactRepositoryLocation("https://repo1.maven.org/maven2/", Optional.empty()));
    }

    @Test
    void skipsDownloadWhenJarExistsWithCorrectHash() throws Exception {
        Path jarPath = expectedJarPath(VERSION);
        Files.createDirectories(jarPath.getParent());
        Files.write(jarPath, new byte[]{1, 2, 3});

        List<Path> result = resolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", VERSION);

        verifyNoInteractions(mockDownloader);
        assertEquals(List.of(jarPath), result);
    }

    @Test
    void downloadsJarWhenAbsent() throws Exception {
        Path jarPath = expectedJarPath(VERSION);

        doAnswer(inv -> {
            Files.createDirectories(jarPath.getParent());
            Files.write(jarPath, new byte[]{1, 2, 3});
            return null;
        }).when(mockDownloader).download(anyString(), eq(jarPath), any());

        List<Path> result = resolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", VERSION);

        verify(mockDownloader).download(
                "https://repo1.maven.org/maven2/com/puppycrawl/tools/checkstyle/" + VERSION + "/checkstyle-" + VERSION + ".jar",
                jarPath, Optional.empty());
        assertEquals(List.of(jarPath), result);
    }

    @Test
    void redownloadsJarWhenHashMismatch() throws Exception {
        Path jarPath = expectedJarPath(VERSION);
        Files.createDirectories(jarPath.getParent());
        Files.write(jarPath, new byte[]{9, 9, 9}); // wrong content

        doAnswer(inv -> {
            Files.write(jarPath, new byte[]{1, 2, 3});
            return null;
        }).when(mockDownloader).download(anyString(), eq(jarPath), any());

        resolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", VERSION);

        verify(mockDownloader).download(anyString(), eq(jarPath), any());
    }

    @Test
    void verifiesHashOfDownloadedBytes() throws Exception {
        Path jarPath = expectedJarPath(VERSION);

        doAnswer(inv -> {
            Files.createDirectories(jarPath.getParent());
            Files.write(jarPath, new byte[]{9, 9, 9});
            return null;
        }).when(mockDownloader).download(anyString(), eq(jarPath), any());

        assertThrows(CheckstyleDownloadException.class,
                () -> resolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", VERSION));
        assertFalse(Files.exists(jarPath));
    }

    @Test
    void downloadsJarUsingSuppliedBaseUrl() throws Exception {
        Path jarPath = expectedJarPath(VERSION);
        resolver = new ManifestBasedArtifactResolver(manifest, m2Root, mockDownloader,
                () -> new ArtifactRepositoryLocation("https://mirror.example.com/repo/", Optional.empty()));

        doAnswer(inv -> {
            Files.createDirectories(jarPath.getParent());
            Files.write(jarPath, new byte[]{1, 2, 3});
            return null;
        }).when(mockDownloader).download(anyString(), eq(jarPath), any());

        resolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", VERSION);

        verify(mockDownloader).download(
                "https://mirror.example.com/repo/com/puppycrawl/tools/checkstyle/" + VERSION + "/checkstyle-" + VERSION + ".jar",
                jarPath, Optional.empty());
    }

    @Test
    void throwsForVersionNotInManifest() {
        assertThrows(CheckstyleDownloadException.class,
                () -> resolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", "9.0"));
    }

    @Test
    void invokesLocationSupplierExactlyOnceForMultiEntryManifest() throws Exception {
        String otherSha = sha256Of(new byte[]{4, 5, 6});
        DownloadManifest multiEntryManifest = DownloadManifest.fromString(
                VERSION + " = com.puppycrawl.tools:checkstyle:" + VERSION + "::" + SHA256
                        + ", org.antlr:antlr4-runtime:" + VERSION + "::" + otherSha + "\n");

        @SuppressWarnings("unchecked")
        Supplier<ArtifactRepositoryLocation> locationSupplier = mock(Supplier.class);
        when(locationSupplier.get())
                .thenReturn(new ArtifactRepositoryLocation("https://repo1.maven.org/maven2/", Optional.empty()));

        doAnswer(inv -> {
            Path target = inv.getArgument(1);
            Files.createDirectories(target.getParent());
            Files.write(target, new byte[]{1, 2, 3});
            return null;
        }).when(mockDownloader).download(contains("checkstyle-" + VERSION + ".jar"), any(), any());
        doAnswer(inv -> {
            Path target = inv.getArgument(1);
            Files.createDirectories(target.getParent());
            Files.write(target, new byte[]{4, 5, 6});
            return null;
        }).when(mockDownloader).download(contains("antlr4-runtime-" + VERSION + ".jar"), any(), any());

        resolver = new ManifestBasedArtifactResolver(multiEntryManifest, m2Root, mockDownloader, locationSupplier);
        resolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", VERSION);

        verify(locationSupplier, times(1)).get();
    }

    private Path expectedJarPath(final String version) {
        return m2Root.resolve("com/puppycrawl/tools/checkstyle/" + version + "/checkstyle-" + version + ".jar");
    }

    private static String sha256Of(final byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
