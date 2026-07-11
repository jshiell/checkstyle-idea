package org.infernus.idea.checkstyle;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState;
import org.infernus.idea.checkstyle.maven.MavenMirrorUrlResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Resolves the base URL to download non-bundled Checkstyle artifacts from, in order of precedence:
 * a manual override in {@link ApplicationConfigurationState}, a mirror detected from the user's Maven
 * {@code settings.xml}, or Maven Central as the default.
 */
public class ArtifactDownloadBaseUrlResolver {

    private static final Logger LOG = Logger.getInstance(ArtifactDownloadBaseUrlResolver.class);
    static final String DEFAULT_BASE_URL = "https://repo1.maven.org/maven2/";

    private final Supplier<String> overrideSupplier;
    private final Supplier<Optional<String>> mavenMirrorSupplier;

    public ArtifactDownloadBaseUrlResolver() {
        this(() -> {
                    Application application = ApplicationManager.getApplication();
                    if (application == null) {
                        return null;
                    }
                    return application.getService(ApplicationConfigurationState.class)
                            .getArtifactRepositoryBaseUrlOverride();
                },
                MavenMirrorUrlResolver::resolveCentralMirror);
    }

    ArtifactDownloadBaseUrlResolver(@NotNull final Supplier<String> overrideSupplier,
                                    @NotNull final Supplier<Optional<String>> mavenMirrorSupplier) {
        this.overrideSupplier = overrideSupplier;
        this.mavenMirrorSupplier = mavenMirrorSupplier;
    }

    @NotNull
    public String resolve() {
        String override = overrideSupplier.get();
        if (override != null && !override.isBlank()) {
            if (isValidHttpUrl(override)) {
                return override;
            }
            LOG.warn("Ignoring invalid artifact download mirror override: " + override);
        }

        try {
            Optional<String> mirrored = mavenMirrorSupplier.get();
            if (mirrored.isPresent()) {
                return mirrored.get();
            }
        } catch (Throwable t) {
            LOG.warn("Failed to resolve a Maven settings.xml mirror for artifact downloads", t);
        }

        return DEFAULT_BASE_URL;
    }

    private static boolean isValidHttpUrl(@Nullable final String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        try {
            String scheme = new URI(candidate).getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
