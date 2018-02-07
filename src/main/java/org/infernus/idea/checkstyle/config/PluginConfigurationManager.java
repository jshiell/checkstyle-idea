package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PluginConfigurationManager {
    public static final String PROJECT_DIR = "$PRJ_DIR$";

    private final List<ConfigurationListener> configurationListeners = Collections.synchronizedList(new ArrayList<>());

    private final Project project;
    private final ProjectConfigurationState projectConfigurationState;
    private final WorkspaceConfigurationState workspaceConfigurationState;

    public PluginConfigurationManager(@NotNull final Project project,
                                      @NotNull final ProjectConfigurationState projectConfigurationState,
                                      @NotNull final WorkspaceConfigurationState workspaceConfigurationState) {
        this.project = project;
        this.projectConfigurationState = projectConfigurationState;
        this.workspaceConfigurationState = workspaceConfigurationState;
    }


    public void addConfigurationListener(final ConfigurationListener configurationListener) {
        if (configurationListener != null) {
            configurationListeners.add(configurationListener);
        }
    }

    private void fireConfigurationChanged() {
        synchronized (configurationListeners) {
            for (ConfigurationListener configurationListener : configurationListeners) {
                configurationListener.configurationChanged();
            }
        }
    }

    public void disableActiveConfiguration() {
        setCurrent(PluginConfigurationBuilder.from(getCurrent())
                .withActiveLocation(null)
                .build(), true);
    }

    @NotNull
    public PluginConfiguration getCurrent() {
        final PluginConfigurationBuilder defaultConfig = PluginConfigurationBuilder.defaultConfiguration(project);
        return workspaceConfigurationState
                .populate(projectConfigurationState.populate(defaultConfig))
                .build();
    }

    public void setCurrent(@NotNull final PluginConfiguration updatedConfiguration, final boolean fireEvents) {
        projectConfigurationState.setCurrentConfig(updatedConfiguration);
        workspaceConfigurationState.setCurrentConfig(updatedConfiguration);
        if (fireEvents) {
            fireConfigurationChanged();
        }
    }

    public static PluginConfigurationManager getInstance(@NotNull final Project project) {
        return ServiceManager.getService(project, PluginConfigurationManager.class);
    }
}
