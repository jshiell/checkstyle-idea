package org.infernus.idea.checkstyle;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;


public class HttpJarDownloader implements ManifestBasedArtifactResolver.JarDownloader {

    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private final HttpClient httpClient;

    public HttpJarDownloader() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public void download(@NotNull final String url, @NotNull final Path target,
                          @NotNull final Optional<ArtifactRepositoryCredentials> credentials) throws IOException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET();
        credentials.ifPresent(c -> requestBuilder.header("Authorization", basicAuthHeader(c)));
        HttpRequest request = requestBuilder.build();
        Path tmp = Files.createTempFile(target.getParent(), ".download-", ".part");
        try {
            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tmp));
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " downloading " + url);
            }
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted: " + url, e);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @NotNull
    private static String basicAuthHeader(@NotNull final ArtifactRepositoryCredentials credentials) {
        String userPass = credentials.username() + ":" + credentials.password();
        return "Basic " + Base64.getEncoder().encodeToString(userPass.getBytes(StandardCharsets.UTF_8));
    }
}
