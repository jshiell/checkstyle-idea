package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@State(name = CheckStylePlugin.ID_PLUGIN + "-app", storages = {@Storage("checkstyle-idea.xml")})
public class ApplicationConfigurationState
        implements PersistentStateComponent<ApplicationConfigurationState.ApplicationSettings> {

    private static final String LAST_ACTIVE_PLUGIN_VERSION = "last-active-plugin-version";

    private ApplicationSettings applicationSettings = defaultApplicationSettings();

    @NotNull
    private ApplicationSettings defaultApplicationSettings() {
        return ApplicationSettings.create(CheckStylePlugin.version());
    }

    public ApplicationSettings getState() {
        return applicationSettings;
    }

    public void loadState(@NotNull final ApplicationSettings sourceApplicationSettings) {
        applicationSettings = sourceApplicationSettings;
    }

    @NotNull
    PluginConfigurationBuilder populate(@NotNull final PluginConfigurationBuilder builder) {
        Map<String, String> settingsMap = applicationSettings.configuration();
        return builder
                .withLastActivePluginVersion(settingsMap.get(LAST_ACTIVE_PLUGIN_VERSION));
    }

    void setCurrentConfig(@NotNull final PluginConfiguration currentPluginConfig) {
        applicationSettings = ApplicationSettings.create(currentPluginConfig.getLastActivePluginVersion());
    }

    static class ApplicationSettings {
        @MapAnnotation
        private Map<String, String> configuration;

        static ApplicationSettings create(final String lastActivePluginVersion) {
            final Map<String, String> mapForSerialization = new TreeMap<>();

            mapForSerialization.put(LAST_ACTIVE_PLUGIN_VERSION, lastActivePluginVersion);

            final ApplicationSettings applicationSettings = new ApplicationSettings();
            applicationSettings.configuration = mapForSerialization;
            return applicationSettings;
        }

        @NotNull
        public Map<String, String> configuration() {
            return Objects.requireNonNullElseGet(configuration, TreeMap::new);
        }
    }
}
