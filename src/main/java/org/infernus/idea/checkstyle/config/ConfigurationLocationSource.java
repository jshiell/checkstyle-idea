package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ConfigurationLocationSource {

    private final Project project;

    public ConfigurationLocationSource(@NotNull final Project project) {
        this.project = project;
    }

    public SortedSet<ConfigurationLocation> getConfigurationLocations(@Nullable final Module module,
                                                                      @Nullable final ConfigurationLocation override) {
        if (module != null) {
            ModuleConfigurationState moduleConfiguration = checkstyleModuleConfiguration(module);
            if (moduleConfiguration.isExcluded()) {
                return Collections.emptySortedSet();
            }

            if (override != null) {
                return new TreeSet<>(Collections.singleton(override));
            }

            PluginConfiguration configuration = configurationManager().getCurrent();
            TreeSet<ConfigurationLocation> moduleActiveConfigurations = moduleConfiguration.getActiveLocationIds().stream()
                    .map(id -> configuration.getLocationById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(TreeSet::new));
            if (!moduleActiveConfigurations.isEmpty()) {
                return moduleActiveConfigurations;
            }
        } else if (override != null) {
            return new TreeSet<>(Collections.singleton(override));
        }
        final SortedSet<ConfigurationLocation> projectActiveLocations = configurationManager().getCurrent().getActiveLocations();
        return activeLocationsOrDefault(projectActiveLocations);
    }

    /**
     * If there are no project-level active locations — fall back to IDE-wide global rules (if configured)
     */
    @NotNull
    private SortedSet<ConfigurationLocation> activeLocationsOrDefault(final SortedSet<ConfigurationLocation> projectActiveLocations) {
        if (!projectActiveLocations.isEmpty()) {
            return projectActiveLocations;
        }
        return new GlobalConfigurationLocations(project).activeLocations();
    }

    private PluginConfigurationManager configurationManager() {
        return project.getService(PluginConfigurationManager.class);
    }

    private ModuleConfigurationState checkstyleModuleConfiguration(final Module module) {
        return module.getService(ModuleConfigurationState.class);
    }

}