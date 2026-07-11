package org.infernus.idea.checkstyle;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;


public record ManifestEntry(
        @NotNull String groupId,
        @NotNull String artifactId,
        @NotNull String version,
        @NotNull String classifier,
        @NotNull String sha256hex) {

    @NotNull
    public Path m2Path(@NotNull final Path m2Root) {
        String groupPath = groupId.replace('.', '/');
        String filename = classifier.isEmpty()
                ? artifactId + "-" + version + ".jar"
                : artifactId + "-" + version + "-" + classifier + ".jar";
        return m2Root.resolve(groupPath).resolve(artifactId).resolve(version).resolve(filename);
    }

    @NotNull
    public String artifactUrl(@NotNull final String baseUrl) {
        String normalisedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String groupPath = groupId.replace('.', '/');
        String filename = classifier.isEmpty()
                ? artifactId + "-" + version + ".jar"
                : artifactId + "-" + version + "-" + classifier + ".jar";
        return normalisedBaseUrl + "/" + groupPath + "/" + artifactId + "/" + version + "/" + filename;
    }
}
