package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public abstract class ProjectConfigurationStateDeserialiser {

    private final Project project;

    public ProjectConfigurationStateDeserialiser(@NotNull final Project project) {
        this.project = project;
    }

    public abstract PluginConfigurationBuilder deserialise(
            @NotNull PluginConfigurationBuilder builder,
            @NotNull Map<String, String> projectConfiguration);

    protected void ensureBundledConfigs(@NotNull final List<ConfigurationLocation> configurationLocations) {
        final ConfigurationLocation sunChecks = configurationLocationFactory().create(BundledConfig.SUN_CHECKS, project);
        final ConfigurationLocation googleChecks = configurationLocationFactory().create(BundledConfig.GOOGLE_CHECKS, project);
        if (!configurationLocations.contains(sunChecks)) {
            configurationLocations.add(sunChecks);
        }
        if (!configurationLocations.contains(googleChecks)) {
            configurationLocations.add(googleChecks);
        }
    }

    protected ConfigurationLocationFactory configurationLocationFactory() {
        return ServiceManager.getService(project, ConfigurationLocationFactory.class);
    }

    protected Project getProject() {
        return project;
    }

}
