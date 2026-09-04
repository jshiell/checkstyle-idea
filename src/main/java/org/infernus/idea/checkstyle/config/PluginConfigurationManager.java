package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class PluginConfigurationManager {

    private final List<ConfigurationListener> configurationListeners = new CopyOnWriteArrayList<>();

    private final Project project;

    private final Consumer<Runnable> edtDispatcher;

    private volatile PluginConfiguration cachedConfiguration;

    public PluginConfigurationManager(@NotNull final Project project) {
        this(project, runnable -> {
            var app = ApplicationManager.getApplication();
            if (app != null) {
                app.invokeLater(runnable);
            } else {
                // Headless or test environment where Application is not available - run synchronously.
                runnable.run();
            }
        });
    }

    PluginConfigurationManager(@NotNull final Project project, @NotNull final Consumer<Runnable> edtDispatcher) {
        this.project = project;
        this.edtDispatcher = edtDispatcher;
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
        edtDispatcher.accept(() -> {
            for (ConfigurationListener configurationListener : configurationListeners) {
                configurationListener.configurationChanged();
            }
        });
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

    /**
     * {@link #getCurrent()} and {@link #setCurrent(PluginConfiguration, boolean)} share the same
     * {@link #cachedConfiguration} field.
     * <p>
     * This method needs to be synchronized to avoid a race condition
     * where one thread is reading the cached configuration while another thread is writing to it.
     */
    public synchronized void invalidate() {
        cachedConfiguration = null;
        fireConfigurationChanged();
    }

    private ProjectConfigurationState projectConfigurationState() {
        return project.getService(ProjectConfigurationState.class);
    }
}
