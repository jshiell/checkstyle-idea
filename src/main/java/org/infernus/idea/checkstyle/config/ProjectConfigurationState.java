package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.ControlFlowException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.util.xmlb.annotations.*;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElseGet;
import static org.infernus.idea.checkstyle.config.PluginConfigurationBuilder.defaultConfiguration;

@State(name = CheckStylePlugin.ID_PLUGIN, storages = {@Storage("checkstyle-idea.xml")})
public class ProjectConfigurationState implements PersistentStateComponent<ProjectConfigurationState.ProjectSettings> {

    private static final Logger LOG = Logger.getInstance(ProjectConfigurationState.class);
    static final String ACTIVE_CONFIG = "active-configuration";
    static final String ACTIVE_CONFIGS_PREFIX = ACTIVE_CONFIG + "-";
    static final String CHECKSTYLE_VERSION_SETTING = "checkstyle-version";
    static final String SCANSCOPE_SETTING = "scanscope";
    static final String SUPPRESS_ERRORS = "suppress-errors";
    static final String COPY_LIBS = "copy-libs";
    static final String THIRDPARTY_CLASSPATH = "thirdparty-classpath";
    static final String SCAN_BEFORE_CHECKIN = "scan-before-checkin";
    static final String LOCATION_PREFIX = "location-";
    static final String PROPERTIES_PREFIX = "property-";

    private final Project project;

    private ProjectSettings projectSettings;

    public ProjectConfigurationState(@NotNull final Project project) {
        this.project = project;

        projectSettings = defaultProjectSettings();
    }

    private ProjectSettings defaultProjectSettings() {
        return ProjectSettings.create(defaultConfiguration(project).build());
    }

    public ProjectSettings getState() {
        return projectSettings;
    }

    public void loadState(@NotNull final ProjectSettings sourceProjectSettings) {
        projectSettings = sourceProjectSettings;
    }

    @NotNull
    PluginConfigurationBuilder populate(@NotNull final PluginConfigurationBuilder builder) {
        return projectSettings.populate(builder, project);
    }

    void setCurrentConfig(@NotNull final PluginConfiguration currentPluginConfig) {
        projectSettings = ProjectSettings.create(currentPluginConfig);
    }

    static class ProjectSettings {

        @Attribute
        private String serialisationVersion;

        @Tag
        private String checkstyleVersion;
        @Tag
        private String scanScope;
        @Tag
        private boolean suppressErrors;
        @Tag
        private boolean copyLibs;
        @Tag
        private boolean scanBeforeCheckin;
        @XCollection
        private List<String> thirdPartyClasspath;
        @XCollection
        private List<String> activeLocationIds;
        @MapAnnotation
        private List<ConfigurationLocation> locations;

        @MapAnnotation
        private Map<String, String> configuration;

        static ProjectSettings create(@NotNull final PluginConfiguration currentPluginConfig) {
            final ProjectSettings projectSettings = new ProjectSettings();

            projectSettings.serialisationVersion = "2";

            projectSettings.checkstyleVersion = currentPluginConfig.getCheckstyleVersion();
            projectSettings.scanScope = currentPluginConfig.getScanScope().name();
            projectSettings.suppressErrors = currentPluginConfig.isSuppressErrors();
            projectSettings.copyLibs = currentPluginConfig.isCopyLibs();
            projectSettings.scanBeforeCheckin = currentPluginConfig.isScanBeforeCheckin();

            projectSettings.thirdPartyClasspath = new ArrayList<>(currentPluginConfig.getThirdPartyClasspath());
            projectSettings.activeLocationIds = new ArrayList<>(currentPluginConfig.getActiveLocationIds());

            projectSettings.locations = currentPluginConfig.getLocations().stream()
                    .map(location -> new ConfigurationLocation(
                            location.getId(),
                            location.getType().name(),
                            location.getRawLocation(),
                            location.getDescription(),
                            location.getNamedScope().map(NamedScope::getScopeId).orElse(""),
                            location.getProperties()
                    ))
                    .collect(Collectors.toList());

            return projectSettings;
        }

        @SuppressWarnings("unused")
            // for serialisation
        ProjectSettings() {
        }

        ProjectSettings(@NotNull final Map<String, String> legacySerialisedFormat) {
            this.configuration = legacySerialisedFormat;
        }

        @NotNull
        Map<String, String> legacyConfiguration() {
            return requireNonNullElseGet(configuration, TreeMap::new);
        }

        PluginConfigurationBuilder populate(@NotNull final PluginConfigurationBuilder builder,
                                            @NotNull final Project project) {
            if (Objects.equals(serialisationVersion, "2")) {
                return builder
                        .withCheckstyleVersion(checkstyleVersion)
                        .withScanScope(lookupScanScope())
                        .withSuppressErrors(suppressErrors)
                        .withCopyLibraries(copyLibs)
                        .withScanBeforeCheckin(scanBeforeCheckin)
                        .withThirdPartyClassPath(requireNonNullElseGet(thirdPartyClasspath, ArrayList::new))
                        .withLocations(deserialiseLocations(project))
                        .withActiveLocationIds(new TreeSet<>(requireNonNullElseGet(activeLocationIds, ArrayList::new)));
            }

            return new LegacyProjectConfigurationStateDeserialiser(project)
                    .deserialise(builder, this);
        }

        @NotNull
        private TreeSet<org.infernus.idea.checkstyle.model.ConfigurationLocation> deserialiseLocations(@NotNull final Project project) {
            TreeSet<org.infernus.idea.checkstyle.model.ConfigurationLocation> configurationLocations = requireNonNullElseGet(this.locations, () -> new ArrayList<ConfigurationLocation>()).stream()
                    .map(location -> deserialiseLocation(project, location))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(TreeSet::new));
            List.of(BundledConfig.values()).forEach(bundleConfig -> {
                if (configurationLocations.stream().noneMatch(bundleConfig::matches)) {
                    configurationLocations.add(configurationLocationFactory(project).create(bundleConfig, project));
                }
            });
            return configurationLocations;
        }

        @NotNull
        private ScanScope lookupScanScope() {
            if (scanScope != null) {
                try {
                    return ScanScope.valueOf(scanScope);
                } catch (IllegalArgumentException e) {
                    // settings got messed up (manual edit?) - use default
                }
            }
            return ScanScope.getDefaultValue();
        }

        private ConfigurationLocationFactory configurationLocationFactory(@NotNull final Project project) {
            return project.getService(ConfigurationLocationFactory.class);
        }

        @Nullable
        private org.infernus.idea.checkstyle.model.ConfigurationLocation deserialiseLocation(@NotNull final Project project,
                                                                                             @Nullable final ProjectConfigurationState.ConfigurationLocation location) {
            if (location == null) {
                return null;
            }

            try {
                org.infernus.idea.checkstyle.model.ConfigurationLocation configurationLocation = configurationLocationFactory(project).create(
                        project,
                        location.id,
                        ConfigurationType.parse(location.type),
                        Objects.requireNonNullElse(location.location, "").trim(),
                        location.description,
                        NamedScopeHelper.getScopeByIdWithDefaultFallback(project, location.scope));
                configurationLocation.setProperties(Objects.requireNonNullElse(location.properties, new HashMap<>()));
                return configurationLocation;
            } catch (Exception e) {
                if (e instanceof ControlFlowException) {
                    throw e;
                }
                LOG.error("Failed to deserialise " + location, e);
                return null;
            }
        }
    }

    static class ConfigurationLocation {

        @Attribute
        private String id;
        @Attribute
        private String type;
        @Attribute
        private String scope;
        @Attribute
        private String description;
        @Text
        private String location;
        @MapAnnotation
        private Map<String, String> properties;

        @SuppressWarnings("unused")
            // serialisation
        ConfigurationLocation() {
        }

        ConfigurationLocation(final String id,
                              final String type,
                              final String location,
                              final String description,
                              final String scope,
                              final Map<String, String> properties) {
            this.id = id;
            this.type = type;
            this.scope = scope;
            this.description = description;
            this.location = location;
            if (properties != null && !properties.isEmpty()) {
                this.properties = properties;
            } else {
                this.properties = null;
            }
        }

        @Override
        public String toString() {
            return "ConfigurationLocation{"
                    + "id='" + id + '\''
                    + ", type='" + type + '\''
                    + ", scope='" + scope + '\''
                    + ", description='" + description + '\''
                    + ", location='" + location + '\''
                    + ", properties='" + properties + '\''
                    + '}';
        }
    }

}
