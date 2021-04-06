package org.infernus.idea.checkstyle.build;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


/**
 * Read the {@code checkstyle.versions.*} and the {@code baseVersion} properties from <i>checkstyle-idea.properties</i>
 * and make them available to the build process.
 */
public class CheckstyleVersions {

    private static final String PROP_FILE = "src/main/resources/checkstyle-idea.properties";

    private static final String PROP_VERSIONS_SUPPORTED = "checkstyle.versions.supported";
    private static final String PROP_DEPENDENCY_MAP = "checkstyle.dependencies.map";
    private static final String PROP_NAME_BASEVERSION = "baseVersion";

    private final File propertyFile;

    private final SortedSet<String> versions;
    private final Map<String, String> dependencyMappings;

    private final String baseVersion;

    public CheckstyleVersions(final Project project) {
        propertyFile = new File(project.getProjectDir(), PROP_FILE);

        final Properties properties = readProperties();
        versions = buildVersionSet(properties);
        baseVersion = readBaseVersion(properties);
        dependencyMappings = readDependencyMap(properties);
    }

    private SortedSet<String> buildVersionSet(final Properties properties) {
        SortedSet<String> theVersions = new TreeSet<>(new VersionComparator());
        theVersions.addAll(readVersions(properties, PROP_VERSIONS_SUPPORTED));
        return Collections.unmodifiableSortedSet(theVersions);
    }

    private Properties readProperties() {
        final Properties props = new Properties();
        try (InputStream is = new FileInputStream(propertyFile)) {
            props.load(is);
        } catch (IllegalArgumentException | SecurityException | IOException e) {
            throw new GradleException("Error reading configuration file '" + propertyFile + "' during build.", e);
        }
        return props;
    }

    private Set<String> readVersions(final Properties props, final String propertyName) {
        final String propertyValue = props.getProperty(propertyName);
        if (propertyValue == null || propertyValue.trim().isEmpty()) {
            throw new GradleException("Property '" + propertyName + "' missing from configuration file '" + PROP_FILE + "'");
        }

        final String[] checkstyleVersions = propertyValue.trim().split("\\s*,\\s*");
        final Set<String> result = new HashSet<>();
        for (final String version : checkstyleVersions) {
            if (!version.isEmpty()) {
                result.add(version);
            }
        }

        if (result.isEmpty()) {
            throw new GradleException("Property '" + propertyName + "' was empty in configuration file '" + PROP_FILE + "'");
        }
        return result;
    }

    private String readBaseVersion(final Properties properties) {
        final String baseVersionValue = properties.getProperty(PROP_NAME_BASEVERSION);
        if (baseVersionValue == null || baseVersionValue.trim().isEmpty()) {
            throw new GradleException("Property '" + PROP_NAME_BASEVERSION + "' missing from configuration file '"
                    + PROP_FILE + "'");
        }
        if (!versions.contains(baseVersionValue)) {
            throw new GradleException("Specified base version '" + baseVersionValue + "' is not a supported version. "
                    + "Supported versions: " + versions);
        }
        return baseVersionValue;
    }

    private Map<String, String> readDependencyMap(final Properties properties) {
        final String propertyValue = properties.getProperty(PROP_DEPENDENCY_MAP);
        if (propertyValue == null || propertyValue.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, String> mappings = new HashMap<>();
        for (final String mapping : propertyValue.trim().split("\\s*,\\s*")) {
            if (!mapping.isEmpty()) {
                final String[] oldDependencyToNewDependency = parseKeyValueMapping(mapping);
                mappings.put(oldDependencyToNewDependency[0], oldDependencyToNewDependency[1]);
            }
        }
        return Collections.unmodifiableMap(mappings);
    }

    public File getPropertyFile() {
        return propertyFile;
    }

    public SortedSet<String> getVersions() {
        return versions;
    }

    public String getBaseVersion() {
        return baseVersion;
    }

    public static String toGradleVersion(final String checkstyleVersion) {
        return checkstyleVersion.replaceAll("\\.", "_");
    }

    public static Dependency createCheckstyleDependency(final Project project, final String checkstyleVersion) {
        final ModuleDependency csDep = (ModuleDependency) project.getDependencies().create(
                "com.puppycrawl.tools:checkstyle:" + checkstyleVersion);
        final Map<String, String> ex = new HashMap<>();
        ex.put("group", "commons-logging");
        ex.put("module", "commons-logging");
        csDep.exclude(ex);
        return csDep;
    }

    private String[] parseKeyValueMapping(final String mapping) {
        final String[] kv = mapping.split("\\s*->\\s*");
        if (kv.length != 2) {
            throw new GradleException("Internal error: Property '" + CheckstyleVersions.PROP_DEPENDENCY_MAP
                    + "' contains invalid mapping '" + mapping + "'");
        }
        return kv;
    }

    public Map<String, String> getDependencyMappings() {
        return dependencyMappings;
    }
}
