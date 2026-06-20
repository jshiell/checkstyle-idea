package org.infernus.idea.checkstyle;

import org.infernus.idea.checkstyle.exception.CheckstyleDownloadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

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
        resolver = new ManifestBasedArtifactResolver(manifest, m2Root, mockDownloader);
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
        }).when(mockDownloader).download(anyString(), eq(jarPath));

        List<Path> result = resolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", VERSION);

        verify(mockDownloader).download(
                "https://repo1.maven.org/maven2/com/puppycrawl/tools/checkstyle/" + VERSION + "/checkstyle-" + VERSION + ".jar",
                jarPath);
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
        }).when(mockDownloader).download(anyString(), eq(jarPath));

        resolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", VERSION);

        verify(mockDownloader).download(anyString(), eq(jarPath));
    }

    @Test
    void verifiesHashOfDownloadedBytes() throws Exception {
        Path jarPath = expectedJarPath(VERSION);

        doAnswer(inv -> {
            Files.createDirectories(jarPath.getParent());
            Files.write(jarPath, new byte[]{9, 9, 9});
            return null;
        }).when(mockDownloader).download(anyString(), eq(jarPath));

        assertThrows(CheckstyleDownloadException.class,
                () -> resolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", VERSION));
        assertFalse(Files.exists(jarPath));
    }

    @Test
    void throwsForVersionNotInManifest() {
        assertThrows(CheckstyleDownloadException.class,
                () -> resolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", "9.0"));
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
