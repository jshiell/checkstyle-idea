package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
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
        return Objects.requireNonNullElseGet(activeLocationIds, TreeSet::new);
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
        return module.getProject().getService(PluginConfigurationManager.class);
    }

    private List<ConfigurationLocation> configurationLocations() {
        return new ArrayList<>(configurationManager().getCurrent().getLocations());
    }

    @Override
    @NotNull
    public ModuleSettings getState() {
        final ModuleSettings settings = new ModuleSettings();
        settings.useLatestSerialisationFormat();
        settings.setActiveLocationIds(Objects.requireNonNullElseGet(activeLocationIds, TreeSet::new));
        settings.setExcludeFromScan(excludedFromScan);
        return settings;
    }

    @Override
    public void loadState(@NotNull final ModuleSettings moduleSettings) {
        this.activeLocationIds = moduleSettings.getActiveLocationIds(module.getProject(), configurationLocations());
        this.excludedFromScan = moduleSettings.isExcludeFromScan();
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

        @Attribute
        private String serialisationVersion;

        @XCollection
        private List<String> activeLocationsIds;
        @Tag
        private boolean excludeFromScan = false;

        // legacy configuration
        @MapAnnotation
        private Map<String, String> configuration = new HashMap<>();

        @NotNull
        static ModuleSettings create(final Map<String, String> configuration) {
            final ModuleSettings moduleSettings = new ModuleSettings();
            moduleSettings.configuration = Objects.requireNonNullElse(configuration, new HashMap<>());
            return moduleSettings;
        }

        public void setActiveLocationIds(@NotNull final SortedSet<String> newActiveLocationIds) {
            this.activeLocationsIds = new ArrayList<>(newActiveLocationIds);
        }

        public void setExcludeFromScan(final boolean excludeFromScan) {
            this.excludeFromScan = excludeFromScan;
        }

        @NotNull
        SortedSet<String> getActiveLocationIds(@NotNull final Project project,
                                               @NotNull final List<ConfigurationLocation> locations) {
            if (Objects.equals(serialisationVersion, "2")) {
                return new TreeSet<>(Objects.requireNonNullElse(activeLocationsIds, Collections.emptyList()));
            }

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

        public boolean isExcludeFromScan() {
            if (Objects.equals(serialisationVersion, "2")) {
                return excludeFromScan;
            }

            return configuration.containsKey(EXCLUDE_FROM_SCAN)
                    && "true".equalsIgnoreCase(configuration.getOrDefault(EXCLUDE_FROM_SCAN, "false"));
        }

        public void useLatestSerialisationFormat() {
            serialisationVersion = "2";
            configuration.clear();
        }
    }
}
