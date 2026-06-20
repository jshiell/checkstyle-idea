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
        downloader.download(baseUrl + "/jar", target);

        assertArrayEquals(content, Files.readAllBytes(target));
    }

    @Test
    void leavesNoFileOnNon200() {
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.getResponseBody().close();
        });

        Path target = tempDir.resolve("checkstyle.jar");
        assertThrows(IOException.class, () -> downloader.download(baseUrl + "/missing", target));
        assertFalse(Files.exists(target));
    }

    @Test
    void throwsIllegalArgumentExceptionOnMalformedUrl() {
        Path target = tempDir.resolve("checkstyle.jar");
        assertThrows(IllegalArgumentException.class,
                () -> downloader.download("not a valid url :// @@", target));
    }
}
