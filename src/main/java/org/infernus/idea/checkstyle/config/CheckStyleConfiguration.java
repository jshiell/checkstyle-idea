package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.ConfigurationListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CheckStyleConfiguration {
    public static final String LEGACY_PROJECT_DIR = "$PROJECT_DIR$";
    public static final String PROJECT_DIR = "$PRJ_DIR$";

    private final List<ConfigurationListener> configurationListeners = Collections.synchronizedList(new ArrayList<>());

    private final Project project;
    private final ProjectConfiguration projectConfiguration;

    /**
     * mock instance which may be set and used by unit tests
     */
    private static CheckStyleConfiguration testInstance = null;


    public CheckStyleConfiguration(@NotNull final Project project,
                                   @NotNull final ProjectConfiguration projectConfiguration) {
        this.project = project;
        this.projectConfiguration = projectConfiguration;
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
        setCurrent(PluginConfigDtoBuilder.from(getCurrent())
                .withActiveLocation(null)
                .build(), true);
    }

    @NotNull
    public PluginConfigDto getCurrent() {
        final PluginConfigDtoBuilder defaultConfig = PluginConfigDtoBuilder.defaultConfiguration(project);
        return projectConfiguration
                .populate(defaultConfig)
                .build();
    }

    public void setCurrent(@NotNull final PluginConfigDto updatedConfiguration, final boolean fireEvents) {
        projectConfiguration.setCurrentConfig(updatedConfiguration);
        if (fireEvents) {
            fireConfigurationChanged();
        }
    }

    public static CheckStyleConfiguration getInstance(@NotNull final Project project) {
        CheckStyleConfiguration result = testInstance;
        if (result == null) {
            result = ServiceManager.getService(project, CheckStyleConfiguration.class);
        }
        return result;
    }

    public static void activateMock4UnitTesting(@Nullable final CheckStyleConfiguration testingInstance) {
        CheckStyleConfiguration.testInstance = testingInstance;
    }
}
