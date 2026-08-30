package org.infernus.idea.checkstyle;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ThirdPartyJarCacheTest {

    private static final String URL = "https://example.invalid/custom-check.jar";

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void resolveOnACacheHitReturnsTheExistingFileWithoutInvokingTheFetcher(@TempDir final Path cacheRoot) throws IOException {
        final byte[] content = {1, 2, 3};
        final Path cachedFile = cacheRoot.resolve(sha256Hex(URL) + ".jar");
        Files.write(cachedFile, content);

        final ThirdPartyJarCache cache = new ThirdPartyJarCache(cacheRoot, (url, target) -> {
            throw new AssertionError("fetcher should not be invoked on a cache hit");
        });

        final Path resolved = cache.resolve(URL);

        assertThat(resolved, is(cachedFile));
    }

    @Test
    void resolveOnACacheMissDownloadsAndCachesAndTheSecondCallIsAPureCacheHit(@TempDir final Path cacheRoot) throws Exception {
        final byte[] jarBytes = validZipBytes();
        server.createContext("/jar", exchange -> {
            exchange.sendResponseHeaders(200, jarBytes.length);
            exchange.getResponseBody().write(jarBytes);
            exchange.getResponseBody().close();
        });
        final String url = baseUrl + "/jar";

        final ThirdPartyJarCache firstCache = new ThirdPartyJarCache(cacheRoot,
                (fetchUrl, target) -> new HttpJarDownloader().download(fetchUrl, target, Optional.empty()));

        final Path resolved = firstCache.resolve(url);

        assertThat(Files.readAllBytes(resolved), is(jarBytes));

        final ThirdPartyJarCache secondCache = new ThirdPartyJarCache(cacheRoot, (fetchUrl, target) -> {
            throw new AssertionError("fetcher should not be invoked on a cache hit");
        });

        final Path secondResolved = secondCache.resolve(url);

        assertThat(secondResolved, is(resolved));
        assertThat(Files.readAllBytes(secondResolved), is(jarBytes));
    }

    @Test
    void resolveOnAnEmptyCacheWithInvalidContentThrowsAndLeavesNoFileBehind(@TempDir final Path cacheRoot) {
        final ThirdPartyJarCache cache = new ThirdPartyJarCache(cacheRoot,
                (url, target) -> Files.write(target, "<html>not a jar</html>".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.infernus.idea.checkstyle.exception.ThirdPartyJarDownloadException.class,
                () -> cache.resolve(URL));

        assertThat(java.util.Arrays.asList(cacheRoot.toFile().list()), org.hamcrest.Matchers.empty());
    }

    @Test
    void forceRefreshWithInvalidContentLeavesAPreExistingValidCacheUntouched(@TempDir final Path cacheRoot) throws IOException {
        final byte[] originalContent = validZipBytes();
        final Path cachedFile = cacheRoot.resolve(sha256Hex(URL) + ".jar");
        Files.write(cachedFile, originalContent);

        final ThirdPartyJarCache cache = new ThirdPartyJarCache(cacheRoot,
                (url, target) -> Files.write(target, "<html>not a jar</html>".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.infernus.idea.checkstyle.exception.ThirdPartyJarDownloadException.class,
                () -> cache.forceRefresh(URL));

        assertThat(Files.readAllBytes(cachedFile), is(originalContent));
        assertThat(cache.resolve(URL), is(cachedFile));
    }

    @Test
    void resolveOnAnEmptyCacheWithAFetcherIoExceptionThrowsAndLeavesNoFileBehind(@TempDir final Path cacheRoot) {
        final ThirdPartyJarCache cache = new ThirdPartyJarCache(cacheRoot, (url, target) -> {
            throw new IOException("connection refused");
        });

        final org.infernus.idea.checkstyle.exception.ThirdPartyJarDownloadException ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        org.infernus.idea.checkstyle.exception.ThirdPartyJarDownloadException.class,
                        () -> cache.resolve(URL));

        assertThat(ex.getCause(), org.hamcrest.Matchers.instanceOf(IOException.class));
        assertThat(java.util.Arrays.asList(cacheRoot.toFile().list()), org.hamcrest.Matchers.empty());
    }

    private static byte[] validZipBytes() throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("entry.txt"));
            zip.write("content".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static String sha256Hex(final String value) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
