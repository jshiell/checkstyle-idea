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
import org.infernus.idea.checkstyle.util.IDEAUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A manager for CheckStyle plug-in configuration.
 */
@State(
        name = CheckStyleConstants.ID_PLUGIN,
        storages = {
                @Storage(id = "default", file = StoragePathMacros.PROJECT_FILE),
                @Storage(id = "dir", file = StoragePathMacros.PROJECT_CONFIG_DIR + "/checkstyle-idea.xml", scheme = StorageScheme.DIRECTORY_BASED)
        }
)
public final class CheckStyleConfiguration implements ExportableComponent,
        PersistentStateComponent<CheckStyleConfiguration.ProjectSettings> {

    private static final Log LOG = LogFactory.getLog(CheckStyleConfiguration.class);

    private static final String ACTIVE_CONFIG = "active-configuration";
    private static final String CHECK_TEST_CLASSES = "check-test-classes";
    private static final String CHECK_NONJAVA_FILES = "check-nonjava-files";
    private static final String SUPPRESS_ERRORS = "suppress-errors";
    private static final String THIRDPARTY_CLASSPATH = "thirdparty-classpath";
    private static final String LOCATION_PREFIX = "location-";
    private static final String PROPERTIES_PREFIX = "property-";

    private static final String SUN_CHECKS_CONFIG = "/sun_checks.xml";

    private final Set<ConfigurationLocation> presetLocations = new HashSet<ConfigurationLocation>();
    private final SortedMap<String, String> storage = new ConcurrentSkipListMap<String, String>();
    private final ReentrantLock storageLock = new ReentrantLock();
    private final List<ConfigurationListener> configurationListeners = Collections.synchronizedList(new ArrayList<ConfigurationListener>());

    private final Project project;

    /**
     * Scan files before vcs checkin.
     */
    private boolean scanFilesBeforeCheckin = false;

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

        final ResourceBundle resources = ResourceBundle.getBundle(CheckStyleConstants.RESOURCE_BUNDLE);
        final ConfigurationLocation checkStyleSunChecks = configurationLocationFactory().create(project, ConfigurationType.CLASSPATH,
                SUN_CHECKS_CONFIG, resources.getString("file.default.description"));
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
        final List<ConfigurationLocation> configurationLocations = getConfigurationLocations();

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
            final List<ConfigurationLocation> configurationLocations = getConfigurationLocations();

            if (!storage.containsKey(ACTIVE_CONFIG)) {
                return null;
            }

            ConfigurationLocation activeLocation = null;
            try {
                activeLocation = configurationLocationFactory().create(project, storage.get(ACTIVE_CONFIG));
            } catch (IllegalArgumentException e) {
                LOG.warn("Could not load active configuration", e);
            }

            if (activeLocation == null || !configurationLocations.contains(activeLocation)) {
                LOG.info("Active configuration is invalid, returning null");
                return null;
            }

            // ensure we update the map with any parsing/tokenisation changes
            setActiveConfiguration(activeLocation);

            return activeLocation;
        } finally {
            storageLock.unlock();
        }
    }

    public List<ConfigurationLocation> getConfigurationLocations() {
        storageLock.lock();
        try {
            final List<ConfigurationLocation> locations = new ArrayList<ConfigurationLocation>();

            for (Map.Entry<String, String> entry : storage.entrySet()) {
                if (!entry.getKey().startsWith(LOCATION_PREFIX)) {
                    continue;
                }

                final String value = entry.getValue();
                try {
                    final ConfigurationLocation location = configurationLocationFactory().create(
                            project, value);

                    final Map<String, String> properties = new HashMap<String, String>();

                    final int index = Integer.parseInt(entry.getKey().substring(LOCATION_PREFIX.length()));
                    final String propertyPrefix = PROPERTIES_PREFIX + index + ".";

                    // loop again over all settings to find the properties belonging to this configuration
                    // not the best solution, but since there are only few items it doesn't hurt too much...
                    for (Map.Entry<String, String> innerEntry : storage.entrySet()) {
                        if (innerEntry.getKey().startsWith(propertyPrefix)) {
                            final String propertyName = innerEntry.getKey().substring(propertyPrefix.length());
                            properties.put(propertyName, innerEntry.getValue());
                        }
                    }

                    location.setProperties(properties);
                    locations.add(location);

                } catch (IllegalArgumentException e) {
                    LOG.error("Could not parse location: " + value, e);
                }
            }

            for (ConfigurationLocation presetLocation : presetLocations) {
                if (!locations.contains(presetLocation)) {
                    locations.add(0, presetLocation);
                }
            }

            // ensure we update the map with any parsing/tokenisation changes
            setConfigurationLocations(locations, false);

            return locations;
        } finally {
            storageLock.unlock();
        }
    }

    private ConfigurationLocationFactory configurationLocationFactory() {
        return ServiceManager.getService(project, ConfigurationLocationFactory.class);
    }

    public void setConfigurationLocations(final List<ConfigurationLocation> configurationLocations) {
        setConfigurationLocations(configurationLocations, true);
    }

    private void setConfigurationLocations(final List<ConfigurationLocation> configurationLocations,
                                           final boolean fireEvents) {
        storageLock.lock();
        try {
            for (final Iterator i = storage.keySet().iterator(); i.hasNext(); ) {
                final String propertyName = i.next().toString();
                if (propertyName.startsWith(LOCATION_PREFIX) || propertyName.startsWith(PROPERTIES_PREFIX)) {
                    i.remove();
                }
            }

            if (configurationLocations == null) {
                return;
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
                    final MessageFormat message = new MessageFormat(
                            IDEAUtilities.getResource("checkstyle.could-not-read-properties",
                                    "Properties could not be read from the CheckStyle configuration file from {0}."));
                    IDEAUtilities.showError(project, message.format(new Object[]{configurationLocation.getLocation()}));
                }

                ++index;
            }

            if (fireEvents) {
                fireConfigurationChanged();
            }

        } finally {
            storageLock.unlock();
        }
    }

    @NotNull
    public List<String> getThirdPartyClassPath() {
        final List<String> thirdPartyClasspath = new ArrayList<String>();

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

    public boolean isScanningTestClasses() {
        final String p = storage.get(CHECK_TEST_CLASSES);
        return p != null && Boolean.valueOf(p);
    }

    public void setScanningTestClasses(final boolean scanTestFles) {
        storage.put(CHECK_TEST_CLASSES, Boolean.toString(scanTestFles));
    }

    public boolean isScanningNonJavaFiles() {
        final String propertyValue = storage.get(CHECK_NONJAVA_FILES);
        return propertyValue != null && Boolean.valueOf(propertyValue);
    }

    public void setScanningNonJavaFiles(final boolean scanNonJavaFiles) {
        storage.put(CHECK_NONJAVA_FILES, Boolean.toString(scanNonJavaFiles));
    }

    public boolean isSuppressingErrors() {
        final String propertyValue = storage.get(SUPPRESS_ERRORS);
        return propertyValue != null && Boolean.valueOf(propertyValue);
    }

    public void setSuppressingErrors(final boolean suppressingErrors) {
        storage.put(SUPPRESS_ERRORS, Boolean.toString(suppressingErrors));
    }

    public boolean isScanFilesBeforeCheckin() {
        return scanFilesBeforeCheckin;
    }

    public void setScanFilesBeforeCheckin(final boolean scanFilesBeforeCheckin) {
        this.scanFilesBeforeCheckin = scanFilesBeforeCheckin;
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

        for (String prefix : new String[]{CheckStyleConstants.PROJECT_DIR, CheckStyleConstants.LEGACY_PROJECT_DIR}) {
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
                return CheckStyleConstants.PROJECT_DIR + path.substring(
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
                storage.putAll(projectSettings.configurationAsMap());
            }
        } finally {
            storageLock.unlock();
        }
    }

    /**
     * Wrapper class for IDEA state serialisation.
     */
    public static class ProjectSettings {
        public Map<String, String> configuration;

        public ProjectSettings() {
            this.configuration = new TreeMap<String, String>();
        }

        public ProjectSettings(final Map<String, String> configuration) {
            this.configuration = new TreeMap<String, String>(configuration);
        }

        public Map<String, String> configurationAsMap() {
            if (configuration == null) {
                return Collections.emptyMap();
            }
            return configuration;
        }
    }
}
