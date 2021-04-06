package org.infernus.idea.checkstyle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.intellij.openapi.util.Pair;
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
    private static final String PROP_VERSION_MAP = "checkstyle.versions.map";

    private final SortedSet<String> supportedVersions;
    private final SortedMap<String, String> replacementMap;

    public VersionListReader() {
        this(PROP_FILE);
    }

    VersionListReader(@NotNull final String propertyFile) {
        final Properties props = readProperties(propertyFile);
        supportedVersions = readSupportedVersions(propertyFile, props);
        replacementMap = readVersionMap(propertyFile, props, supportedVersions);
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
    private SortedMap<String, String> readVersionMap(@NotNull final String propertyFile,
                                                     @NotNull final Properties props,
                                                     @NotNull final SortedSet<String> pSupportedVersions) {
        final String propertyValue = props.getProperty(PROP_VERSION_MAP);
        if (Strings.isBlank(propertyValue)) {
            throw new CheckStylePluginException("Internal error: Property '" + PROP_VERSION_MAP + "' missing from "
                    + "configuration file '" + propertyFile + "'");
        }

        final String[] mappings = propertyValue.trim().split("\\s*,\\s*");
        final SortedMap<String, String> result = new TreeMap<>(new VersionComparator());
        for (final String mapping : mappings) {
            if (!mapping.isEmpty()) {
                final Pair<String, String> validMapping = readValidMapping(propertyFile, mapping, pSupportedVersions);
                if (result.containsKey(validMapping.getFirst())) {
                    throw new CheckStylePluginException("Internal error: Property '" + PROP_VERSION_MAP + "' "
                            + "contains duplicate mapping \"" + mapping + "\" in configuration file '" + propertyFile
                            + "'");
                }
                result.put(validMapping.getFirst(), validMapping.getSecond());
            }
        }
        return Collections.unmodifiableSortedMap(result);
    }


    private Pair<String, String> readValidMapping(@NotNull final String propertyFile,
                                                  @NotNull final String mapping,
                                                  @NotNull final SortedSet<String> pSupportedVersions) {

        final String[] kv = mapping.split("\\s*->\\s*");
        if (kv.length != 2) {
            throw new CheckStylePluginException("Internal error: Property '" + PROP_VERSION_MAP + "' contains "
                    + "invalid mapping '" + mapping + "' in configuration file '" + propertyFile + "'");
        }

        final String unsupportedVersion = kv[0];
        final String goodVersion = kv[1];
        if (unsupportedVersion.isEmpty() || goodVersion.isEmpty()) {
            throw new CheckStylePluginException("Internal error: Property '" + PROP_VERSION_MAP + "' contains "
                    + "invalid mapping '" + mapping + "' in configuration file '" + propertyFile + "'");
        }

        if (!pSupportedVersions.contains(goodVersion)) {
            throw new CheckStylePluginException("Internal error: Property '" + PROP_VERSION_MAP + "' contains "
                    + "invalid mapping '" + mapping + "'. Target version " + goodVersion + " is not a supported "
                    + "version in configuration file '" + propertyFile + "'");
        }
        if (pSupportedVersions.contains(unsupportedVersion)) {
            throw new CheckStylePluginException("Internal error: Property '" + PROP_VERSION_MAP + "' contains "
                    + "invalid mapping '" + mapping + "'. Checkstyle version " + unsupportedVersion + " is in "
                    + "fact supported in configuration file '" + propertyFile + "'");
        }
        return new Pair<>(unsupportedVersion, goodVersion);
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

    @NotNull
    public SortedMap<String, String> getReplacementMap() {
        return replacementMap;
    }
}
