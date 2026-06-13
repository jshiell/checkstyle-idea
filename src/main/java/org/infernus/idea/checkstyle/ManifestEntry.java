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
    public String mavenCentralUrl() {
        String groupPath = groupId.replace('.', '/');
        String filename = classifier.isEmpty()
                ? artifactId + "-" + version + ".jar"
                : artifactId + "-" + version + "-" + classifier + ".jar";
        return "https://repo1.maven.org/maven2/" + groupPath + "/" + artifactId + "/" + version + "/" + filename;
    }
}
