package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.TreeMap;

@State(name = CheckStylePlugin.ID_PLUGIN + "-app", storages = {@Storage("checkstyle-idea.xml")})
public class ApplicationConfigurationState
        implements PersistentStateComponent<ApplicationConfigurationState.ApplicationSettings> {

    private static final String LAST_ACTIVE_PLUGIN_VERSION = "last-active-plugin-version";

    private ApplicationSettings applicationSettings = defaultApplicationSettings();

    @NotNull
    private ApplicationSettings defaultApplicationSettings() {
        return new ApplicationSettings(CheckStylePlugin.version());
    }

    public ApplicationSettings getState() {
        return applicationSettings;
    }

    public void loadState(final ApplicationSettings sourceApplicationSettings) {
        if (sourceApplicationSettings != null) {
            applicationSettings = sourceApplicationSettings;
        } else {
            applicationSettings = defaultApplicationSettings();
        }
    }

    @NotNull
    PluginConfigurationBuilder populate(@NotNull final PluginConfigurationBuilder builder) {
        Map<String, String> settingsMap = applicationSettings.getConfiguration();
        return builder
                .withLastActivePluginVersion(settingsMap.get(LAST_ACTIVE_PLUGIN_VERSION));
    }

    void setCurrentConfig(@NotNull final PluginConfiguration currentPluginConfig) {
        applicationSettings = new ApplicationSettings(currentPluginConfig.getLastActivePluginVersion());
    }

    static class ApplicationSettings {
        private Map<String, String> configuration;

        /**
         * No-args constructor for deserialization.
         */
        @SuppressWarnings("unused")
        public ApplicationSettings() {
            super();
        }

        public ApplicationSettings(final String lastActivePluginVersion) {
            final Map<String, String> mapForSerialization = new TreeMap<>();

            mapForSerialization.put(LAST_ACTIVE_PLUGIN_VERSION, lastActivePluginVersion);

            configuration = mapForSerialization;
        }

        public void setConfiguration(final Map<String, String> deserialisedConfiguration) {
            configuration = deserialisedConfiguration;
        }

        @NotNull
        public Map<String, String> getConfiguration() {
            if (configuration == null) {
                return new TreeMap<>();
            }
            return configuration;
        }
    }
}
