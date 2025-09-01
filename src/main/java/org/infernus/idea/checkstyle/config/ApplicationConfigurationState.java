package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.jetbrains.annotations.NotNull;

@State(name = CheckStylePlugin.ID_PLUGIN + "-app", storages = {@Storage("checkstyle-idea.xml")})
public class ApplicationConfigurationState implements
    PersistentStateComponent<ApplicationConfigurationState.ApplicationSettings> {

    private static final String BASE_DOWNLOAD_URL = "base-download-url";
    private static final String CACHE_PATH = "cache-path";
    private static final String LAST_ACTIVE_PLUGIN_VERSION = "last-active-plugin-version";

    private ApplicationSettings applicationSettings = defaultApplicationSettings();

    @NotNull
    private ApplicationSettings defaultApplicationSettings() {
        return ApplicationSettings.create(CheckStylePlugin.version());
    }

    @NotNull
    public ApplicationSettings getState() {
        return applicationSettings;
    }

    public void loadState(@NotNull final ApplicationSettings sourceApplicationSettings) {
        applicationSettings = sourceApplicationSettings;
    }

    public void setBaseDownloadUrl(@NotNull final String baseDownloadUrl) {
        final var updatedApplicationSettings = new ApplicationSettings(applicationSettings);
        updatedApplicationSettings.setBaseDownloadUrl(baseDownloadUrl);
        applicationSettings = updatedApplicationSettings;
    }

    public void setCachePath(@NotNull final Path cachePath) {
        final var updatedApplicationSettings = new ApplicationSettings(applicationSettings);
        updatedApplicationSettings.setCachePath(cachePath.toString());
        applicationSettings = updatedApplicationSettings;
    }

    @NotNull PluginConfigurationBuilder populate(
        @NotNull final PluginConfigurationBuilder builder) {
        Map<String, String> settingsMap = applicationSettings.configuration();
        return builder.withLastActivePluginVersion(settingsMap.get(LAST_ACTIVE_PLUGIN_VERSION));
    }

    void setCurrentConfig(@NotNull final PluginConfiguration currentPluginConfig) {
        applicationSettings = ApplicationSettings.create(
            currentPluginConfig.getLastActivePluginVersion());
    }

    public static class ApplicationSettings {

        @MapAnnotation
        private Map<String, String> configuration;

        public ApplicationSettings() {

        }

        public ApplicationSettings(final ApplicationSettings applicationSettings) {
            this.configuration = new TreeMap<>();
            this.configuration.putAll(applicationSettings.configuration());
        }

        public static ApplicationSettings create() {
            final ApplicationSettings applicationSettings = new ApplicationSettings();
            applicationSettings.configuration = new TreeMap<>();
            return applicationSettings;
        }

        public static ApplicationSettings create(final String lastActivePluginVersion) {
            final Map<String, String> mapForSerialization = new TreeMap<>();

            mapForSerialization.put(LAST_ACTIVE_PLUGIN_VERSION, lastActivePluginVersion);

            final ApplicationSettings applicationSettings = new ApplicationSettings();
            applicationSettings.configuration = mapForSerialization;
            return applicationSettings;
        }

        @NotNull
        public Map<String, String> configuration() {
            configuration = Objects.requireNonNullElse(configuration, new TreeMap<>());
            return configuration;
        }

        @NotNull
        public String getBaseDownloadUrl() {
            final var config = configuration();
            return Objects.requireNonNullElse(config.get(BASE_DOWNLOAD_URL),
                "https://github.com/checkstyle/checkstyle/releases/download");
        }

        public void setBaseDownloadUrl(@NotNull final String baseDownloadUrl) {
            configuration().put(BASE_DOWNLOAD_URL, baseDownloadUrl);
        }

        @NotNull
        public Path getCachePath() {
            final var config = configuration();
            final String cachePath = config.getOrDefault(CACHE_PATH, "").strip();
            if (cachePath.isBlank()) {
                return Path.of(System.getProperty("java.io.tmpdir"), "checkstyle-idea-cache");
            }

            return Path.of(cachePath);
        }

        public void setCachePath(@NotNull final String cachePath) {
            configuration().put(CACHE_PATH, cachePath);
        }

        public String getLastActivePluginVersion() {
            return Objects.requireNonNullElse(configuration().get(LAST_ACTIVE_PLUGIN_VERSION),
                CheckStylePlugin.version());
        }

        public void setLastActivePluginVersion(final String lastActivePluginVersion) {
            configuration().put(LAST_ACTIVE_PLUGIN_VERSION, lastActivePluginVersion);
        }

    }
}
