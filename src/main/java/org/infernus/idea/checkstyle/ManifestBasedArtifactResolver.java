package org.infernus.idea.checkstyle;

import org.infernus.idea.checkstyle.exception.CheckstyleDownloadException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;


public class ManifestBasedArtifactResolver implements CheckstyleArtifactDownloader.ArtifactResolver {

    public interface JarDownloader {
        void download(String url, Path target) throws IOException;
    }

    private final DownloadManifest manifest;
    private final Path m2Root;
    private final JarDownloader jarDownloader;

    public ManifestBasedArtifactResolver(@NotNull final DownloadManifest manifest,
                                         @NotNull final Path m2Root,
                                         @NotNull final JarDownloader jarDownloader) {
        this.manifest = manifest;
        this.m2Root = m2Root;
        this.jarDownloader = jarDownloader;
    }

    @Override
    @NotNull
    public List<Path> resolveTransitively(@NotNull final String groupId,
                                          @NotNull final String artifactId,
                                          @NotNull final String version) throws Exception {
        List<ManifestEntry> entries = manifest.entriesFor(version);
        if (entries.isEmpty()) {
            throw new CheckstyleDownloadException("No manifest entry found for Checkstyle " + version);
        }

        List<Path> paths = new ArrayList<>();
        for (ManifestEntry entry : entries) {
            paths.add(ensureJar(entry));
        }
        return paths;
    }

    private Path ensureJar(@NotNull final ManifestEntry entry) throws IOException {
        Path target = entry.m2Path(m2Root);
        if (Files.exists(target) && sha256Matches(target, entry.sha256hex())) {
            return target;
        }
        Files.createDirectories(target.getParent());
        jarDownloader.download(entry.mavenCentralUrl(), target);
        String actualHex = sha256Hex(target);
        if (!entry.sha256hex().equalsIgnoreCase(actualHex)) {
            Files.deleteIfExists(target);
            throw new CheckstyleDownloadException(
                    "SHA-256 mismatch after downloading " + entry.mavenCentralUrl()
                            + "; expected " + entry.sha256hex() + " but was " + actualHex);
        }
        return target;
    }

    private static boolean sha256Matches(@NotNull final Path file, @NotNull final String expectedHex) {
        return expectedHex.equalsIgnoreCase(sha256Hex(file));
    }

    private static String sha256Hex(@NotNull final Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }
}
