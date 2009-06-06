package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * A manager for CheckStyle plug-in configuration.
 *
 * @author James Shiell
 * @version 1.0
 */
public final class CheckStyleConfiguration extends Properties {

    @NonNls
    private static final Log LOG = LogFactory.getLog(CheckStyleConfiguration.class);

    private static final long serialVersionUID = 2804470793153612480L;

    private static final String ACTIVE_CONFIG = "active-configuration";
    private static final String CHECK_TEST_CLASSES = "check-test-classes";
    private static final String THIRDPARTY_CLASSPATH = "thirdparty-classpath";
    private static final String LOCATION_PREFIX = "location-";
    private static final String PROPERTIES_PREFIX = "property-";

    private static final String DEFAULT_CONFIG = "/sun_checks.xml";

    private final Project project;
    private final ConfigurationLocation defaultLocation;

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

        final ResourceBundle resources = ResourceBundle.getBundle(
                CheckStyleConstants.RESOURCE_BUNDLE);
        defaultLocation = ConfigurationLocationFactory.create(project, ConfigurationType.CLASSPATH,
                DEFAULT_CONFIG, resources.getString("file.default.description"));
    }

    public ConfigurationLocation getDefaultLocation() {
        return defaultLocation;
    }

    public void setActiveConfiguration(final ConfigurationLocation configurationLocation) {
        final List<ConfigurationLocation> configurationLocations = getConfigurationLocations();

        if (configurationLocation != null && !configurationLocations.contains(configurationLocation)) {
            throw new IllegalArgumentException("Location is not valid: " + configurationLocation);
        }

        if (configurationLocation != null) {
            setProperty(ACTIVE_CONFIG, configurationLocation.getDescriptor());
        } else {
            remove(ACTIVE_CONFIG);
        }
    }

    public ConfigurationLocation getActiveConfiguration() {
        final List<ConfigurationLocation> configurationLocations = getConfigurationLocations();

        if (!containsKey(ACTIVE_CONFIG)) {
            return defaultLocation;
        }

        ConfigurationLocation activeLocation = null;
        try {
            activeLocation = ConfigurationLocationFactory.create(project, getProperty(ACTIVE_CONFIG));
        } catch (IllegalArgumentException e) {
            LOG.warn("Could not load active configuration", e);
        }

        if (activeLocation == null || !configurationLocations.contains(activeLocation)) {
            LOG.info("Active configuration is invalid, returning default");
            return defaultLocation;
        }

        return activeLocation;
    }

    public List<ConfigurationLocation> getConfigurationLocations() {
        final List<ConfigurationLocation> locations = new ArrayList<ConfigurationLocation>();

        for (Object configProperty : keySet()) {
            if (!configProperty.toString().startsWith(LOCATION_PREFIX)) {
                continue;
            }

            try {
                final ConfigurationLocation location = ConfigurationLocationFactory.create(
                        project, getProperty(configProperty.toString()));

                final Map<String, String> properties = new HashMap<String, String>();

                final int index = Integer.parseInt(configProperty.toString().substring(LOCATION_PREFIX.length()));
                final String propertyPrefix = PROPERTIES_PREFIX + index + ".";
                for (Object innerConfigProperty : keySet()) {
                    if (!innerConfigProperty.toString().startsWith(propertyPrefix)) {
                        continue;
                    }

                    final String propertyName = innerConfigProperty.toString().substring(propertyPrefix.length());
                    properties.put(propertyName, getProperty(innerConfigProperty.toString()));
                }

                location.setProperties(properties);
                locations.add(location);

            } catch (IllegalArgumentException e) {
                LOG.error("Could not parse location: " + getProperty(configProperty.toString()), e);
            }
        }

        if (!locations.contains(defaultLocation)) {
            locations.add(0, defaultLocation);
        }

        return locations;
    }

    public void setConfigurationLocations(final List<ConfigurationLocation> configurationLocations) {
        for (Iterator i = keySet().iterator(); i.hasNext();) {
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
            setProperty(LOCATION_PREFIX + index, configurationLocation.getDescriptor());

            final Map<String, String> properties = configurationLocation.getProperties();
            if (properties != null) {
                for (final String property : properties.keySet()) {
                    setProperty(PROPERTIES_PREFIX + index + "." + property, properties.get(property));
                }
            }

            ++index;
        }
    }

    @NotNull
    public List<String> getThirdPartyClassPath() {
        final List<String> thirdPartyClasspath = new ArrayList<String>();

        final String value = getProperty(THIRDPARTY_CLASSPATH);
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
            remove(THIRDPARTY_CLASSPATH);
            return;
        }

        final StringBuilder valueString = new StringBuilder();
        for (final String part : value) {
            if (valueString.length() > 0) {
                valueString.append(";");
            }
            valueString.append(tokenisePath(part));
        }

        setProperty(THIRDPARTY_CLASSPATH, valueString.toString());
    }

    public boolean isScanningTestClasses() {
        return Boolean.valueOf(getProperty(CHECK_TEST_CLASSES,
                Boolean.toString(false)));
    }

    public void setScanningTestClasses(final boolean scanTestFles) {
        setProperty(CHECK_TEST_CLASSES, Boolean.toString(scanTestFles));
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

        if (path.startsWith(CheckStyleConstants.PROJECT_DIR)) {
            final File projectPath = getProjectPath();
            if (projectPath != null) {
                final File fullConfigFile = new File(projectPath,
                        path.substring(CheckStyleConstants.PROJECT_DIR.length()));
                return fullConfigFile.getAbsolutePath();
            } else {
                LOG.warn("Could not untokenise path as project dir is unset: "
                        + path);
            }
        }

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
        if (project == null) {
            return null;
        }

        final VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        return new File(baseDir.getPath());
    }
}
