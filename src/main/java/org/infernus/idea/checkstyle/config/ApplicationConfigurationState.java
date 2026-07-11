package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Application-wide (IDE-wide) CheckStyle plugin configuration. Registered as an {@code applicationService}.
 */
@State(
        name = "CheckStyle-IDEA-Application",
        storages = {@Storage(value = "checkstyle-idea-app.xml", roamingType = RoamingType.DISABLED)}
)
public final class ApplicationConfigurationState
        implements PersistentStateComponent<ApplicationConfigurationState.ApplicationSettings> {

    private ApplicationSettings applicationSettings = new ApplicationSettings();

    @Nullable
    public String getArtifactRepositoryBaseUrlOverride() {
        return applicationSettings.artifactRepositoryBaseUrlOverride;
    }

    public void setArtifactRepositoryBaseUrlOverride(@Nullable final String artifactRepositoryBaseUrlOverride) {
        applicationSettings.artifactRepositoryBaseUrlOverride = artifactRepositoryBaseUrlOverride;
    }

    @Override
    @NotNull
    public ApplicationSettings getState() {
        return applicationSettings;
    }

    @Override
    public void loadState(@NotNull final ApplicationSettings sourceApplicationSettings) {
        this.applicationSettings = sourceApplicationSettings;
    }

    public static class ApplicationSettings {

        @Tag
        public String artifactRepositoryBaseUrlOverride;
    }
}
