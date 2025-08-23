package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import com.intellij.platform.ide.progress.TasksKt;
import com.intellij.util.io.HttpRequests;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.jetbrains.annotations.NotNull;

public class CheckstyleDownloader {

    private final Project project;

    public CheckstyleDownloader(@NotNull final Project project) {
        this.project = project;
    }

    public void downloadVersion(@NotNull final String version, @NotNull final Path outputPath)
        throws IOException {
        if (!Files.exists(outputPath.getParent())) {
            Files.createDirectories(outputPath.getParent());
        }

        TasksKt.runWithModalProgressBlocking(project, "Downloading Checkstyle",
            (scope, continuation) -> {
                try {
                    var currentDownloadAttempt = 0;
                    IOException exception = null;
                    final var maxDownloadAttempts = 3;
                    while (currentDownloadAttempt < maxDownloadAttempts) {
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
                    throw new RuntimeException(e);
                }

                return null;
            });
    }

    @NotNull
    public Path getPathToVersion(@NotNull final String version) {
        return getCacheDirectoryPath().resolve(getArtifactName(version));
    }

    @NotNull
    private String getArtifactName(@NotNull final String version) {
        return "checkstyle-" + version + "-all.jar";
    }

    @NotNull
    private Path getCacheDirectoryPath() {
        final var pluginConfiguration = project.getService(PluginConfigurationManager.class)
            .getCurrent();

        return pluginConfiguration.getCachePath();
    }

    private void tryDownload(final String version, final Path outputPath) throws IOException {
        final var pluginConfiguration = project.getService(PluginConfigurationManager.class)
            .getCurrent();
        String baseUrl = pluginConfiguration.getBaseDownloadUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        final var finalBaseUrl = baseUrl;

        HttpRequests.request(
                finalBaseUrl + "/checkstyle-" + version + "/" + getArtifactName(version))
            .saveToFile(outputPath.toFile(), null);
    }
}
