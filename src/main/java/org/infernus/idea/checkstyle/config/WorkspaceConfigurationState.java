package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.TreeMap;

import static org.infernus.idea.checkstyle.config.PluginConfigurationBuilder.defaultConfiguration;

@State(name = CheckStylePlugin.ID_PLUGIN + "-workspace", storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)})
public class WorkspaceConfigurationState
        implements PersistentStateComponent<WorkspaceConfigurationState.WorkspaceSettings> {

    private static final String LAST_ACTIVE_PLUGIN_VERSION = "last-active-plugin-version";

    private final Project project;

    private WorkspaceSettings workspaceSettings;

    public WorkspaceConfigurationState(@NotNull final Project project) {
        this.project = project;

        workspaceSettings = defaultWorkspaceSettings();
    }

    @NotNull
    private WorkspaceSettings defaultWorkspaceSettings() {
        return new WorkspaceSettings(defaultConfiguration(project).build());
    }

    public WorkspaceSettings getState() {
        return workspaceSettings;
    }

    public void loadState(final WorkspaceSettings sourceWorkspaceSettings) {
        if (sourceWorkspaceSettings != null) {
            workspaceSettings = sourceWorkspaceSettings;
        } else {
            workspaceSettings = defaultWorkspaceSettings();
        }
    }

    @NotNull
    PluginConfigurationBuilder populate(@NotNull final PluginConfigurationBuilder builder) {
        Map<String, String> settingsMap = workspaceSettings.getConfiguration();
        return builder
                .withLastActivePluginVersion(settingsMap.get(LAST_ACTIVE_PLUGIN_VERSION));
    }

    void setCurrentConfig(@NotNull final PluginConfiguration currentPluginConfig) {
        workspaceSettings = new WorkspaceSettings(currentPluginConfig);
    }

    static class WorkspaceSettings {
        private Map<String, String> configuration;

        /**
         * No-args constructor for deserialization.
         */
        public WorkspaceSettings() {
            super();
        }

        public WorkspaceSettings(@NotNull final PluginConfiguration currentPluginConfig) {
            final Map<String, String> mapForSerialization = new TreeMap<>();

            mapForSerialization.put(LAST_ACTIVE_PLUGIN_VERSION, currentPluginConfig.getLastActivePluginVersion());

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
