package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A manager for CheckStyle module configuration.
 */
@State(
        name = ModuleConfigurationState.ID_MODULE_PLUGIN,
        storages = {@Storage(StoragePathMacros.MODULE_FILE)}
)
public final class ModuleConfigurationState
        implements PersistentStateComponent<ModuleConfigurationState.ModuleSettings> {

    private static final Logger LOG = Logger.getInstance(ModuleConfigurationState.class);

    public static final String ID_MODULE_PLUGIN = "CheckStyle-IDEA-Module";

    private final Module module;

    private SortedSet<String> activeLocationIds;
    private boolean excludedFromScan;

    public ModuleConfigurationState(@NotNull final Module module) {
        this.module = module;
    }

    public void setActiveLocationIds(@NotNull final SortedSet<String> activeLocationIds) {
        this.activeLocationIds = activeLocationIds;
    }

    @NotNull
    public SortedSet<String> getActiveLocationIds() {
        return activeLocationIds;
    }


    public void setExcluded(final boolean excluded) {
        this.excludedFromScan = excluded;
    }

    public boolean isExcluded() {
        return excludedFromScan;
    }

    public boolean isUsingModuleConfiguration() {
        return !getActiveLocationIds().isEmpty();
    }

    private PluginConfigurationManager configurationManager() {
        return ServiceManager.getService(module.getProject(), PluginConfigurationManager.class);
    }

    private List<ConfigurationLocation> configurationLocations() {
        return new ArrayList<>(configurationManager().getCurrent().getLocations());
    }

    @Override
    @NotNull
    public ModuleSettings getState() {
        final ModuleSettings settings = new ModuleSettings();
        settings.setActiveLocationIds(activeLocationIds, module.getProject(), configurationManager().getCurrent());
        settings.setExcluded(excludedFromScan);
        return settings;
    }

    @Override
    public void loadState(@NotNull final ModuleSettings moduleSettings) {
        this.activeLocationIds = moduleSettings.getActiveLocationIds(module.getProject(), configurationLocations());
        this.excludedFromScan = moduleSettings.isExcluded();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ModuleConfigurationState that = (ModuleConfigurationState) o;
        return excludedFromScan == that.excludedFromScan && Objects.equals(module, that.module)
                && Objects.equals(activeLocationIds, that.activeLocationIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(module, activeLocationIds, excludedFromScan);
    }

    static class ModuleSettings {

        private static final String ACTIVE_CONFIG = "active-configuration";
        private static final String ACTIVE_CONFIGS_PREFIX = ACTIVE_CONFIG + "-";
        private static final String EXCLUDE_FROM_SCAN = "exclude-from-scan";

        @MapAnnotation
        private Map<String, String> configuration = new HashMap<>();

        @NotNull
        static ModuleSettings create(final Map<String, String> configuration) {
            final ModuleSettings moduleSettings = new ModuleSettings();
            moduleSettings.configuration = Objects.requireNonNullElse(configuration, new HashMap<>());
            return moduleSettings;
        }

        @NotNull
        public Map<String, String> configuration() {
            return Objects.requireNonNullElse(configuration, Collections.emptyMap());
        }

        @NotNull
        SortedSet<String> getActiveLocationIds(@NotNull final Project project,
                                               @NotNull final List<ConfigurationLocation> locations) {
            SortedSet<String> activeLocations = new TreeSet<>();
            try {
                if (configuration.get(ACTIVE_CONFIG) != null) {
                    activeLocations.add(configuration.get(ACTIVE_CONFIG));
                }
                configuration.keySet().stream()
                        .filter(propertyName -> propertyName.startsWith(ACTIVE_CONFIGS_PREFIX))
                        .map(configuration::get)
                        .filter(Objects::nonNull)
                        .forEach(activeLocations::add);
            } catch (IllegalArgumentException e) {
                LOG.warn("Could not load active configurations", e);
            }

            if (activeLocations.isEmpty()) {
                LOG.info("Active module configuration is invalid, returning project configuration");
                return new TreeSet<>();
            }

            return activeLocations.stream()
                    .map(it -> Descriptor.parse(it, project))
                    .map(it -> it.findIn(locations, project))
                    .filter(Optional::isPresent)
                    .map(it -> it.get().getId())
                    .collect(Collectors.toCollection(TreeSet::new));
        }


        public void setActiveLocationIds(@NotNull final SortedSet<String> activeLocationIds,
                                         @NotNull final Project project,
                                         @NotNull final PluginConfiguration pluginConfiguration) {
            final List<String> listOfLocationsIds = new ArrayList<>(activeLocationIds);
            for (int i = 0; i < listOfLocationsIds.size(); i++) {
                String currentId = listOfLocationsIds.get(i);
                Optional<ConfigurationLocation> currentLocation = pluginConfiguration.getLocationById(currentId);
                if (currentLocation.isPresent()) {
                    configuration.put(ACTIVE_CONFIGS_PREFIX + i, Descriptor.of(currentLocation.get(), project).toString());
                }
            }
        }


        public void setExcluded(final boolean excluded) {
            if (excluded) {
                configuration.put(EXCLUDE_FROM_SCAN, "true");
            } else {
                configuration.remove(EXCLUDE_FROM_SCAN);
            }
        }

        public boolean isExcluded() {
            return configuration.containsKey(EXCLUDE_FROM_SCAN)
                    && "true".equalsIgnoreCase(configuration.getOrDefault(EXCLUDE_FROM_SCAN, "false"));
        }
    }
}
