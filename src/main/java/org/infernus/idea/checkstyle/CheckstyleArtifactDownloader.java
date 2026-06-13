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

    public CheckstyleArtifactDownloader(@NotNull final Path m2LocalRepository,
                                        @NotNull final ArtifactResolver resolver) {
        this.m2LocalRepository = m2LocalRepository;
        this.resolver = resolver;
    }

    @NotNull
    public static Path defaultM2Root() {
        return Path.of(System.getProperty("user.home"), ".m2", "repository");
    }

    @NotNull
    public static CheckstyleArtifactDownloader create(@NotNull final Path m2Root) {
        DownloadManifest manifest = DownloadManifest.fromClasspath();
        ManifestBasedArtifactResolver resolver = new ManifestBasedArtifactResolver(
                manifest, m2Root, new HttpJarDownloader());
        return new CheckstyleArtifactDownloader(m2Root, resolver);
    }

    public static boolean isAvailableLocally(@NotNull final Path m2LocalRepository,
                                             @NotNull final String version) {
        return primaryJarPath(m2LocalRepository, version).toFile().exists();
    }

    public boolean isAvailableLocally(@NotNull final String version) {
        return isAvailableLocally(m2LocalRepository, version);
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
    private static Path primaryJarPath(@NotNull final Path m2Root,
                                       @NotNull final String version) {
        return m2Root
                .resolve("com/puppycrawl/tools/checkstyle")
                .resolve(version)
                .resolve("checkstyle-" + version + ".jar");
    }
}
