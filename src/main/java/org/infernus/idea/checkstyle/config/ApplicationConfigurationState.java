package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Text;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

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

    @Nullable
    public String getArtifactRepositoryOverrideUsername() {
        return applicationSettings.artifactRepositoryOverrideUsername;
    }

    public void setArtifactRepositoryOverrideUsername(@Nullable final String artifactRepositoryOverrideUsername) {
        applicationSettings.artifactRepositoryOverrideUsername = artifactRepositoryOverrideUsername;
    }

    @NotNull
    public List<GlobalConfigurationLocation> getGlobalLocations() {
        List<GlobalConfigurationLocation> globalConfigurationLocations = requireNonNullElse(applicationSettings.globalLocations, Collections.emptyList());
        return Collections.unmodifiableList(globalConfigurationLocations);
    }

    public void setGlobalLocations(@NotNull final List<GlobalConfigurationLocation> globalLocations) {
        applicationSettings.globalLocations = new ArrayList<>(globalLocations);
    }

    @NotNull
    public List<String> getActiveGlobalLocationIds() {
        List<String> activeGlobalLocationIds = requireNonNullElse(applicationSettings.activeGlobalLocationIds, Collections.emptyList());
        return Collections.unmodifiableList(activeGlobalLocationIds);
    }

    public void setActiveGlobalLocationIds(@NotNull final List<String> activeGlobalLocationIds) {
        applicationSettings.activeGlobalLocationIds = new ArrayList<>(activeGlobalLocationIds);
    }

    public boolean isUseGlobalRulesByDefault() {
        return applicationSettings.useGlobalRulesByDefault;
    }

    public void setUseGlobalRulesByDefault(final boolean useGlobalRulesByDefault) {
        applicationSettings.useGlobalRulesByDefault = useGlobalRulesByDefault;
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

        @Tag
        public String artifactRepositoryOverrideUsername;

        @MapAnnotation
        public List<GlobalConfigurationLocation> globalLocations;

        @XCollection
        public List<String> activeGlobalLocationIds;

        @Tag
        public boolean useGlobalRulesByDefault;
    }

    /**
     * Serialisation DTO for a globally-configured Checkstyle rule file location, stored in
     * {@code checkstyle-idea-app.xml}. Mirrors the structure of
     * {@link org.infernus.idea.checkstyle.config.ProjectConfigurationState.ConfigurationLocation} but without project-relative scope.
     */
    public static class GlobalConfigurationLocation {

        @Attribute
        public String id;

        @Attribute
        public String type;

        @Attribute
        public String description;

        @Text
        public String location;

        @MapAnnotation
        public Map<String, String> properties;

        @SuppressWarnings("unused") // for serialisation
        public GlobalConfigurationLocation() {
        }

        public GlobalConfigurationLocation(final String id,
                                           final String type,
                                           final String location,
                                           final String description) {
            this.id = id;
            this.type = type;
            this.location = location;
            this.description = description;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            final GlobalConfigurationLocation that = (GlobalConfigurationLocation) other;
            return Objects.equals(id, that.id)
                    && Objects.equals(type, that.type)
                    && Objects.equals(location, that.location)
                    && Objects.equals(description, that.description)
                    && Objects.equals(properties, that.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, type, location, description, properties);
        }

        @Override
        public String toString() {
            return "GlobalConfigurationLocation{"
                    + "id='" + id + '\''
                    + ", type='" + type + '\''
                    + ", description='" + description + '\''
                    + ", location='" + location + '\''
                    + '}';
        }
    }
}
