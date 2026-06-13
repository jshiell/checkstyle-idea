package org.infernus.idea.checkstyle;

import org.infernus.idea.checkstyle.exception.CheckstyleDownloadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class CheckstyleArtifactDownloaderTest {

    @TempDir
    Path m2Root;

    private CheckstyleArtifactDownloader.ArtifactResolver mockResolver;
    private CheckstyleArtifactDownloader downloader;

    @BeforeEach
    void setUp() {
        mockResolver = mock(CheckstyleArtifactDownloader.ArtifactResolver.class);
        downloader = new CheckstyleArtifactDownloader(m2Root, mockResolver);
    }

    @Test
    void isAvailableLocallyReturnsFalseWhenJarAbsent() {
        assertFalse(downloader.isAvailableLocally("10.26.1"));
    }

    @Test
    void isAvailableLocallyReturnsTrueWhenJarPresent() throws Exception {
        Path jarPath = m2Root
                .resolve("com/puppycrawl/tools/checkstyle/10.26.1/checkstyle-10.26.1.jar");
        Files.createDirectories(jarPath.getParent());
        Files.createFile(jarPath);

        assertTrue(downloader.isAvailableLocally("10.26.1"));
    }

    @Test
    void downloadReturnsPathsFromResolver() throws Exception {
        List<Path> expected = List.of(Path.of("/fake/checkstyle.jar"));
        when(mockResolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", "10.26.1"))
                .thenReturn(expected);

        List<Path> result = downloader.download("10.26.1");

        assertEquals(expected, result);
    }

    @Test
    void downloadWrapsResolverExceptionAsDownloadException() throws Exception {
        when(mockResolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", "10.4"))
                .thenThrow(new RuntimeException("network error"));

        assertThrows(CheckstyleDownloadException.class, () -> downloader.download("10.4"));
    }

    @Test
    void getLocalPathsReturnsMavenLocalRepoPaths() throws Exception {
        List<Path> resolved = List.of(
                m2Root.resolve("com/puppycrawl/tools/checkstyle/10.26.1/checkstyle-10.26.1.jar"),
                m2Root.resolve("org/antlr/antlr4-runtime/4.13.2/antlr4-runtime-4.13.2.jar"));
        when(mockResolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", "10.26.1"))
                .thenReturn(resolved);

        List<Path> result = downloader.download("10.26.1");

        assertEquals(resolved, result);
    }
}
