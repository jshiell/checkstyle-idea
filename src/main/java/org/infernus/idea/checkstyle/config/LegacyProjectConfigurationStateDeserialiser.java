package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.VersionListReader;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.util.OS;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static org.infernus.idea.checkstyle.config.ProjectConfigurationState.*;

public class LegacyProjectConfigurationStateDeserialiser {

    private static final Logger LOG = Logger.getInstance(LegacyProjectConfigurationStateDeserialiser.class);

    private static final String CHECK_TEST_CLASSES = "check-test-classes";
    private static final String CHECK_NONJAVA_FILES = "check-nonjava-files";

    private final Project project;

    public LegacyProjectConfigurationStateDeserialiser(@NotNull final Project project) {
        this.project = project;
    }

    public PluginConfigurationBuilder deserialise(@NotNull final PluginConfigurationBuilder builder,
                                                  @NotNull final ProjectSettings projectSettings) {
        final Map<String, String> projectConfiguration = projectSettings.legacyConfiguration();
        convertSettingsFormat(projectConfiguration);
        final TreeSet<ConfigurationLocation> locations = new TreeSet<>(readConfigurationLocations(projectConfiguration));
        return builder
                .withCheckstyleVersion(readCheckstyleVersion(projectConfiguration))
                .withScanScope(scopeValueOf(projectConfiguration))
                .withSuppressErrors(booleanValueOf(projectConfiguration, SUPPRESS_ERRORS))
                .withCopyLibraries(booleanValueOfWithDefault(projectConfiguration, COPY_LIBS, OS.isWindows()))
                .withLocations(locations)
                .withThirdPartyClassPath(readThirdPartyClassPath(projectConfiguration))
                .withActiveLocationIds(readActiveLocationIds(projectConfiguration, locations))
                .withScanBeforeCheckin(booleanValueOf(projectConfiguration, SCAN_BEFORE_CHECKIN));
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

    @NotNull
    private SortedSet<String> readActiveLocationIds(@NotNull final Map<String, String> configuration,
                                                    @NotNull final Collection<ConfigurationLocation> locations) {
        Object serializedSingleLocation = configuration.get(ACTIVE_CONFIG);

        final SortedSet<String> activeDescriptors;
        if (serializedSingleLocation != null && !serializedSingleLocation.toString().trim().isEmpty()) {
            // For backwards compatibility if only one location is used.
            activeDescriptors = new TreeSet<>();
            activeDescriptors.add(serializedSingleLocation.toString());
        } else {
            activeDescriptors = readActiveConfigurationLocationsDescriptors(configuration);
        }

        return activeDescriptors.stream()
                .map(it -> Descriptor.parse(it, getProject()))
                .map(it -> it.findIn(locations, getProject()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ConfigurationLocation::getId)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @NotNull
    private String readCheckstyleVersion(@NotNull final Map<String, String> configuration) {
        final VersionListReader vlr = new VersionListReader();
        Object result = configuration.get(CHECKSTYLE_VERSION_SETTING);
        if (result == null) {
            return vlr.getDefaultVersion();
        }
        return vlr.getReplacementMap().getOrDefault(result.toString(), result.toString());
    }

    @NotNull
    private List<String> readThirdPartyClassPath(@NotNull final Map<String, String> configuration) {
        final List<String> thirdPartyClasspath = new ArrayList<>();

        final Object value = configuration.get(THIRDPARTY_CLASSPATH);
        if (value != null) {
            final String[] parts = value.toString().split(";");
            for (final String part : parts) {
                if (part.length() > 0) {
                    thirdPartyClasspath.add(projectFilePaths().detokenise(part));
                }
            }
        }
        return thirdPartyClasspath;
    }

    @NotNull
    private ScanScope scopeValueOf(@NotNull final Map<String, String> configuration) {
        final Object propertyValue = configuration.get(SCANSCOPE_SETTING);
        ScanScope result = ScanScope.getDefaultValue();
        if (propertyValue != null) {
            try {
                result = ScanScope.valueOf(propertyValue.toString());
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
                .map(Object::toString)
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

    @Nullable
    private ConfigurationLocation deserialiseLocation(@NotNull final Map<String, String> configuration,
                                                      @NotNull final String key) {
        final Object serialisedLocation = configuration.get(key);
        try {
            ConfigurationLocation location = Descriptor.parse(serialisedLocation.toString(), getProject()).toConfigurationLocation(getProject());
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
                    if (property.getValue() != null) {
                        properties.put(propertyName, property.getValue());
                    } else {
                        properties.put(propertyName, null);
                    }
                });
        return properties;
    }

    @NotNull
    private String propertyPrefix(final String key) {
        final int index;
        if (key.startsWith(LOCATION_PREFIX)) {
            index = Integer.parseInt(key.substring(LOCATION_PREFIX.length()));
        } else {
            index = Integer.parseInt(key.substring(ACTIVE_CONFIGS_PREFIX.length()));
        }
        return PROPERTIES_PREFIX + index + ".";
    }

    private ProjectFilePaths projectFilePaths() {
        return getProject().getService(ProjectFilePaths.class);
    }


    private boolean booleanValueOf(@NotNull final Map<String, String> properties, final String propertyName) {
        return booleanValueOfWithDefault(properties, propertyName, false);
    }

    private boolean booleanValueOfWithDefault(@NotNull final Map<String, String> properties,
                                              final String propertyName,
                                              final boolean defaultValue) {
        final Object value = properties.get(propertyName);
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return defaultValue;
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

    private ConfigurationLocationFactory configurationLocationFactory() {
        return project.getService(ConfigurationLocationFactory.class);
    }

    private Project getProject() {
        return project;
    }

}
