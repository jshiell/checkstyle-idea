package org.infernus.idea.checkstyle.build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;


/**
 * Read the {@code checkstyle.versions.*} and the {@code baseVersion} properties from <i>checkstyle-idea.properties</i>
 * and make them available to the build process.
 */
public class CheckstyleVersions
{
    private static final String PROP_FILE = "src/main/resources/checkstyle-idea.properties";

    private static final String PROP_NAME_JAVA7 = "checkstyle.versions.java7";

    private static final String PROP_NAME_JAVA8 = "checkstyle.versions.java8";

    private static final String PROP_NAME_BASEVERSION = "baseVersion";

    private final File propertyFile;

    private final SortedSet<String> versions;

    private final String baseVersion;


    public CheckstyleVersions(final Project project) {
        propertyFile = new File(project.getProjectDir(), PROP_FILE);
        final Properties props = readProperties();
        versions = buildVersionSet(props);
        baseVersion = readBaseVersion(props);
    }


    private SortedSet<String> buildVersionSet(final Properties pProperties) {
        SortedSet<String> theVersions = new TreeSet<>(new VersionComparator());
        theVersions.addAll(readVersions(pProperties, PROP_NAME_JAVA7));
        Set<String> versions8 = readVersions(pProperties, PROP_NAME_JAVA8);
        if (!Collections.disjoint(theVersions, versions8)) {
            throw new GradleException("Properties '" + PROP_NAME_JAVA7 + "' and '" + PROP_NAME_JAVA8 + "' contain " +
                    "duplicate entries in configuration file '" + PROP_FILE + "'");
        }
        theVersions.addAll(versions8);
        return Collections.unmodifiableSortedSet(theVersions);
    }


    private Properties readProperties() {
        final Properties props = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(propertyFile);
            try {
                props.load(is);
            } catch (IllegalArgumentException | IOException e) {
                throw new GradleException("Error reading configuration file '" + propertyFile + "' during build.", e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        } catch (SecurityException | FileNotFoundException e) {
            throw new GradleException("Could not find configuration file '" + propertyFile + "' during build.", e);
        }
        return props;
    }


    private Set<String> readVersions(final Properties props, final String propertyName) {
        final String propertyValue = props.getProperty(propertyName);
        if (propertyValue == null || propertyValue.trim().isEmpty()) {
            throw new GradleException("Property '" + propertyName + "' missing from configuration file '" + PROP_FILE
                    + "'");
        }

        final String[] versions = propertyValue.trim().split("\\s*,\\s*");
        final Set<String> result = new HashSet<>();
        for (final String version : versions) {
            if (!version.isEmpty()) {
                result.add(version);
            }
        }

        if (result.isEmpty()) {
            throw new GradleException("Property '" + propertyName + "' was empty in configuration file '" + PROP_FILE
                    + "'");
        }
        return result;
    }


    private String readBaseVersion(final Properties pProperties) {
        final String baseVersion = pProperties.getProperty(PROP_NAME_BASEVERSION);
        if (baseVersion == null || baseVersion.trim().isEmpty()) {
            throw new GradleException("Property '" + PROP_NAME_BASEVERSION + "' missing from configuration file '"
                    + PROP_FILE + "'");
        }
        if (!versions.contains(baseVersion)) {
            throw new GradleException("Specified base version '" + baseVersion + "' is not a supported version. "
                    + "Supported versions: " + versions);
        }
        return baseVersion;
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


    public static String toGradleVersion(final String pCheckstyleVersion) {
        return pCheckstyleVersion.replaceAll("\\.", "_");
    }


    public static Dependency createCheckstyleDependency(final Project pProject, final String pCheckstyleVersion) {
        final ModuleDependency csDep = (ModuleDependency) pProject.getDependencies().create( //
                "com.puppycrawl.tools:checkstyle:" + pCheckstyleVersion);
        final Map<String, String> ex = new HashMap<>();
        ex.put("group", "commons-logging");
        ex.put("module", "commons-logging");
        csDep.exclude(ex);
        return csDep;
    }
}
