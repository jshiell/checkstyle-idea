package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

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
                .withActiveLocationIds(new TreeSet<>())
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
        return project.getService(PluginConfigurationManager.class);
    }

    private ProjectConfigurationState projectConfigurationState() {
        return project.getService(ProjectConfigurationState.class);
    }

    private ApplicationConfigurationState applicationConfigurationState() {
        return ApplicationManager.getApplication().getService(ApplicationConfigurationState.class);
    }
}
