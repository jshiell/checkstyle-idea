package org.infernus.idea.checkstyle;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.lang.UrlClassLoader;
import org.infernus.idea.checkstyle.csapi.CheckstyleActions;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.util.ChildFirstURLClassLoader;
import org.infernus.idea.checkstyle.util.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.util.Strings.isBlank;


/**
 * Loads Checkstyle classes from a given Checkstyle version.
 */
public class CheckstyleClassLoaderContainer {
    private static final String PROP_FILE = "checkstyle-classpaths.properties";
    private static final String CSACTIONS_CLASS = "org.infernus.idea.checkstyle.service.CheckstyleActionsImpl";

    /**
     * First pattern we can use to guess the build dir, valid for IntelliJ 2017.2 and later. {@code /build} must be
     * appended to the result afterwards.
     * <img src="doc-files/CheckstyleClassLoader-1.png"/>
     */
    private static final Pattern CLASSES_URL_2017_2_LATER = Pattern.compile(
            "^(.*?)[/\\\\]out[/\\\\]production[/\\\\]classes[/\\\\]?$");

    /**
     * Second pattern we can use to guess the build dir, valid for IntelliJ 2017.1 or earlier:
     * <img src="doc-files/CheckstyleClassLoader-2.png"/>
     */
    private static final Pattern CLASSES_URL_2017_1_EARLIER = Pattern.compile(
            "^(.*?)[/\\\\]classes(?:[/\\\\]main)?[/\\\\]?$");

    /**
     * Or if we're building in IDEA with Gradle, it'll be something like:
     * /Users/foo/Projects/checkstyle-idea/build/classes/java/main/
     */
    private static final Pattern CLASSES_URL_GRADLE = Pattern.compile(
            "^(.*?)[/\\\\]classes[/\\\\]java[/\\\\]main[/\\\\]?$");

    private final ClassLoader classLoader;

    private final Project project;
    private final CheckstyleProjectService checkstyleProjectService;

    public CheckstyleClassLoaderContainer(@NotNull final Project project,
                                          @NotNull final CheckstyleProjectService checkstyleProjectService,
                                          @NotNull final String checkstyleVersion,
                                          @Nullable final List<URL> thirdPartyClassPath) {
        this.project = project;
        this.checkstyleProjectService = checkstyleProjectService;

        final Properties classPathInfos = loadClassPathInfos();
        final String cpProp = classPathInfos.getProperty(checkstyleVersion);
        if (isBlank(cpProp)) {
            throw new CheckStylePluginException("Unsupported Checkstyle version: " + checkstyleVersion);
        }
        classLoader = buildClassLoader(cpProp, emptyListIfNull(thirdPartyClassPath));
    }

    @NotNull
    private List<URL> emptyListIfNull(@Nullable final List<URL> potentiallyNullList) {
        return Objects.requireNonNullElse(potentiallyNullList, Collections.emptyList());
    }

    @NotNull
    private static Properties loadClassPathInfos() {
        final Properties result = new Properties();
        try (InputStream is = CheckstyleClassLoaderContainer.class.getClassLoader().getResourceAsStream(PROP_FILE)) {
            result.load(is);
        } catch (IOException e) {
            throw new CheckStylePluginException("Could not read plugin-internal file: " + PROP_FILE, e);
        }
        return result;
    }

    @NotNull
    private ClassLoader buildClassLoader(@NotNull final String classPathFromProps,
                                         @NotNull final List<URL> thirdPartyClasspath) {
        final String basePluginPath = getBasePluginPath();

        List<URL> urls;
        if (basePluginPath != null) {
            urls = baseClasspathUrlsForPackagedPlugin(classPathFromProps, basePluginPath);
        } else {
            urls = baseClasspathUrlsForIDEAUnitTests(classPathFromProps);
        }

        urls.addAll(thirdPartyClasspath);

        // The plugin classloader is the new classloader's parent classloader.
        final ChildFirstURLClassLoader newClassLoader = new ChildFirstURLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
        if (weAreDebuggingADifferentVersionOfIdea(newClassLoader)) {
            // if we're debugging from another version of IDEA then child-first will do nasty things to the IDEA classpath
            Notifications.showWarning(project, message("plugin.debugging"));
            return new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
        }

        return newClassLoader;
    }

    private static List<URL> baseClasspathUrlsForPackagedPlugin(@NotNull final String classPathFromProps,
                                                                @NotNull final String basePath) {
        try {
            final List<URL> urls = new ArrayList<>();

            urls.add(new File(basePath, "checkstyle/classes").toURI().toURL());
            for (String jar : splitClassPathFromProperties(classPathFromProps)) {
                File jarLocation = new File(basePath, jar);
                if (!jarLocation.exists()) {
                    throw new CheckStylePluginException("Cannot find packaged artefact: " + jarLocation.getAbsolutePath());
                }
                urls.add(jarLocation.toURI().toURL());
            }

            return urls;

        } catch (MalformedURLException e) {
            throw new CheckStylePluginException("Failed to parse classpath URL", e);
        }
    }

    private List<URL> baseClasspathUrlsForIDEAUnitTests(@NotNull final String classPathFromProps) {
        try {
            final List<URL> urls = new ArrayList<>();
            final String buildPath = guessBuildPathFromClasspath();
            URL unitTestingClassPath = null;
            if (buildPath != null) {
                final File classesDir4UnitTesting = new File(buildPath, "classes/java/csaccess");
                if (classesDir4UnitTesting.exists()) {
                    unitTestingClassPath = classesDir4UnitTesting.toURI().toURL();
                }
            }

            if (unitTestingClassPath == null) {
                throw new CheckStylePluginException("Could not determine plugin directory or build directory");
            }

            urls.add(unitTestingClassPath);

            for (String jar : splitClassPathFromProperties(classPathFromProps)) {
                String testJarLocation = "tmp/gatherCheckstyleArtifacts" + jar.substring(jar.lastIndexOf('/'));
                File jarLocation = new File(buildPath, testJarLocation);
                if (!jarLocation.exists()) {
                    throw new CheckStylePluginException("Cannot find collected artefact: " + jarLocation.getAbsolutePath());
                }
                urls.add(jarLocation.toURI().toURL());
            }

            return urls;

        } catch (MalformedURLException e) {
            throw new CheckStylePluginException("Failed to parse classpath URL", e);
        }
    }

    @NotNull
    private static String[] splitClassPathFromProperties(@NotNull final String classPathFromProps) {
        return classPathFromProps.trim().split("\\s*;\\s*");
    }

    private boolean weAreDebuggingADifferentVersionOfIdea(final ClassLoader classLoaderToTest) {
        try {
            return Project.class != classLoaderToTest.loadClass("com.intellij.openapi.project.Project");
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @NotNull
    private List<URL> getUrls(@NotNull final ClassLoader sourceClassLoader) {
        List<URL> result;
        if (sourceClassLoader instanceof UrlClassLoader) {          // happens normally
            result = ((UrlClassLoader) sourceClassLoader).getUrls();
        } else if (sourceClassLoader instanceof URLClassLoader) {   // happens in test cases
            result = Arrays.asList(((URLClassLoader) sourceClassLoader).getURLs());
        } else {
            URL classResource = CheckstyleClassLoaderContainer.class.getResource("CheckstyleClassLoaderContainer.class");
            try {
                URL trimmedUrl = new URL(Objects.requireNonNull(classResource).toString().replaceFirst("org[/\\\\]infernus.*", ""));
                result = Collections.singletonList(trimmedUrl);
            } catch (MalformedURLException e) {
                result = Collections.singletonList(classResource);
            }
        }
        return result;
    }

    /**
     * Determine the base path of the plugin. When running in IntelliJ, this is something like
     * {@code C:/Users/jdoe/.IdeaIC2016.3/config/plugins/CheckStyle-IDEA} (on Windows); when running in Gradle,
     * it's probably the sandbox directory.
     *
     * @return the base path, as absolute path
     */
    @Nullable
    private String getBasePluginPath() {
        String result = getPluginPath();

        if (result == null) {
            result = getPreinstalledPluginPath();
        }

        return result;
    }

    @Nullable
    private String guessBuildPathFromClasspath() {
        final List<URL> urls = getUrls(getClass().getClassLoader());
        String result = guessFromClassPath(urls, CLASSES_URL_2017_2_LATER);
        if (result != null) {
            result += "/build";
        } else {
            result = guessFromClassPath(urls, CLASSES_URL_2017_1_EARLIER);
        }
        if (result == null) {
            result = guessFromClassPath(urls, CLASSES_URL_GRADLE);
        }
        if (result != null) {
            result = urlDecode(result);
        }
        return result;
    }

    @Nullable
    private String getPluginPath() {
        try {
            File pluginDir = new File(PathManager.getPluginsPath(), CheckStylePlugin.ID_PLUGIN);
            if (pluginDir.exists()) {
                return pluginDir.getAbsolutePath();
            }
        } catch (RuntimeException ignored) {
            // ok, if this fails, we are in a unit test situation where PathManager is not initialized, which is fine
        }
        return null;
    }

    @Nullable
    private String getPreinstalledPluginPath() {
        try {
            File preInstalledPluginDir = new File(PathManager.getPreInstalledPluginsPath(), CheckStylePlugin.ID_PLUGIN);
            if (preInstalledPluginDir.exists()) {
                return preInstalledPluginDir.getAbsolutePath();
            }
        } catch (RuntimeException ignored) {
            // ok, if this fails, we are in a unit test situation where PathManager is not initialized, which is fine
        }
        return null;
    }

    @Nullable
    private String guessFromClassPath(@NotNull final List<URL> urls, @NotNull final Pattern pattern) {
        String result = null;
        for (final URL url : urls) {
            Matcher matcher = pattern.matcher(url.getPath());
            if (matcher.find()) {
                result = matcher.group(1);
                break;
            }
        }
        return result;
    }

    @NotNull
    private String urlDecode(final String urlEncodedString) {
        return URLDecoder.decode(urlEncodedString, StandardCharsets.UTF_8);
    }

    @NotNull
    CheckstyleActions loadCheckstyleImpl() {
        try {
            Constructor<?> constructor = classLoader
                    .loadClass(CSACTIONS_CLASS)
                    .getConstructor(Project.class, CheckstyleProjectService.class);
            return (CheckstyleActions) constructor.newInstance(project, checkstyleProjectService);
        } catch (ReflectiveOperationException e) {
            throw new CheckStylePluginException("internal error", e);
        }
    }

    ClassLoader getClassLoader() {
        return classLoader;
    }
}
