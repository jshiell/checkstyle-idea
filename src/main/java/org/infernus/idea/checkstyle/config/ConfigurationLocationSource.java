package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckStyleModuleConfiguration;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigurationLocationSource {

    private final Project project;

    public ConfigurationLocationSource(@NotNull final Project project) {
        this.project = project;
    }

    public ConfigurationLocation getConfigurationLocation(@Nullable final Module module,
                                                          @Nullable final ConfigurationLocation override) {
        if (override != null) {
            return override;
        }

        if (module != null) {
            CheckStyleModuleConfiguration moduleConfiguration = checkstyleModuleConfiguration(module);
            if (moduleConfiguration.isExcluded()) {
                return null;
            }
            return moduleConfiguration.getActiveConfiguration();
        }
        return configurationManager().getCurrent().getActiveLocation();
    }

    private PluginConfigurationManager configurationManager() {
        return ServiceManager.getService(project, PluginConfigurationManager.class);
    }

    private CheckStyleModuleConfiguration checkstyleModuleConfiguration(final Module module) {
        return ModuleServiceManager.getService(module, CheckStyleModuleConfiguration.class);
    }

}
