package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ConfigurationLocationSource {

    private static final Logger LOG = Logger.getInstance(ConfigurationLocationSource.class);

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
    private SortedSet<ConfigurationLocation> activeLocationsOrDefault(SortedSet<ConfigurationLocation> projectActiveLocations) {
        if (!projectActiveLocations.isEmpty()) {
            return projectActiveLocations;
        }
        return globalActiveLocations();
    }

    /**
     * Resolves the active global (IDE-wide) rule locations from {@link ApplicationConfigurationState},
     * instantiating them in this project's context so path resolution and caching work correctly.
     * Returns an empty set when the global fallback is disabled or no global locations are active.
     */
    @NotNull
    private SortedSet<ConfigurationLocation> globalActiveLocations() {
        final ApplicationConfigurationState appState = ApplicationManager.getApplication()
                .getService(ApplicationConfigurationState.class);
        if (!appState.isUseGlobalRulesByDefault()) {
            return Collections.emptySortedSet();
        }
        final List<String> activeIds = appState.getActiveGlobalLocationIds();
        if (activeIds.isEmpty()) {
            return Collections.emptySortedSet();
        }
        final ConfigurationLocationFactory factory = project.getService(ConfigurationLocationFactory.class);
        return appState.getGlobalLocations().stream()
                .filter(dto -> activeIds.contains(dto.id))
                .map(dto -> deserializeGlobalLocation(factory, dto))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Nullable
    private ConfigurationLocation deserializeGlobalLocation(
            @NotNull final ConfigurationLocationFactory factory,
            @NotNull final ApplicationConfigurationState.GlobalConfigurationLocation dto) {
        try {
            final ConfigurationType type = ConfigurationType.parse(dto.type);
            if (type == null) {
                return null;
            }
            final ConfigurationLocation location = factory.create(
                    project, dto.id, type,
                    Objects.requireNonNullElse(dto.location, "").trim(),
                    dto.description, null);
            if (dto.properties != null) {
                location.setProperties(dto.properties);
            }
            return location;
        } catch (Exception e) {
            LOG.error("Failed to deserialize global configuration location: " + dto, e);
            return null;
        }
    }

    private PluginConfigurationManager configurationManager() {
        return project.getService(PluginConfigurationManager.class);
    }

    private ModuleConfigurationState checkstyleModuleConfiguration(final Module module) {
        return module.getService(ModuleConfigurationState.class);
    }

}
