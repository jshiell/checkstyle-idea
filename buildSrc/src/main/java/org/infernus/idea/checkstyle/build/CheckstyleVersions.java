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
 * Read the {@code checkstyle.versions.*} properties from <i>checkstyle-idea.properties</i> and make them available
 * to the build process.
 */
public class CheckstyleVersions
{
    private static final String PROP_FILE = "src/main/resources/checkstyle-idea.properties";

    private static final String PROP_NAME_JAVA7 = "checkstyle.versions.java7";

    private static final String PROP_NAME_JAVA8 = "checkstyle.versions.java8";

    private final File propertyFile;

    private final SortedSet<String> versions;


    public CheckstyleVersions(final Project project) {
        propertyFile = new File(project.getProjectDir(), PROP_FILE);
        versions = buildVersionSet();
    }


    private SortedSet<String> buildVersionSet() {
        final Properties props = readProperties();
        SortedSet<String> theVersions = new TreeSet<>(new VersionComparator());
        theVersions.addAll(readVersions(props, PROP_NAME_JAVA7));
        Set<String> versions8 = readVersions(props, PROP_NAME_JAVA8);
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


    public File getPropertyFile() {
        return propertyFile;
    }


    public SortedSet<String> getVersions() {
        return versions;
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
