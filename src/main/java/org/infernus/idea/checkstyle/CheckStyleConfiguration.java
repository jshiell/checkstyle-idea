package org.infernus.idea.checkstyle;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * A manager for CheckStyle plug-in configuration.
 */
@State(
        name = CheckStylePlugin.ID_PLUGIN,
        storages = {
                @Storage(id = "default", file = StoragePathMacros.PROJECT_FILE),
                @Storage(id = "dir", file = StoragePathMacros.PROJECT_CONFIG_DIR + "/checkstyle-idea.xml", scheme = StorageScheme.DIRECTORY_BASED)
        }
)
public final class CheckStyleConfiguration implements ExportableComponent,
        PersistentStateComponent<CheckStyleConfiguration.ProjectSettings> {

    public static final String PROJECT_DIR = "$PRJ_DIR$";
    public static final String LEGACY_PROJECT_DIR = "$PROJECT_DIR$";

    private static final Log LOG = LogFactory.getLog(CheckStyleConfiguration.class);

    private static final String ACTIVE_CONFIG = "active-configuration";
    private static final String CHECK_TEST_CLASSES = "check-test-classes";
    private static final String CHECK_NONJAVA_FILES = "check-nonjava-files";
    private static final String SCANSCOPE_SETTING = "scanscope";
    private static final String SUPPRESS_ERRORS = "suppress-errors";
    private static final String THIRDPARTY_CLASSPATH = "thirdparty-classpath";
    private static final String SCAN_BEFORE_CHECKIN = "scan-before-checkin";

    private static final String LOCATION_PREFIX = "location-";
    private static final String PROPERTIES_PREFIX = "property-";

    private static final String SUN_CHECKS_CONFIG = "/sun_checks.xml";

    private final Set<ConfigurationLocation> presetLocations = new HashSet<>();
    private final SortedMap<String, String> storage = new ConcurrentSkipListMap<>();
    private final ReentrantLock storageLock = new ReentrantLock();
    private final List<ConfigurationListener> configurationListeners = Collections.synchronizedList(new ArrayList<>());

    private final Project project;

    /**
     * Create a new configuration bean.
     *
     * @param project the project we belong to.
     */
    public CheckStyleConfiguration(final Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project is required");
        }

        this.project = project;

        final ConfigurationLocation checkStyleSunChecks = configurationLocationFactory().create(project, ConfigurationType.CLASSPATH,
                SUN_CHECKS_CONFIG, CheckStyleBundle.message("file.default.description"));
        presetLocations.add(checkStyleSunChecks);
    }

    public void addConfigurationListener(final ConfigurationListener configurationListener) {
        if (configurationListener != null) {
            configurationListeners.add(configurationListener);
        }
    }

    private void fireConfigurationChanged() {
        for (ConfigurationListener configurationListener : configurationListeners) {
            configurationListener.configurationChanged();
        }
    }

    @NotNull
    public File[] getExportFiles() {
        return new File[]{PathManager.getOptionsFile("checkstyle-idea_project_settings")};
    }

    @NotNull
    public String getPresentableName() {
        return "CheckStyle-IDEA Project Settings";
    }

    public Set<ConfigurationLocation> getPresetLocations() {
        return Collections.unmodifiableSet(presetLocations);
    }

    public void setActiveConfiguration(final ConfigurationLocation configurationLocation) {
        final List<ConfigurationLocation> configurationLocations = configurationLocations();
        if (configurationLocation != null && !configurationLocations.contains(configurationLocation)) {
            throw new IllegalArgumentException("Location is not valid: " + configurationLocation);
        }

        storageLock.lock();
        try {
            if (configurationLocation != null) {
                storage.put(ACTIVE_CONFIG, configurationLocation.getDescriptor());
            } else {
                storage.remove(ACTIVE_CONFIG);
            }
        } finally {
            storageLock.unlock();
        }
    }

    public ConfigurationLocation getActiveConfiguration() {
        storageLock.lock();
        try {
            if (!storage.containsKey(ACTIVE_CONFIG)) {
                return null;
            }

            ConfigurationLocation activeLocation = null;
            try {
                activeLocation = configurationLocationFactory().create(project, storage.get(ACTIVE_CONFIG));
            } catch (IllegalArgumentException e) {
                LOG.warn("Could not load active configuration", e);
            }

            final List<ConfigurationLocation> configurationLocations = configurationLocations();
            if (activeLocation == null || !configurationLocations.contains(activeLocation)) {
                LOG.info("Active configuration is invalid, returning null");
                return null;
            }

            setActiveConfiguration(activeLocation);

            return activeLocation;
        } finally {
            storageLock.unlock();
        }
    }

    public List<ConfigurationLocation> getAndResolveConfigurationLocations() {
        return setConfigurationLocations(configurationLocations(), false);
    }

    public List<ConfigurationLocation> configurationLocations() {
        storageLock.lock();
        try {
            final List<ConfigurationLocation> locations = storage.entrySet().stream()
                    .filter(this::propertyIsALocation)
                    .map(this::deserialiseLocation)
                    .filter(this::notNull)
                    .collect(Collectors.toList());

            return addPresetLocationsTo(locations);

        } finally {
            storageLock.unlock();
        }
    }

    private boolean notNull(Object object) {
        return object != null;
    }

    @Nullable
    private ConfigurationLocation deserialiseLocation(final Map.Entry<String, String> locationProperty) {
        final String serialisedLocation = locationProperty.getValue();
        try {
            final ConfigurationLocation location = configurationLocationFactory().create(project, serialisedLocation);
            location.setProperties(propertiesFor(locationProperty));
            return location;

        } catch (IllegalArgumentException e) {
            LOG.error("Could not parse location: " + serialisedLocation, e);
            return null;
        }
    }

    private boolean propertyIsALocation(final Map.Entry<String, String> property) {
        return property.getKey().startsWith(LOCATION_PREFIX);
    }

    private List<ConfigurationLocation> addPresetLocationsTo(final List<ConfigurationLocation> locations) {
        presetLocations.stream()
                .filter(presetLocation -> !locations.contains(presetLocation))
                .forEach(presetLocation -> locations.add(0, presetLocation));
        return locations;
    }

    @NotNull
    private Map<String, String> propertiesFor(final Map.Entry<String, String> storageEntry) {
        final Map<String, String> properties = new HashMap<>();

        final String propertyPrefix = propertyPrefix(storageEntry.getKey());

        // loop again over all settings to find the properties belonging to this configuration
        // not the best solution, but since there are only few items it doesn't hurt too much...
        storage.entrySet().stream()
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

    private ConfigurationLocationFactory configurationLocationFactory() {
        return ServiceManager.getService(project, ConfigurationLocationFactory.class);
    }

    public void setConfigurationLocations(final List<ConfigurationLocation> configurationLocations) {
        setConfigurationLocations(configurationLocations, true);
    }

    private List<ConfigurationLocation> setConfigurationLocations(final List<ConfigurationLocation> configurationLocations,
                                                                  final boolean fireEvents) {
        storageLock.lock();
        try {
            removeUnknownProperties();

            if (configurationLocations == null) {
                return Collections.emptyList();
            }

            int index = 0;
            for (ConfigurationLocation configurationLocation : configurationLocations) {
                storage.put(LOCATION_PREFIX + index, configurationLocation.getDescriptor());

                try {
                    final Map<String, String> properties = configurationLocation.getProperties();
                    if (properties != null) {
                        for (Map.Entry<String, String> entry : properties.entrySet()) {
                            String value = entry.getValue();
                            if (value == null) {
                                value = "";
                            }
                            storage.put(PROPERTIES_PREFIX + index + "." + entry.getKey(), value);
                        }
                    }
                } catch (IOException e) {
                    LOG.error("Failed to read properties from " + configurationLocation, e);
                    Notifications.showError(project,
                            CheckStyleBundle.message("checkstyle.could-not-read-properties", configurationLocation.getLocation()));
                }

                ++index;
            }

            if (fireEvents) {
                fireConfigurationChanged();
            }

            return configurationLocations;

        } finally {
            storageLock.unlock();
        }
    }

    private void removeUnknownProperties() {
        for (final Iterator i = storage.keySet().iterator(); i.hasNext(); ) {
            final String propertyName = i.next().toString();
            if (propertyName.startsWith(LOCATION_PREFIX) || propertyName.startsWith(PROPERTIES_PREFIX)) {
                i.remove();
            }
        }
    }

    @NotNull
    public List<String> getThirdPartyClassPath() {
        final List<String> thirdPartyClasspath = new ArrayList<>();

        final String value = storage.get(THIRDPARTY_CLASSPATH);
        if (value != null) {
            final String[] parts = value.split(";");
            for (final String part : parts) {
                thirdPartyClasspath.add(untokenisePath(part));
            }
        }

        return thirdPartyClasspath;
    }

    public void setThirdPartyClassPath(final List<String> value) {
        if (value == null) {
            storage.remove(THIRDPARTY_CLASSPATH);
            return;
        }

        final StringBuilder valueString = new StringBuilder();
        for (final String part : value) {
            if (valueString.length() > 0) {
                valueString.append(";");
            }
            valueString.append(tokenisePath(part));
        }

        storage.put(THIRDPARTY_CLASSPATH, valueString.toString());
    }

    @NotNull
    public ScanScope getScanScope() {
        return scopeValueOf(SCANSCOPE_SETTING);
    }

    public void setScanScope(@Nullable final ScanScope pScanScope) {
        storage.put(SCANSCOPE_SETTING, pScanScope != null ? pScanScope.name() : ScanScope.getDefaultValue().name());
    }

    public boolean isSuppressingErrors() {
        return booleanValueOf(SUPPRESS_ERRORS);
    }

    public void setSuppressingErrors(final boolean suppressingErrors) {
        save(SUPPRESS_ERRORS, suppressingErrors);
    }

    public boolean isScanFilesBeforeCheckin() {
        return booleanValueOf(SCAN_BEFORE_CHECKIN);
    }

    public void setScanFilesBeforeCheckin(final boolean scanFilesBeforeCheckin) {
        save(SCAN_BEFORE_CHECKIN, scanFilesBeforeCheckin);
    }

    private void save(final String propertyName, final boolean propertyValue) {
        storage.put(propertyName, Boolean.toString(propertyValue));
    }

    private boolean booleanValueOf(final String propertyName) {
        final String propertyValue = storage.get(propertyName);
        return propertyValue != null && Boolean.valueOf(propertyValue);
    }

    @NotNull
    private ScanScope scopeValueOf(final String propertyName) {
        final String propertyValue = storage.get(propertyName);
        ScanScope result = ScanScope.getDefaultValue();
        if (propertyValue != null) {
            try {
                result = ScanScope.valueOf(propertyValue);
            }
            catch (IllegalArgumentException e) {
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
                return untokeniseForPrefix(path, prefix, getProjectPath());
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
     * Process a path and add tokens as necessary.
     *
     * @param path the path to processed.
     * @return the tokenised path.
     */
    private String tokenisePath(final String path) {
        if (path == null) {
            return null;
        }

        final File projectPath = getProjectPath();
        if (projectPath != null) {
            final String projectPathAbs = projectPath.getAbsolutePath();
            if (path.startsWith(projectPathAbs)) {
                return PROJECT_DIR + path.substring(
                        projectPathAbs.length());
            }
        }

        return path;
    }

    /**
     * Get the base path of the project.
     *
     * @return the base path of the project.
     */
    @Nullable
    private File getProjectPath() {
        final VirtualFile baseDir = project.getBaseDir();
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
        storageLock.lock();
        try {
            return new ProjectSettings(storage);
        } finally {
            storageLock.unlock();
        }
    }


    /**
     * Load the state from the given settings beans.
     *
     * @param projectSettings the project settings to load.
     */
    public void loadState(final ProjectSettings projectSettings) {
        storageLock.lock();
        try {
            storage.clear();
            if (projectSettings != null) {
                Map<String, String> loadedMap = projectSettings.configurationAsMap();
                convertSettingsFormat(loadedMap);
                storage.putAll(loadedMap);
            }
        } finally {
            storageLock.unlock();
        }
    }



    /**
     * Needed when a setting written by a previous version of this plugin gets loaded by a newer version; converts
     * the scan scope settings based on flags to the enum value.
     * @param pLoadedMap the loaded settings
     */
    private void convertSettingsFormat(final Map<String, String> pLoadedMap)
    {
        if (pLoadedMap != null && !pLoadedMap.isEmpty() && !pLoadedMap.containsKey(SCANSCOPE_SETTING)) {
            ScanScope scope = ScanScope.fromFlags(
                booleanValueOf(CHECK_TEST_CLASSES), booleanValueOf(CHECK_NONJAVA_FILES));
            pLoadedMap.put(SCANSCOPE_SETTING, scope.name());
            pLoadedMap.remove(CHECK_TEST_CLASSES);
            pLoadedMap.remove(CHECK_NONJAVA_FILES);
        }
    }



    /**
     * Wrapper class for IDEA state serialisation.
     */
    public static class ProjectSettings {
        // this must remain public for serialisation purposes
        public Map<String, String> configuration;

        public ProjectSettings() {
            this.configuration = new TreeMap<>();
        }

        public ProjectSettings(final Map<String, String> configuration) {
            this.configuration = new TreeMap<>(configuration);
        }

        public Map<String, String> configurationAsMap() {
            if (configuration == null) {
                return Collections.emptyMap();
            }
            return configuration;
        }
    }
}
