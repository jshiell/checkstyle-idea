package org.infernus.idea.checkstyle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.apache.commons.io.IOUtils;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Makes the Checkstyle tool available to the plugin in the correct version. Registered in {@code plugin.xml}.
 * This must be a project-level service because the Checkstyle version is chosen per project.
 */
public class CheckstyleProjectService
{
    private static final String PROP_FILE = "checkstyle-idea.properties";

    private static final String PROP_NAME_JAVA7 = "checkstyle.versions.java7";

    private static final String PROP_NAME_JAVA8 = "checkstyle.versions.java8";

    /** mock instance which may be set and used by unit tests */
    private static CheckstyleProjectService sMock = null;

    private final Project project;

    private Callable<CheckstyleClassLoader> checkstyleClassLoaderFactory = null;
    private CheckstyleClassLoader checkstyleClassLoader = null;

    private final SortedSet<String> supportedVersions;


    public CheckstyleProjectService(@NotNull final Project project) {
        this.project = project;
        supportedVersions = readSupportedVersions();
        activateCheckstyleVersion(getDefaultVersion());
    }


    /**
     * Read the supported Checkstyle versions from the config properties file.
     *
     * @return the supported versions which match the Java level of the current JVM
     */
    private SortedSet<String> readSupportedVersions() {
        final Properties props = readProperties();
        final String javaVersion = Runtime.class.getPackage().getSpecificationVersion();

        final SortedSet<String> theVersions = new TreeSet<>(new VersionComparator());
        theVersions.addAll(readVersions(props, PROP_NAME_JAVA7));
        if (!javaVersion.startsWith("1.7")) {
            theVersions.addAll(readVersions(props, PROP_NAME_JAVA8));
        }
        return Collections.unmodifiableSortedSet(theVersions);
    }


    private Properties readProperties() {
        final Properties props = new Properties();
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(PROP_FILE);
            if (is == null) {
                // in unit tests, it seems we need this:
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROP_FILE);
            }
            if (is != null) {
                props.load(is);
            }
        } catch (IllegalArgumentException | IOException e) {
            throw new CheckStylePluginException("Internal error: Could not read internal configuration file '" +
                    PROP_FILE + "'", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
        if (props.isEmpty()) {
            throw new CheckStylePluginException("Internal error: Could not read internal configuration file '" +
                    PROP_FILE + "'");
        }
        return props;
    }


    private Set<String> readVersions(final Properties props, final String propertyName) {
        final String propertyValue = props.getProperty(propertyName);
        if (Strings.isBlank(propertyValue)) {
            throw new CheckStylePluginException("Internal error: Property '" + propertyName + "' missing from " +
                    "configuration file '" + PROP_FILE + "'");
        }

        final String[] versions = propertyValue.trim().split("\\s*,\\s*");
        final Set<String> result = new HashSet<>();
        for (final String version : versions) {
            if (!version.isEmpty()) {
                result.add(version);
            }
        }

        if (result.isEmpty()) {
            throw new CheckStylePluginException("Internal error: Property '" + propertyName + "' was empty in " +
                    "configuration file '" + PROP_FILE + "'");
        }
        return result;
    }


    @NotNull
    public SortedSet<String> getSupportedVersions() {
        return supportedVersions;
    }


    public boolean isSupportedVersion(@Nullable final String pVersion) {
        return pVersion != null && supportedVersions.contains(pVersion);
    }


    @NotNull
    public String getDefaultVersion() {
        return supportedVersions.last();
    }


    public void activateCheckstyleVersion(@Nullable final String pVersion) {
        final String version = isSupportedVersion(pVersion) ? pVersion : getDefaultVersion();
        synchronized (project) {
            checkstyleClassLoaderFactory = new Callable<CheckstyleClassLoader>()
            {
                @Override
                public CheckstyleClassLoader call() {
                    return new CheckstyleClassLoader(project, version);
                }
            };
            checkstyleClassLoader = null;
        }
    }


    public CheckstyleActions getCheckstyleInstance() {
        try {
            synchronized (project) {
                if (checkstyleClassLoader == null) {
                    checkstyleClassLoader = checkstyleClassLoaderFactory.call();
                }
                // Don't worry about caching, class loaders do lots of caching.
                return checkstyleClassLoader.loadCheckstyleImpl();
            }
        } catch (CheckStylePluginException e) {
            throw e;
        } catch (Exception e) {
            throw new CheckStylePluginException("internal error", e);
        }
    }


    public static CheckstyleProjectService getInstance(@NotNull final Project pProject) {
        CheckstyleProjectService result = sMock;
        if (result == null) {
            result = ServiceManager.getService(pProject, CheckstyleProjectService.class);
        }
        return result;
    }


    public static void activateMock4UnitTesting(@Nullable final CheckstyleProjectService pMock) {
        sMock = pMock;
    }
}
