package org.infernus.idea.checkstyle;

import org.apache.commons.io.IOUtils;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.util.Strings;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;


/**
 * Reads the list of supported Checkstyle versions from the <tt>checkstyle-idea.properties</tt> and provides it to
 * the Java code.
 */
class VersionListReader
{
    private static final String PROP_FILE = "checkstyle-idea.properties";

    private static final String PROP_NAME_JAVA7 = "checkstyle.versions.java7";
    private static final String PROP_NAME_JAVA8 = "checkstyle.versions.java8";

    private final SortedSet<String> supportedVersions;


    VersionListReader() {
        this(PROP_FILE);
    }

    VersionListReader(@NotNull final String pPropertyFile) {
        supportedVersions = readSupportedVersions(pPropertyFile);
    }


    /**
     * Read the supported Checkstyle versions from the config properties file.
     *
     * @param pPropertyFile file name of the property file to be passed to {@link ClassLoader#getResourceAsStream}
     * @return the supported versions which match the Java level of the current JVM
     */
    @NotNull
    private SortedSet<String> readSupportedVersions(@NotNull final String pPropertyFile) {
        final Properties props = readProperties(pPropertyFile);
        final String javaVersion = Runtime.class.getPackage().getSpecificationVersion();

        final SortedSet<String> theVersions = new TreeSet<>(new VersionComparator());
        theVersions.addAll(readVersions(pPropertyFile, props, PROP_NAME_JAVA7));
        if (!javaVersion.startsWith("1.7")) {
            theVersions.addAll(readVersions(pPropertyFile, props, PROP_NAME_JAVA8));
        }
        return Collections.unmodifiableSortedSet(theVersions);
    }


    @NotNull
    private Properties readProperties(@NotNull final String pPropertyFile) {
        final Properties props = new Properties();
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(pPropertyFile);
            if (is == null) {
                // in unit tests, it seems we need this:
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(pPropertyFile);
            }
            if (is != null) {
                props.load(is);
            }
        } catch (IllegalArgumentException | IOException e) {
            throw new CheckStylePluginException("Internal error: Could not read internal configuration file '"
                    + pPropertyFile + "'", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
        if (props.isEmpty()) {
            throw new CheckStylePluginException("Internal error: Could not read internal configuration file '"
                    + pPropertyFile + "'");
        }
        return props;
    }


    @NotNull
    private Set<String> readVersions(@NotNull final String pPropertyFile, @NotNull final Properties props,
                                     @NotNull final String propertyName) {
        final String propertyValue = props.getProperty(propertyName);
        if (Strings.isBlank(propertyValue)) {
            throw new CheckStylePluginException("Internal error: Property '" + propertyName + "' missing from "
                    + "configuration file '" + pPropertyFile + "'");
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
                    + "configuration file '" + pPropertyFile + "'");
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
