package org.infernus.idea.checkstyle;

import org.infernus.idea.checkstyle.exception.CheckstyleDownloadException;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;


/**
 * Resolves Checkstyle JARs for non-bundled versions, using the Maven local repository as cache.
 */
public class CheckstyleArtifactDownloader {

    public interface ArtifactResolver {
        List<Path> resolveTransitively(String groupId, String artifactId, String version)
                throws Exception;
    }

    private final Path m2LocalRepository;
    private final ArtifactResolver resolver;

    CheckstyleArtifactDownloader(@NotNull final Path m2LocalRepository,
                                 @NotNull final ArtifactResolver resolver) {
        this.m2LocalRepository = m2LocalRepository;
        this.resolver = resolver;
    }

    public boolean isAvailableLocally(@NotNull final String version) {
        return primaryJarPath(version).toFile().exists();
    }

    @NotNull
    public List<Path> download(@NotNull final String version) {
        try {
            return resolver.resolveTransitively("com.puppycrawl.tools", "checkstyle", version);
        } catch (Exception e) {
            throw new CheckstyleDownloadException(
                    "Failed to download Checkstyle " + version, e);
        }
    }

    @NotNull
    private Path primaryJarPath(@NotNull final String version) {
        return m2LocalRepository
                .resolve("com/puppycrawl/tools/checkstyle")
                .resolve(version)
                .resolve("checkstyle-" + version + ".jar");
    }
}
