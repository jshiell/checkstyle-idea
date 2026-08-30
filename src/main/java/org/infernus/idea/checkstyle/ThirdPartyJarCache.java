package org.infernus.idea.checkstyle;

import org.infernus.idea.checkstyle.exception.ThirdPartyJarDownloadException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ThirdPartyJarCache {

    public interface JarFetcher {
        void fetch(String url, Path target) throws IOException;
    }

    private final Path cacheRoot;
    private final JarFetcher fetcher;

    public ThirdPartyJarCache(@NotNull final Path cacheRoot, @NotNull final JarFetcher fetcher) {
        this.cacheRoot = cacheRoot;
        this.fetcher = fetcher;
    }

    @NotNull
    public static Path defaultCacheRoot() {
        return Path.of(System.getProperty("user.home"), ".checkstyle-idea", "third-party-jars");
    }

    @NotNull
    public static ThirdPartyJarCache create() {
        return new ThirdPartyJarCache(defaultCacheRoot(),
                (url, target) -> new HttpJarDownloader().download(url, target, Optional.empty()));
    }

    @NotNull
    public Path resolve(@NotNull final String url) {
        final Path cachePath = cachePathFor(url);
        if (Files.exists(cachePath)) {
            return cachePath;
        }
        return forceRefresh(url);
    }

    @NotNull
    public Path forceRefresh(@NotNull final String url) {
        final Path target = cachePathFor(url);
        try {
            Files.createDirectories(cacheRoot);
            final Path tmp = Files.createTempFile(cacheRoot, ".download-", ".part");
            try {
                fetcher.fetch(url, tmp);
                try (ZipFile ignored = new ZipFile(tmp.toFile())) {
                    // validate it's a well-formed zip
                } catch (ZipException e) {
                    throw new IOException("Downloaded file is not a valid JAR", e);
                }
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            throw new ThirdPartyJarDownloadException("Failed to download third-party check JAR from " + url, e);
        }
        return target;
    }

    @NotNull
    private Path cachePathFor(@NotNull final String url) {
        return cacheRoot.resolve(sha256Hex(url) + ".jar");
    }

    @NotNull
    private static String sha256Hex(@NotNull final String value) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
