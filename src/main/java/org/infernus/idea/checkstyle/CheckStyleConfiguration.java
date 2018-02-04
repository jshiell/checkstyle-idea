package org.infernus.idea.checkstyle;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.infernus.idea.checkstyle.config.PluginConfigDto;
import org.infernus.idea.checkstyle.config.PluginConfigDtoBuilder;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.util.Notifications;
import org.infernus.idea.checkstyle.util.OS;
import org.infernus.idea.checkstyle.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.infernus.idea.checkstyle.config.PluginConfigDtoBuilder.defaultConfiguration;


/**
 * A manager for the persistent CheckStyle plug-in configuration. Registered in {@code plugin.xml}.
 */
@State(name = CheckStylePlugin.ID_PLUGIN, storages = {@Storage("checkstyle-idea.xml")})
public class CheckStyleConfiguration
        implements ExportableComponent, PersistentStateComponent<CheckStyleConfiguration.ProjectSettings> {

    private static final Logger LOG = Logger.getInstance(CheckStyleConfiguration.class);

    public static final String PROJECT_DIR = "$PRJ_DIR$";
    public static final String LEGACY_PROJECT_DIR = "$PROJECT_DIR$";

    private static final String ACTIVE_CONFIG = "active-configuration";
    private static final String CHECKSTYLE_VERSION_SETTING = "checkstyle-version";
    private static final String CHECK_TEST_CLASSES = "check-test-classes";
    private static final String CHECK_NONJAVA_FILES = "check-nonjava-files";
    private static final String SCANSCOPE_SETTING = "scanscope";
    private static final String SUPPRESS_ERRORS = "suppress-errors";
    private static final String COPY_LIBS = "copy-libs";
    private static final String THIRDPARTY_CLASSPATH = "thirdparty-classpath";
    private static final String SCAN_BEFORE_CHECKIN = "scan-before-checkin";
    private static final String LAST_ACTIVE_PLUGIN_VERSION = "last-active-plugin-version";

    private static final String LOCATION_PREFIX = "location-";
    private static final String PROPERTIES_PREFIX = "property-";

    private final List<ConfigurationListener> configurationListeners = Collections.synchronizedList(new ArrayList<>());

    private final Project project;

    private PluginConfigDto currentPluginConfig = null;

    /**
     * mock instance which may be set and used by unit tests
     */
    private static CheckStyleConfiguration testInstance = null;


    public CheckStyleConfiguration(final Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project is required");
        }

        this.project = project;
    }


    public void addConfigurationListener(final ConfigurationListener configurationListener) {
        if (configurationListener != null) {
            configurationListeners.add(configurationListener);
        }
    }

    private void fireConfigurationChanged() {
        synchronized (configurationListeners) {
            for (ConfigurationListener configurationListener : configurationListeners) {
                configurationListener.configurationChanged();
            }
        }
    }

    @NotNull
    public File[] getExportFiles() {
        return new File[]{PathManager.getOptionsFile("checkstyle-idea_project_settings")};
    }

    @NotNull
    public String getPresentableName() {
        return CheckStylePlugin.ID_PLUGIN + " Project Settings";
    }


    public void disableActiveConfiguration() {
        setCurrent(PluginConfigDtoBuilder.from(getCurrent())
                .withActiveLocation(null)
                .build(), true);
    }


    @NotNull
    public PluginConfigDto getCurrent() {
        return currentPluginConfig != null ? currentPluginConfig : defaultConfiguration(project).build();
    }

    public void setCurrent(@NotNull final PluginConfigDto updatedConfiguration, final boolean fireEvents) {
        currentPluginConfig = updatedConfiguration;
        if (fireEvents) {
            fireConfigurationChanged();
        }
    }


    private List<ConfigurationLocation> readConfigurationLocations(@NotNull final Map<String, String> pLoadedMap) {
        List<ConfigurationLocation> result = pLoadedMap.entrySet().stream()
                .filter(this::propertyIsALocation)
                .map(e -> deserialiseLocation(pLoadedMap, e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        ensureBundledConfigs(result);  // useful for migration, or when config file edited manually
        return result;
    }

    private void ensureBundledConfigs(@NotNull final List<ConfigurationLocation> pConfigurationLocations) {
        final ConfigurationLocation sunChecks = configurationLocationFactory().create(BundledConfig.SUN_CHECKS, project);
        final ConfigurationLocation googleChecks = configurationLocationFactory().create(BundledConfig.GOOGLE_CHECKS, project);
        if (!pConfigurationLocations.contains(sunChecks)) {
            pConfigurationLocations.add(sunChecks);
        }
        if (!pConfigurationLocations.contains(googleChecks)) {
            pConfigurationLocations.add(googleChecks);
        }
    }

    @Nullable
    private ConfigurationLocation deserialiseLocation(@NotNull final Map<String, String> pLoadedMap,
                                                      @NotNull final String pKey) {
        final String serialisedLocation = pLoadedMap.get(pKey);
        try {
            final ConfigurationLocation location = configurationLocationFactory().create(project, serialisedLocation);
            location.setProperties(propertiesFor(pLoadedMap, pKey));
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
    private Map<String, String> propertiesFor(@NotNull final Map<String, String> pLoadedMap,
                                              @NotNull final String pKey) {
        final Map<String, String> properties = new HashMap<>();

        final String propertyPrefix = propertyPrefix(pKey);

        // loop again over all settings to find the properties belonging to this configuration
        // not the best solution, but since there are only few items it doesn't hurt too much...
        pLoadedMap.entrySet().stream()
                .filter(property -> property.getKey().startsWith(propertyPrefix))
                .forEach(property -> {
                    final String propertyName = property.getKey().substring(propertyPrefix.length());
                    properties.put(propertyName, property.getValue());
                });
        return properties;
    }

    @NotNull
    private String propertyPrefix(final String key) {
        final int index = Integer.parseInt(key.substring(LOCATION_PREFIX.length()));
        return PROPERTIES_PREFIX + index + ".";
    }

    ConfigurationLocationFactory configurationLocationFactory() {
        return ServiceManager.getService(project, ConfigurationLocationFactory.class);
    }


    private List<String> readThirdPartyClassPath(@NotNull final Map<String, String> pLoadedMap) {
        final List<String> thirdPartyClasspath = new ArrayList<>();

        final String value = pLoadedMap.get(THIRDPARTY_CLASSPATH);
        if (value != null) {
            final String[] parts = value.split(";");
            for (final String part : parts) {
                if (part.length() > 0) {
                    thirdPartyClasspath.add(untokenisePath(part));
                }
            }
        }
        return thirdPartyClasspath;
    }

    private boolean booleanValueOf(@NotNull final Map<String, String> loadedMap, final String propertyName) {
        return Boolean.parseBoolean(loadedMap.get(propertyName));
    }

    private boolean booleanValueOfWithDefault(@NotNull final Map<String, String> loadedMap, final String propertyName,
                                              final boolean defaultValue) {
        final String v = loadedMap.get(propertyName);
        return v != null ? Boolean.parseBoolean(v) : defaultValue;
    }

    @NotNull
    private ScanScope scopeValueOf(@NotNull final Map<String, String> loadedMap) {
        final String propertyValue = loadedMap.get(SCANSCOPE_SETTING);
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

    /**
     * Process a stored file path for any tokens.
     *
     * @param path the path to process.
     * @return the processed path.
     */
    private String untokenisePath(final String path) {
        if (path == null) {
            return null;
        }

        LOG.debug("Processing file: " + path);

        for (String prefix : new String[]{PROJECT_DIR, LEGACY_PROJECT_DIR}) {
            if (path.startsWith(prefix)) {
                return untokeniseForPrefix(path, prefix, getProjectPath(project));
            }
        }

        return path;
    }

    private String untokeniseForPrefix(final String path, final String prefix, final File projectPath) {
        if (projectPath != null) {
            final File fullConfigFile = new File(projectPath, path.substring(prefix.length()));
            return fullConfigFile.getAbsolutePath();
        }

        LOG.warn("Could not untokenise path as project dir is unset: " + path);
        return path;
    }


    /**
     * Get the base path of the project.
     *
     * @return the base path of the project.
     */
    @Nullable
    private static File getProjectPath(@NotNull final Project pProject) {
        final VirtualFile baseDir = pProject.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        return new File(baseDir.getPath());
    }


    /**
     * Create a copy of the current configuration.
     *
     * @return a copy of the current configuration settings
     */
    public ProjectSettings getState() {
        if (currentPluginConfig != null) {
            return new ProjectSettings(project, currentPluginConfig);
        }
        return new ProjectSettings(project, defaultConfiguration(project).build());
    }

    /**
     * Load the state from the given settings beans.
     *
     * @param projectSettings the project settings to load.
     */
    public void loadState(final ProjectSettings projectSettings) {
        if (projectSettings != null) {
            Map<String, String> loadedMap = projectSettings.getConfiguration();
            convertSettingsFormat(loadedMap);
            currentPluginConfig = toDto(loadedMap);
        }
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

    private PluginConfigDto toDto(final Map<String, String> deserialisedMap) {
        return PluginConfigDtoBuilder.defaultConfiguration(project)
                .withCheckstyleVersion(readCheckstyleVersion(deserialisedMap))
                .withScanScope(scopeValueOf(deserialisedMap))
                .withSuppressErrors(booleanValueOf(deserialisedMap, SUPPRESS_ERRORS))
                .withCopyLibraries(booleanValueOfWithDefault(deserialisedMap, COPY_LIBS, OS.isWindows()))
                .withLocations(new TreeSet<>(readConfigurationLocations(deserialisedMap)))
                .withThirdPartyClassPath(readThirdPartyClassPath(deserialisedMap))
                .withActiveLocation(readActiveLocation(deserialisedMap, new TreeSet<>(readConfigurationLocations(deserialisedMap))))
                .withScanBeforeCheckin(booleanValueOf(deserialisedMap, SCAN_BEFORE_CHECKIN))
                .withLastActivePluginVersion(deserialisedMap.get(LAST_ACTIVE_PLUGIN_VERSION))
                .build();
    }

    private ConfigurationLocation readActiveLocation(@NotNull final Map<String, String> pLoadedMap,
                                                     @NotNull final SortedSet<ConfigurationLocation> pLocations) {
        String serializedLocation = pLoadedMap.get(ACTIVE_CONFIG);
        if (serializedLocation != null && !serializedLocation.trim().isEmpty()) {
            ConfigurationLocation activeLocation = configurationLocationFactory().create(project, serializedLocation);
            return pLocations.stream().filter(cl -> cl.equals(activeLocation)).findFirst().orElse(null);
        }
        return null;
    }

    @NotNull
    private String readCheckstyleVersion(@NotNull final Map<String, String> pLoadedMap) {
        final VersionListReader vlr = new VersionListReader();
        String result = pLoadedMap.get(CHECKSTYLE_VERSION_SETTING);
        if (result == null) {
            result = vlr.getDefaultVersion();
        } else {
            result = vlr.getReplacementMap().getOrDefault(result, result);
        }
        return result;
    }


    /**
     * Wrapper class for IDEA state serialisation.
     */
    static class ProjectSettings {
        private Map<String, String> configuration;

        /**
         * No-args constructor for deserialization.
         */
        public ProjectSettings() {
            super();
        }

        public ProjectSettings(@NotNull final Project project,
                               @NotNull final PluginConfigDto currentPluginConfig) {
            final Map<String, String> mapForSerialization = new TreeMap<>();
            mapForSerialization.put(CHECKSTYLE_VERSION_SETTING, currentPluginConfig.getCheckstyleVersion());
            mapForSerialization.put(SCANSCOPE_SETTING, currentPluginConfig.getScanScope().name());
            mapForSerialization.put(SUPPRESS_ERRORS, String.valueOf(currentPluginConfig.isSuppressErrors()));
            mapForSerialization.put(COPY_LIBS, String.valueOf(currentPluginConfig.isCopyLibs()));

            serializeLocations(project, mapForSerialization, new ArrayList<>(currentPluginConfig.getLocations()));

            String s = serializeThirdPartyClasspath(project, currentPluginConfig.getThirdPartyClasspath());
            if (!Strings.isBlank(s)) {
                mapForSerialization.put(THIRDPARTY_CLASSPATH, s);
            }

            mapForSerialization.put(SCAN_BEFORE_CHECKIN, String.valueOf(currentPluginConfig.isScanBeforeCheckin()));
            if (currentPluginConfig.getActiveLocation() != null) {
                mapForSerialization.put(ACTIVE_CONFIG, currentPluginConfig.getActiveLocation().getDescriptor());
            }

            mapForSerialization.put(LAST_ACTIVE_PLUGIN_VERSION, currentPluginConfig.getLastActivePluginVersion());

            configuration = mapForSerialization;
        }

        public void setConfiguration(final Map<String, String> deserialisedConfiguration) {
            configuration = deserialisedConfiguration;
        }

        @NotNull
        public Map<String, String> getConfiguration() {
            if (configuration == null) {
                return new TreeMap<>();
            }
            return configuration;
        }

        private void serializeLocations(@NotNull final Project project,
                                        @NotNull final Map<String, String> storage,
                                        @NotNull final List<ConfigurationLocation> configurationLocations) {
            int index = 0;
            for (final ConfigurationLocation configurationLocation : configurationLocations) {
                storage.put(LOCATION_PREFIX + index, configurationLocation.getDescriptor());
                try {
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
                } catch (IOException e) {
                    LOG.warn("Failed to read properties from " + configurationLocation, e);
                    Notifications.showError(project, CheckStyleBundle.message("checkstyle" + ""
                            + ".could-not-read-properties", configurationLocation.getLocation()));
                }

                ++index;
            }
        }

        private String serializeThirdPartyClasspath(@NotNull final Project project,
                                                    @NotNull final List<String> thirdPartyClasspath) {
            final StringBuilder sb = new StringBuilder();
            for (final String part : thirdPartyClasspath) {
                if (sb.length() > 0) {
                    sb.append(";");
                }
                sb.append(tokenisePath(project, part));
            }
            return sb.toString();
        }

        /**
         * Process a path and add tokens as necessary.
         *
         * @param path the path to processed.
         * @return the tokenised path.
         */
        private String tokenisePath(@NotNull final Project project, final String path) {
            if (path == null) {
                return null;
            }

            final File projectPath = getProjectPath(project);
            if (projectPath != null) {
                final String projectPathAbs = projectPath.getAbsolutePath();
                if (path.startsWith(projectPathAbs)) {
                    return PROJECT_DIR + path.substring(projectPathAbs.length());
                }
            }

            return path;
        }
    }


    public static CheckStyleConfiguration getInstance(@NotNull final Project project) {
        CheckStyleConfiguration result = testInstance;
        if (result == null) {
            result = ServiceManager.getService(project, CheckStyleConfiguration.class);
        }
        return result;
    }


    public static void activateMock4UnitTesting(@Nullable final CheckStyleConfiguration testingInstance) {
        CheckStyleConfiguration.testInstance = testingInstance;
    }
}
