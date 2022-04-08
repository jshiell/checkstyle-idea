package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckStyleModuleConfiguration;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public class ConfigurationLocationSource {

    private final Project project;

    public ConfigurationLocationSource(@NotNull final Project project) {
        this.project = project;
    }

    public SortedSet<ConfigurationLocation> getConfigurationLocations(@Nullable final Module module,
                                                                      @Nullable final ConfigurationLocation override) {
        if (override != null) {
            return new TreeSet<>(Collections.singleton(override));
        }

        if (module != null) {
            CheckStyleModuleConfiguration moduleConfiguration = checkstyleModuleConfiguration(module);
            if (moduleConfiguration.isExcluded()) {
                return Collections.emptySortedSet();
            }
            return moduleConfiguration.getActiveConfigurations();
        }
        return configurationManager().getCurrent().getActiveLocations();
    }

    private PluginConfigurationManager configurationManager() {
        return ServiceManager.getService(project, PluginConfigurationManager.class);
    }

    private CheckStyleModuleConfiguration checkstyleModuleConfiguration(final Module module) {
        return module.getService(CheckStyleModuleConfiguration.class);
    }

}
