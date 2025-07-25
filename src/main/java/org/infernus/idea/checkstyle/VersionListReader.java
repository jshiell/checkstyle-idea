package org.infernus.idea.checkstyle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.util.Strings;
import org.jetbrains.annotations.NotNull;


/**
 * Reads the list of supported Checkstyle versions from the <tt>checkstyle-idea.properties</tt> and provides it to
 * the Java code.
 */
public class VersionListReader {
    private static final String PROP_FILE = "checkstyle-idea.properties";

    private static final String PROP_SUPPORTED_VERSIONS = "checkstyle.versions.supported";

    private final SortedSet<String> supportedVersions;

    public VersionListReader() {
        this(PROP_FILE);
    }

    VersionListReader(@NotNull final String propertyFile) {
        final Properties props = readProperties(propertyFile);
        supportedVersions = readSupportedVersions(propertyFile, props);
    }


    @NotNull
    private Properties readProperties(@NotNull final String propertyFile) {
        final Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(propertyFile)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IllegalArgumentException | IOException e) {
            throw new CheckStylePluginException("Internal error: Could not read internal configuration file '"
                    + propertyFile + "'", e);
        }

        if (props.isEmpty()) {
            throw new CheckStylePluginException("Internal error: Could not read internal configuration file '"
                    + propertyFile + "'");
        }
        return props;
    }


    /**
     * Read the supported Checkstyle versions from the config properties file.
     *
     * @param propertyFile file name of the property file to be passed to {@link ClassLoader#getResourceAsStream}
     * @param props the properties read from the property file
     * @return the supported versions which match the Java level of the current JVM
     */
    @NotNull
    private SortedSet<String> readSupportedVersions(@NotNull final String propertyFile,
                                                    @NotNull final Properties props) {
        final SortedSet<String> theVersions = new TreeSet<>(new VersionComparator());
        theVersions.addAll(readVersions(propertyFile, props, PROP_SUPPORTED_VERSIONS));
        return Collections.unmodifiableSortedSet(theVersions);
    }


    @NotNull
    private Set<String> readVersions(@NotNull final String propertyFile,
                                     @NotNull final Properties props,
                                     @NotNull final String propertyName) {
        final String propertyValue = props.getProperty(propertyName);
        if (Strings.isBlank(propertyValue)) {
            throw new CheckStylePluginException("Internal error: Property '" + propertyName + "' missing from "
                    + "configuration file '" + propertyFile + "'");
        }

        final String[] versions = propertyValue.trim().split("\\s*,\\s*");
        final Set<String> result = new HashSet<>();
        for (final String version : versions) {
            if (!version.isEmpty()) {
                result.add(version);
            }
        }

        if (result.isEmpty()) {
            throw new CheckStylePluginException("Internal error: Property '" + propertyName + "' was empty in "
                    + "configuration file '" + propertyFile + "'");
        }
        return result;
    }

    @NotNull
    public SortedSet<String> getSupportedVersions() {
        return supportedVersions;
    }

    @NotNull
    public String getDefaultVersion() {
        return getDefaultVersion(supportedVersions);
    }

    @NotNull
    public static String getDefaultVersion(@NotNull final SortedSet<String> pSupportedVersions) {
        return pSupportedVersions.last();
    }
}
