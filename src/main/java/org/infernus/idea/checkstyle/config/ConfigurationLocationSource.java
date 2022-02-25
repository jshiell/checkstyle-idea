package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckStyleModuleConfiguration;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ConfigurationLocationSource {

    private final Project project;

    public ConfigurationLocationSource(@NotNull final Project project) {
        this.project = project;
    }

    public Optional<ConfigurationLocation> getConfigurationLocation(@Nullable final Module module,
                                                                    @Nullable final ConfigurationLocation override) {
        if (override != null) {
            return Optional.of(override);
        }

        if (module != null) {
            CheckStyleModuleConfiguration moduleConfiguration = checkstyleModuleConfiguration(module);
            if (moduleConfiguration.isExcluded()) {
                return Optional.empty();
            }
            return moduleConfiguration.getActiveConfiguration();
        }
        return configurationManager().getCurrent().getActiveLocation();
    }

    private PluginConfigurationManager configurationManager() {
        return ServiceManager.getService(project, PluginConfigurationManager.class);
    }

    private CheckStyleModuleConfiguration checkstyleModuleConfiguration(final Module module) {
        return module.getService(CheckStyleModuleConfiguration.class);
    }

}
