package org.infernus.idea.checkstyle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.io.HttpRequests;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState;
import org.jetbrains.annotations.NotNull;

public class CheckstyleDownloader {

    public void deleteVersion(@NotNull final String version) throws IOException {
        Files.delete(getArtifactPath(version));
    }

    public void downloadVersion(@NotNull final String version) throws IOException {
        final var outputPath = getArtifactPath(version);
        if (!Files.exists(outputPath.getParent())) {
            Files.createDirectories(outputPath.getParent());
        }

        try {
            var currentDownloadAttempt = 0;
            IOException exception = null;
            while (currentDownloadAttempt < 3) {
                currentDownloadAttempt++;

                try {
                    tryDownload(version, outputPath);
                    exception = null;
                } catch (final IOException ioException) {
                    exception = ioException;
                }
            }

            if (exception != null) {
                throw exception;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path getArtifactPath(final String version) {
        return ApplicationManager.getApplication().getService(ApplicationConfigurationState.class)
            .getState().getCachePath().resolve(getArtifactName(version));
    }

    @NotNull
    private String getArtifactName(@NotNull final String version) {
        return "checkstyle-" + version + "-all.jar";
    }

    @NotNull
    private String getBaseDownloadUrl() {
        return ApplicationManager.getApplication().getService(ApplicationConfigurationState.class)
            .getState().getBaseDownloadUrl();
    }

    private void tryDownload(final String version, final Path outputPath) throws IOException {
        var baseUrl = getBaseDownloadUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        HttpRequests.request(baseUrl + "/checkstyle-" + version + "/" + getArtifactName(version))
            .saveToFile(outputPath.toFile(), null);
    }
}
