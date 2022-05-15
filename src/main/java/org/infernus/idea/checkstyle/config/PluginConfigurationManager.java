package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PluginConfigurationManager {

    private final List<ConfigurationListener> configurationListeners = Collections.synchronizedList(new ArrayList<>());

    private final Project project;

    public PluginConfigurationManager(@NotNull final Project project) {
        this.project = project;
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
                .withActiveLocationIds(null)
                .build(), true);
    }

    @NotNull
    public PluginConfiguration getCurrent() {
        final PluginConfigurationBuilder defaultConfig = PluginConfigurationBuilder.defaultConfiguration(project);
        return projectConfigurationState()
                .populate(applicationConfigurationState().populate(defaultConfig))
                .build();
    }

    public void setCurrent(@NotNull final PluginConfiguration updatedConfiguration, final boolean fireEvents) {
        projectConfigurationState().setCurrentConfig(updatedConfiguration);
        applicationConfigurationState().setCurrentConfig(updatedConfiguration);
        if (fireEvents) {
            fireConfigurationChanged();
        }
    }

    public static PluginConfigurationManager getInstance(@NotNull final Project project) {
        return ServiceManager.getService(project, PluginConfigurationManager.class);
    }

    private ProjectConfigurationState projectConfigurationState() {
        return ServiceManager.getService(project, ProjectConfigurationState.class);
    }

    private ApplicationConfigurationState applicationConfigurationState() {
        return ServiceManager.getService(ApplicationConfigurationState.class);
    }
}
