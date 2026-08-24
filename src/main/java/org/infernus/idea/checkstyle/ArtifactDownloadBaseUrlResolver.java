package org.infernus.idea.checkstyle;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.infernus.idea.checkstyle.config.ApplicationConfigurationState;
import org.infernus.idea.checkstyle.config.PasswordSafeArtifactRepositoryCredentialsStore;
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

    private final Supplier<Optional<ArtifactRepositoryLocation>> overrideSupplier;
    private final Supplier<Optional<ArtifactRepositoryLocation>> mavenMirrorSupplier;

    public ArtifactDownloadBaseUrlResolver() {
        this(() -> {
                    Application application = ApplicationManager.getApplication();
                    if (application == null) {
                        return Optional.empty();
                    }
                    ApplicationConfigurationState state =
                            application.getService(ApplicationConfigurationState.class);
                    String override = state.getArtifactRepositoryBaseUrlOverride();
                    if (override == null || override.isBlank()) {
                        return Optional.empty();
                    }
                    String username = state.getArtifactRepositoryOverrideUsername();
                    Optional<ArtifactRepositoryCredentials> credentials = (username == null || username.isBlank())
                            ? Optional.empty()
                            : ArtifactRepositoryCredentials.of(username,
                                    new PasswordSafeArtifactRepositoryCredentialsStore()
                                            .getPassword(username).orElse(null));
                    return Optional.of(new ArtifactRepositoryLocation(override, credentials));
                },
                () -> MavenMirrorUrlResolver.resolveCentralMirror()
                        .map(url -> new ArtifactRepositoryLocation(url, Optional.empty())));
    }

    ArtifactDownloadBaseUrlResolver(@NotNull final Supplier<Optional<ArtifactRepositoryLocation>> overrideSupplier,
                                    @NotNull final Supplier<Optional<ArtifactRepositoryLocation>> mavenMirrorSupplier) {
        this.overrideSupplier = overrideSupplier;
        this.mavenMirrorSupplier = mavenMirrorSupplier;
    }

    @NotNull
    public ArtifactRepositoryLocation resolve() {
        Optional<ArtifactRepositoryLocation> override = overrideSupplier.get();
        if (override.isPresent()) {
            if (isValidHttpUrl(override.get().baseUrl())) {
                return override.get();
            }
            LOG.warn("Ignoring invalid artifact download mirror override: " + override.get().baseUrl());
        }

        try {
            Optional<ArtifactRepositoryLocation> mirrored = mavenMirrorSupplier.get();
            if (mirrored.isPresent()) {
                return mirrored.get();
            }
        } catch (Throwable t) {
            LOG.warn("Failed to resolve a Maven settings.xml mirror for artifact downloads", t);
        }

        return new ArtifactRepositoryLocation(DEFAULT_BASE_URL, Optional.empty());
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
