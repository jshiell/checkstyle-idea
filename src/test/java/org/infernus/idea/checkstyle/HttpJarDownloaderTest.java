package org.infernus.idea.checkstyle;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;


public class HttpJarDownloaderTest {

    @TempDir
    Path tempDir;

    private HttpServer server;
    private String baseUrl;
    private HttpJarDownloader downloader;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
        downloader = new HttpJarDownloader();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void writesFileOnSuccess() throws Exception {
        byte[] content = {1, 2, 3};
        server.createContext("/jar", exchange -> {
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
            exchange.getResponseBody().close();
        });

        Path target = tempDir.resolve("checkstyle.jar");
        downloader.download(baseUrl + "/jar", target, Optional.empty());

        assertArrayEquals(content, Files.readAllBytes(target));
    }

    @Test
    void leavesNoFileOnNon200() {
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.getResponseBody().close();
        });

        Path target = tempDir.resolve("checkstyle.jar");
        assertThrows(IOException.class, () -> downloader.download(baseUrl + "/missing", target, Optional.empty()));
        assertFalse(Files.exists(target));
    }

    @Test
    void throwsIllegalArgumentExceptionOnMalformedUrl() {
        Path target = tempDir.resolve("checkstyle.jar");
        assertThrows(IllegalArgumentException.class,
                () -> downloader.download("not a valid url :// @@", target, Optional.empty()));
    }

    @Test
    void sendsNoAuthorizationHeaderWhenCredentialsAbsent() throws Exception {
        AtomicReference<String> seenHeader = new AtomicReference<>();
        byte[] content = {1, 2, 3};
        server.createContext("/jar", exchange -> {
            seenHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
            exchange.getResponseBody().close();
        });

        downloader.download(baseUrl + "/jar", tempDir.resolve("checkstyle.jar"), Optional.empty());

        assertNull(seenHeader.get());
    }

    @Test
    void sendsBasicAuthorizationHeaderWithCorrectEncodingWhenCredentialsPresent() throws Exception {
        AtomicReference<String> seenHeader = new AtomicReference<>();
        byte[] content = {1, 2, 3};
        server.createContext("/jar", exchange -> {
            seenHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
            exchange.getResponseBody().close();
        });

        downloader.download(baseUrl + "/jar", tempDir.resolve("checkstyle.jar"),
                Optional.of(new ArtifactRepositoryCredentials("user", "pass")));

        String expected = "Basic " + Base64.getEncoder().encodeToString("user:pass".getBytes());
        assertEquals(expected, seenHeader.get());
    }

    @Test
    void succeedsOnlyWhenCredentialsSupplied() throws Exception {
        server.createContext("/secure", exchange -> {
            String header = exchange.getRequestHeaders().getFirst("Authorization");
            String expected = "Basic " + Base64.getEncoder().encodeToString("user:pass".getBytes());
            if (expected.equals(header)) {
                byte[] content = {4, 5, 6};
                exchange.sendResponseHeaders(200, content.length);
                exchange.getResponseBody().write(content);
                exchange.getResponseBody().close();
            } else {
                exchange.sendResponseHeaders(401, -1);
                exchange.getResponseBody().close();
            }
        });

        Path target = tempDir.resolve("checkstyle.jar");
        assertThrows(IOException.class, () -> downloader.download(baseUrl + "/secure", target, Optional.empty()));
        assertFalse(Files.exists(target));

        assertDoesNotThrow(() -> downloader.download(baseUrl + "/secure", target,
                Optional.of(new ArtifactRepositoryCredentials("user", "pass"))));
        assertArrayEquals(new byte[]{4, 5, 6}, Files.readAllBytes(target));
    }

    @Test
    void doesNotForwardAuthorizationHeaderAcrossCrossHostRedirect() throws Exception {
        HttpServer targetServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> seenHeader = new AtomicReference<>();
        try {
            byte[] content = {7, 8, 9};
            targetServer.createContext("/target", exchange -> {
                seenHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
                exchange.sendResponseHeaders(200, content.length);
                exchange.getResponseBody().write(content);
                exchange.getResponseBody().close();
            });
            targetServer.start();
            String redirectTarget = "http://127.0.0.1:" + targetServer.getAddress().getPort() + "/target";

            server.createContext("/redirect", exchange -> {
                exchange.getResponseHeaders().add("Location", redirectTarget);
                exchange.sendResponseHeaders(302, -1);
                exchange.getResponseBody().close();
            });

            downloader.download(baseUrl + "/redirect", tempDir.resolve("checkstyle.jar"),
                    Optional.of(new ArtifactRepositoryCredentials("user", "pass")));

            assertNull(seenHeader.get());
        } finally {
            targetServer.stop(0);
        }
    }
}
