package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class PluginConfigurationManager {

    private final List<ConfigurationListener> configurationListeners = new CopyOnWriteArrayList<>();

    private final Project project;

    private volatile PluginConfiguration cachedConfiguration;

    public PluginConfigurationManager(@NotNull final Project project) {
        this.project = project;
    }

    public void addConfigurationListener(final ConfigurationListener configurationListener) {
        if (configurationListener != null) {
            configurationListeners.add(configurationListener);
        }
    }

    public void removeConfigurationListener(final ConfigurationListener configurationListener) {
        configurationListeners.remove(configurationListener);
    }

    private void fireConfigurationChanged() {
        for (ConfigurationListener configurationListener : configurationListeners) {
            configurationListener.configurationChanged();
        }
    }

    public synchronized void disableActiveConfiguration() {
        setCurrent(PluginConfigurationBuilder.from(getCurrent())
                .withActiveLocationIds(new TreeSet<>())
                .build(), true);
    }

    @NotNull
    public synchronized PluginConfiguration getCurrent() {
        if (cachedConfiguration != null) {
            return cachedConfiguration;
        }

        cachedConfiguration = projectConfigurationState()
                .populate(PluginConfigurationBuilder.defaultConfiguration(project))
                .build();
        return cachedConfiguration;
    }

    public synchronized void setCurrent(@NotNull final PluginConfiguration updatedConfiguration, final boolean fireEvents) {
        cachedConfiguration = null;

        projectConfigurationState().setCurrentConfig(updatedConfiguration);
        if (fireEvents) {
            fireConfigurationChanged();
        }
    }

    private ProjectConfigurationState projectConfigurationState() {
        return project.getService(ProjectConfigurationState.class);
    }
}
