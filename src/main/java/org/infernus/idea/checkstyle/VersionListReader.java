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
import org.apache.commons.io.IOUtils;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.util.Strings;
import org.jetbrains.annotations.NotNull;


/**
 * Reads the list of supported Checkstyle versions from the <tt>checkstyle-idea.properties</tt> and provides it to
 * the Java code.
 */
class VersionListReader
{
    private static final String PROP_FILE = "checkstyle-idea.properties";

    private static final String PROP_NAME_JAVA7 = "checkstyle.versions.java7";
    private static final String PROP_NAME_JAVA8 = "checkstyle.versions.java8";
    private static final String PROP_VERSION_MAP = "checkstyle.versions.map";

    private final SortedSet<String> supportedVersions;
    private final SortedMap<String, String> replacementMap;


    VersionListReader() {
        this(PROP_FILE);
    }

    VersionListReader(@NotNull final String pPropertyFile) {
        final Properties props = readProperties(pPropertyFile);
        supportedVersions = readSupportedVersions(pPropertyFile, props);
        replacementMap = readVersionMap(pPropertyFile, props, supportedVersions);
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


    /**
     * Read the supported Checkstyle versions from the config properties file.
     *
     * @param pPropertyFile file name of the property file to be passed to {@link ClassLoader#getResourceAsStream}
     * @param props the properties read from the property file
     * @return the supported versions which match the Java level of the current JVM
     */
    @NotNull
    private SortedSet<String> readSupportedVersions(@NotNull final String pPropertyFile,
                                                    @NotNull final Properties props) {
        final String javaVersion = Runtime.class.getPackage().getSpecificationVersion();

        final SortedSet<String> theVersions = new TreeSet<>(new VersionComparator());
        theVersions.addAll(readVersions(pPropertyFile, props, PROP_NAME_JAVA7));
        if (!javaVersion.startsWith("1.7")) {
            theVersions.addAll(readVersions(pPropertyFile, props, PROP_NAME_JAVA8));
        }
        return Collections.unmodifiableSortedSet(theVersions);
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
    private SortedMap<String, String> readVersionMap(@NotNull final String pPropertyFile,
                                                     @NotNull final Properties props,
                                                     @NotNull final SortedSet<String> pSupportedVersions) {
        final String propertyValue = props.getProperty(PROP_VERSION_MAP);
        if (Strings.isBlank(propertyValue)) {
            throw new CheckStylePluginException("Internal error: Property '" + PROP_VERSION_MAP + "' missing from "
                    + "configuration file '" + pPropertyFile + "'");
        }

        final String[] mappings = propertyValue.trim().split("\\s*,\\s*");
        final SortedMap<String, String> result = new TreeMap<>(new VersionComparator());
        for (final String mapping : mappings) {
            if (!mapping.isEmpty()) {
                final Pair<String, String> validMapping = readValidMapping(pPropertyFile, mapping, pSupportedVersions);
                if (result.containsKey(validMapping.getFirst())) {
                    throw new CheckStylePluginException("Internal error: Property '" + PROP_VERSION_MAP + "' "
                            + "contains duplicate mapping \"" + mapping + "\" in configuration file '" + pPropertyFile
                            + "'");
                }
                result.put(validMapping.getFirst(), validMapping.getSecond());
            }
        }
        return Collections.unmodifiableSortedMap(result);
    }


    private Pair<String, String> readValidMapping(@NotNull final String pPropertyFile, @NotNull final String
            pMapping, @NotNull final SortedSet<String> pSupportedVersions) {

        final String[] kv = pMapping.split("\\s*->\\s*");
        if (kv.length != 2) {
            throw new CheckStylePluginException("Internal error: Property '" + PROP_VERSION_MAP + "' contains "
                    + "invalid mapping '" + pMapping + "' in configuration file '" + pPropertyFile + "'");
        }

        final String unsupportedVersion = kv[0];
        final String goodVersion = kv[1];
        if (unsupportedVersion.isEmpty() || goodVersion.isEmpty()) {
            throw new CheckStylePluginException("Internal error: Property '" + PROP_VERSION_MAP + "' contains "
                    + "invalid mapping '" + pMapping + "' in configuration file '" + pPropertyFile + "'");
        }

        if (!pSupportedVersions.contains(goodVersion)) {
            throw new CheckStylePluginException("Internal error: Property '" + PROP_VERSION_MAP + "' contains "
                    + "invalid mapping '" + pMapping + "'. Target version " + goodVersion + " is not a supported "
                    + "version in configuration file '" + pPropertyFile + "'");
        }
        if (pSupportedVersions.contains(unsupportedVersion)) {
            throw new CheckStylePluginException("Internal error: Property '" + PROP_VERSION_MAP + "' contains "
                    + "invalid mapping '" + pMapping + "'. Checkstyle version " + unsupportedVersion + " is in "
                    + "fact supported in configuration file '" + pPropertyFile + "'");
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
