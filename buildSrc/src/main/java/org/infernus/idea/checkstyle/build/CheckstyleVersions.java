package org.infernus.idea.checkstyle.build;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

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
    private static final String PROP_BUNDLED_VERSIONS = "bundledVersions";
    private static final String PROP_NAME_BASEVERSION = "baseVersion";

    private final File propertyFile;

    private final SortedSet<String> versions;
    private final SortedSet<String> bundledVersions;

    private final String baseVersion;

    public CheckstyleVersions(final Project project) {
        this(new File(project.getProjectDir(), PROP_FILE));
    }

    CheckstyleVersions(final File propertyFile) {
        this.propertyFile = propertyFile;

        final Properties properties = readProperties();
        versions = buildVersionSet(properties);
        bundledVersions = buildBundledVersionSet(properties, versions);
        baseVersion = readBaseVersion(properties);
    }

    private SortedSet<String> buildVersionSet(final Properties properties) {
        SortedSet<String> theVersions = new TreeSet<>(new VersionComparator());
        theVersions.addAll(readVersions(properties, PROP_VERSIONS_SUPPORTED));
        return Collections.unmodifiableSortedSet(theVersions);
    }

    private SortedSet<String> buildBundledVersionSet(final Properties properties, final SortedSet<String> supportedVersions) {
        SortedSet<String> result = new TreeSet<>(new VersionComparator());
        result.addAll(readVersions(properties, PROP_BUNDLED_VERSIONS));
        for (final String version : result) {
            if (!supportedVersions.contains(version)) {
                throw new GradleException("Property '" + PROP_BUNDLED_VERSIONS + "' contains version " + version
                        + " which is not a supported version in configuration file '" + PROP_FILE + "'");
            }
        }
        return Collections.unmodifiableSortedSet(result);
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

    public File getPropertyFile() {
        return propertyFile;
    }

    public SortedSet<String> getVersions() {
        return versions;
    }

    public SortedSet<String> getBundledVersions() {
        return bundledVersions;
    }

    public String getBaseVersion() {
        return baseVersion;
    }

    public static String toGradleVersion(final String checkstyleVersion) {
        return checkstyleVersion.replaceAll("\\.", "_");
    }

    public static Dependency createCheckstyleDependency(final Project project, final String checkstyleVersion) {
        return createCheckstyleDependency(project.getDependencies(), checkstyleVersion);
    }

    public static Dependency createCheckstyleDependency(final DependencyHandler dependencies, final String checkstyleVersion) {
        final ModuleDependency csDep = (ModuleDependency) dependencies.create(
                "com.puppycrawl.tools:checkstyle:" + checkstyleVersion);
        final Map<String, String> ex = new HashMap<>();
        ex.put("group", "commons-logging");
        ex.put("module", "commons-logging");
        csDep.exclude(ex);
        final Map<String, String> textEx = new HashMap<>();
        textEx.put("group", "org.apache.commons");
        textEx.put("module", "commons-text");
        csDep.exclude(textEx);
        return csDep;
    }

    /**
     * Workaround for <a href="https://github.com/checkstyle/checkstyle/issues/14123">Checkstyle#14123</a>:
     * resolve the {@code com.google.collections:google-collections} capability conflict by selecting Guava.
     */
    public static void applyGoogleCollectionsWorkaround(final Configuration configuration) {
        configuration.getResolutionStrategy()
                .getCapabilitiesResolution()
                .withCapability("com.google.collections", "google-collections",
                        resolutionDetails -> resolutionDetails.select("com.google.guava:guava:0"));
    }
}
