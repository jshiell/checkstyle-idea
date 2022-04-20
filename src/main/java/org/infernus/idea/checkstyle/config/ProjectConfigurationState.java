package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.VersionListReader;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.util.OS;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.infernus.idea.checkstyle.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.infernus.idea.checkstyle.config.PluginConfigurationBuilder.defaultConfiguration;

@State(name = CheckStylePlugin.ID_PLUGIN, storages = {@Storage("checkstyle-idea.xml")})
public class ProjectConfigurationState implements PersistentStateComponent<ProjectConfigurationState.ProjectSettings> {

    private static final Logger LOG = Logger.getInstance(ProjectConfigurationState.class);

    private static final String ACTIVE_CONFIG = "active-configuration";
    private static final String ACTIVE_CONFIGS_PREFIX = ACTIVE_CONFIG + "-";
    private static final String CHECKSTYLE_VERSION_SETTING = "checkstyle-version";
    private static final String CHECK_TEST_CLASSES = "check-test-classes";
    private static final String CHECK_NONJAVA_FILES = "check-nonjava-files";
    private static final String SCANSCOPE_SETTING = "scanscope";
    private static final String SUPPRESS_ERRORS = "suppress-errors";
    private static final String COPY_LIBS = "copy-libs";
    private static final String THIRDPARTY_CLASSPATH = "thirdparty-classpath";
    private static final String SCAN_BEFORE_CHECKIN = "scan-before-checkin";
    private static final String LOCATION_PREFIX = "location-";
    private static final String PROPERTIES_PREFIX = "property-";

    private final Project project;

    private ProjectSettings projectSettings;

    public ProjectConfigurationState(@NotNull final Project project) {
        this.project = project;

        projectSettings = defaultProjectSettings();
    }

    private ConfigurationLocationFactory configurationLocationFactory() {
        return ServiceManager.getService(project, ConfigurationLocationFactory.class);
    }

    private ProjectFilePaths projectFilePaths() {
        return ServiceManager.getService(project, ProjectFilePaths.class);
    }

    private ProjectSettings defaultProjectSettings() {
        return ProjectSettings.create(projectFilePaths(), defaultConfiguration(project).build());
    }

    public ProjectSettings getState() {
        return projectSettings;
    }

    public void loadState(@NotNull final ProjectSettings sourceProjectSettings) {
        projectSettings = sourceProjectSettings;
    }

    @NotNull
    PluginConfigurationBuilder populate(@NotNull final PluginConfigurationBuilder builder) {
        Map<String, String> projectConfiguration = projectSettings.configuration();
        convertSettingsFormat(projectConfiguration);
        final TreeSet<ConfigurationLocation> deserialisedLocations = new TreeSet<>(readConfigurationLocations(projectConfiguration));
        return builder
                .withCheckstyleVersion(readCheckstyleVersion(projectConfiguration))
                .withScanScope(scopeValueOf(projectConfiguration))
                .withSuppressErrors(booleanValueOf(projectConfiguration, SUPPRESS_ERRORS))
                .withCopyLibraries(booleanValueOfWithDefault(projectConfiguration, COPY_LIBS, OS.isWindows()))
                .withLocations(deserialisedLocations)
                .withThirdPartyClassPath(readThirdPartyClassPath(projectConfiguration))
                .withActiveLocationDescriptor(readActiveLocations(projectConfiguration))
                .withScanBeforeCheckin(booleanValueOf(projectConfiguration, SCAN_BEFORE_CHECKIN));
    }

    void setCurrentConfig(@NotNull final PluginConfiguration currentPluginConfig) {
        projectSettings = ProjectSettings.create(projectFilePaths(), currentPluginConfig);
    }

    /**
     * Needed when a setting written by a previous version of this plugin gets loaded by a newer version; converts
     * the scan scope settings based on flags to the enum value.
     *
     * @param deserialisedMap the loaded settings
     */
    private void convertSettingsFormat(final Map<String, String> deserialisedMap) {
        if (deserialisedMap != null && !deserialisedMap.isEmpty() && !deserialisedMap.containsKey(SCANSCOPE_SETTING)) {
            ScanScope scope = ScanScope.fromFlags(booleanValueOf(deserialisedMap, CHECK_TEST_CLASSES),
                    booleanValueOf(deserialisedMap, CHECK_NONJAVA_FILES));
            deserialisedMap.put(SCANSCOPE_SETTING, scope.name());
            deserialisedMap.remove(CHECK_TEST_CLASSES);
            deserialisedMap.remove(CHECK_NONJAVA_FILES);
        }
    }

    private SortedSet<String> readActiveLocations(@NotNull final Map<String, String> configuration) {
        String serializedSingleLocation = configuration.get(ACTIVE_CONFIG);
        if (serializedSingleLocation != null && !serializedSingleLocation.trim().isEmpty()) {
            // For backwards compatibility if only one location is used.
            final SortedSet<String> locations = new TreeSet<>();
            locations.add(serializedSingleLocation);
            return locations;
        }

        return readActiveConfigurationLocationsDescriptors(configuration);
    }

    @NotNull
    private String readCheckstyleVersion(@NotNull final Map<String, String> configuration) {
        final VersionListReader vlr = new VersionListReader();
        String result = configuration.get(CHECKSTYLE_VERSION_SETTING);
        if (result == null) {
            result = vlr.getDefaultVersion();
        } else {
            result = vlr.getReplacementMap().getOrDefault(result, result);
        }
        return result;
    }


    private List<String> readThirdPartyClassPath(@NotNull final Map<String, String> configuration) {
        final List<String> thirdPartyClasspath = new ArrayList<>();

        final String value = configuration.get(THIRDPARTY_CLASSPATH);
        if (value != null) {
            final String[] parts = value.split(";");
            for (final String part : parts) {
                if (part.length() > 0) {
                    thirdPartyClasspath.add(projectFilePaths().detokenise(part));
                }
            }
        }
        return thirdPartyClasspath;
    }

    private boolean booleanValueOf(@NotNull final Map<String, String> loadedMap, final String propertyName) {
        return Boolean.parseBoolean(loadedMap.get(propertyName));
    }

    private boolean booleanValueOfWithDefault(@NotNull final Map<String, String> configuration,
                                              final String propertyName,
                                              final boolean defaultValue) {
        final String v = configuration.get(propertyName);
        return v != null ? Boolean.parseBoolean(v) : defaultValue;
    }

    @NotNull
    private ScanScope scopeValueOf(@NotNull final Map<String, String> configuration) {
        final String propertyValue = configuration.get(SCANSCOPE_SETTING);
        ScanScope result = ScanScope.getDefaultValue();
        if (propertyValue != null) {
            try {
                result = ScanScope.valueOf(propertyValue);
            } catch (IllegalArgumentException e) {
                // settings got messed up (manual edit?) - use default
            }
        }
        return result;
    }

    private SortedSet<String> readActiveConfigurationLocationsDescriptors(@NotNull final Map<String, String> configuration) {
        return configuration.keySet().stream()
                .filter(propertyName -> propertyName.startsWith(ACTIVE_CONFIGS_PREFIX))
                .map(configuration::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private List<ConfigurationLocation> readConfigurationLocations(@NotNull final Map<String, String> configuration) {
        List<ConfigurationLocation> result = configuration.entrySet().stream()
                .filter(this::propertyIsALocation)
                .map(e -> deserialiseLocation(configuration, e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        ensureBundledConfigs(result);  // useful for migration, or when config file edited manually
        return result;
    }

    private void ensureBundledConfigs(@NotNull final List<ConfigurationLocation> configurationLocations) {
        final ConfigurationLocation sunChecks = configurationLocationFactory().create(BundledConfig.SUN_CHECKS, project);
        final ConfigurationLocation googleChecks = configurationLocationFactory().create(BundledConfig.GOOGLE_CHECKS, project);
        if (!configurationLocations.contains(sunChecks)) {
            configurationLocations.add(sunChecks);
        }
        if (!configurationLocations.contains(googleChecks)) {
            configurationLocations.add(googleChecks);
        }
    }

    @Nullable
    private ConfigurationLocation deserialiseLocation(@NotNull final Map<String, String> configuration,
                                                      @NotNull final String key) {
        final String serialisedLocation = configuration.get(key);
        try {
            final ConfigurationLocation location = configurationLocationFactory().create(project, serialisedLocation);
            location.setProperties(propertiesFor(configuration, key));
            return location;

        } catch (IllegalArgumentException e) {
            LOG.warn("Could not parse location: " + serialisedLocation, e);
            return null;
        }
    }

    private boolean propertyIsALocation(final Map.Entry<String, String> property) {
        return property.getKey().startsWith(LOCATION_PREFIX);
    }

    @NotNull
    private Map<String, String> propertiesFor(@NotNull final Map<String, String> configuration,
                                              @NotNull final String pKey) {
        final Map<String, String> properties = new HashMap<>();

        final String propertyPrefix = propertyPrefix(pKey);

        // loop again over all settings to find the properties belonging to this configuration
        // not the best solution, but since there are only few items it doesn't hurt too much...
        configuration.entrySet().stream()
                .filter(property -> property.getKey().startsWith(propertyPrefix))
                .forEach(property -> {
                    final String propertyName = property.getKey().substring(propertyPrefix.length());
                    properties.put(propertyName, property.getValue());
                });
        return properties;
    }

    @NotNull
    private String propertyPrefix(final String key) {
        final int index = Integer.parseInt(key.startsWith(LOCATION_PREFIX)
                ? key.substring(LOCATION_PREFIX.length())
                : key.substring(ACTIVE_CONFIGS_PREFIX.length()));
        return PROPERTIES_PREFIX + index + ".";
    }

    static class ProjectSettings {
        @MapAnnotation
        private Map<String, String> configuration;

        static ProjectSettings create(@NotNull final ProjectFilePaths projectFilePaths,
                                      @NotNull final PluginConfiguration currentPluginConfig) {
            final Map<String, String> mapForSerialization = new TreeMap<>();
            mapForSerialization.put(CHECKSTYLE_VERSION_SETTING, currentPluginConfig.getCheckstyleVersion());
            mapForSerialization.put(SCANSCOPE_SETTING, currentPluginConfig.getScanScope().name());
            mapForSerialization.put(SUPPRESS_ERRORS, String.valueOf(currentPluginConfig.isSuppressErrors()));
            mapForSerialization.put(COPY_LIBS, String.valueOf(currentPluginConfig.isCopyLibs()));

            serializeLocations(mapForSerialization, new ArrayList<>(currentPluginConfig.getLocations()));

            String s = serializeThirdPartyClasspath(projectFilePaths, currentPluginConfig.getThirdPartyClasspath());
            if (!Strings.isBlank(s)) {
                mapForSerialization.put(THIRDPARTY_CLASSPATH, s);
            }

            mapForSerialization.put(SCAN_BEFORE_CHECKIN, String.valueOf(currentPluginConfig.isScanBeforeCheckin()));

            serializeActiveLocations(mapForSerialization, new ArrayList<>(currentPluginConfig.getActiveLocationDescriptors()));

            final ProjectSettings projectSettings = new ProjectSettings();
            projectSettings.configuration = mapForSerialization;
            return projectSettings;
        }

        private static void serializeActiveLocations(final Map<String, String> storage, final List<String> activeLocationDescriptors) {
            for (int i = 0; i < activeLocationDescriptors.size(); i++) {
                storage.put(ACTIVE_CONFIGS_PREFIX + i, activeLocationDescriptors.get(i));
            }
        }

        @NotNull
        public Map<String, String> configuration() {
            return Objects.requireNonNullElseGet(configuration, TreeMap::new);
        }

        private static void serializeLocations(@NotNull final Map<String, String> storage,
                                               @NotNull final List<ConfigurationLocation> configurationLocations) {
            int index = 0;
            for (final ConfigurationLocation configurationLocation : configurationLocations) {
                storage.put(LOCATION_PREFIX + index, configurationLocation.getDescriptor());
                final Map<String, String> properties = configurationLocation.getProperties();
                if (properties != null) {
                    for (final Map.Entry<String, String> prop : properties.entrySet()) {
                        String value = prop.getValue();
                        if (value == null) {
                            value = "";
                        }
                        storage.put(PROPERTIES_PREFIX + index + "." + prop.getKey(), value);
                    }
                }

                ++index;
            }
        }

        private static String serializeThirdPartyClasspath(@NotNull final ProjectFilePaths projectFilePaths,
                                                           @NotNull final List<String> thirdPartyClasspath) {
            final StringBuilder sb = new StringBuilder();
            for (final String part : thirdPartyClasspath) {
                if (sb.length() > 0) {
                    sb.append(";");
                }
                sb.append(projectFilePaths.tokenise(part));
            }
            return sb.toString();
        }
    }

}
