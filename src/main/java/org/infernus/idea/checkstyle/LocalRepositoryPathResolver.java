package org.infernus.idea.checkstyle;

import com.intellij.openapi.diagnostic.Logger;
import org.infernus.idea.checkstyle.maven.MavenMirrorUrlResolver;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Resolves the local Maven repository root to use as the Checkstyle-artifact download cache, in order
 * of precedence: a {@code <localRepository>} override from the user's Maven {@code settings.xml}, or
 * {@link CheckstyleArtifactDownloader#defaultM2Root()} as the default.
 */
public class LocalRepositoryPathResolver {

    private static final Logger LOG = Logger.getInstance(LocalRepositoryPathResolver.class);

    private final Supplier<Optional<Path>> mavenSettingsSupplier;

    public LocalRepositoryPathResolver() {
        this(MavenMirrorUrlResolver::resolveLocalRepositoryOverride);
    }

    LocalRepositoryPathResolver(@NotNull final Supplier<Optional<Path>> mavenSettingsSupplier) {
        this.mavenSettingsSupplier = mavenSettingsSupplier;
    }

    @NotNull
    public Path resolve() {
        try {
            return mavenSettingsSupplier.get().orElseGet(CheckstyleArtifactDownloader::defaultM2Root);
        } catch (Throwable t) {
            LOG.warn("Failed to resolve a Maven settings.xml local repository override", t);
            return CheckstyleArtifactDownloader.defaultM2Root();
        }
    }
}
