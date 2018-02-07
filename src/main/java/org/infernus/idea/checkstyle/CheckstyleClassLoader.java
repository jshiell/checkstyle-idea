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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.infernus.idea.checkstyle.CheckStyleBundle.message;
import static org.infernus.idea.checkstyle.util.Strings.isBlank;


/**
 * Loads Checkstyle classes from a given Checkstyle version.
 */
public class CheckstyleClassLoader {
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

    private final ClassLoader classLoader;

    private final Project project;
    private final CheckstyleProjectService checkstyleProjectService;

    public CheckstyleClassLoader(@NotNull final Project project,
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
    private List<URL> emptyListIfNull(@Nullable final List<URL> thirdPartyClassPath) {
        if (thirdPartyClassPath != null) {
            return thirdPartyClassPath;
        }
        return Collections.emptyList();
    }

    @NotNull
    private static Properties loadClassPathInfos() {
        final Properties result = new Properties();
        try (InputStream is = CheckstyleClassLoader.class.getClassLoader().getResourceAsStream(PROP_FILE)) {
            result.load(is);
        } catch (IOException e) {
            throw new CheckStylePluginException("Could not read plugin-internal file: " + PROP_FILE, e);
        }
        return result;
    }

    @NotNull
    private ClassLoader buildClassLoader(@NotNull final String pClassPathFromProps, @NotNull final List<URL>
            pThirdPartyClassPath) {
        final String basePath = getBasePath();
        final File classesDir4UnitTesting = new File(basePath, "classes/java/csaccess");
        final boolean unitTesting = classesDir4UnitTesting.exists();

        List<URL> urls = new ArrayList<>();
        try {
            if (unitTesting) {
                urls.add(classesDir4UnitTesting.toURI().toURL());
            } else {
                urls.add(new File(basePath, "checkstyle/classes").toURI().toURL());
            }
            for (String jar : pClassPathFromProps.trim().split("\\s*;\\s*")) {
                if (unitTesting) {
                    String testJarLocation = "tmp/gatherCheckstyleArtifacts" + jar.substring(jar.lastIndexOf('/'));
                    urls.add(new File(basePath, testJarLocation).toURI().toURL());
                } else {
                    urls.add(new File(basePath, jar).toURI().toURL());
                }
            }
        } catch (MalformedURLException e) {
            throw new CheckStylePluginException("internal error", e);
        }

        urls.addAll(pThirdPartyClassPath);

        // The plugin classloader is the new classloader's parent classloader.
        final ChildFirstURLClassLoader newClassLoader = new ChildFirstURLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
        if (weAreDebuggingADifferentVersionOfIdea(newClassLoader)) {
            // if we're debugging from another version of IDEA then child-first will do nasty things to the IDEA classpath
            Notifications.showWarning(project, message("plugin.debugging"));
            return new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
        }
        return newClassLoader;
    }

    private boolean weAreDebuggingADifferentVersionOfIdea(final ClassLoader classLoaderToTest) {
        try {
            return Project.class != classLoaderToTest.loadClass("com.intellij.openapi.project.Project");
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @NotNull
    private List<URL> getUrls(@NotNull final ClassLoader pClassLoader) {
        List<URL> result;
        if (pClassLoader instanceof UrlClassLoader) {          // happens normally
            result = ((UrlClassLoader) pClassLoader).getUrls();
        } else if (pClassLoader instanceof URLClassLoader) {   // happens in test cases
            result = Arrays.asList(((URLClassLoader) pClassLoader).getURLs());
        } else {
            //noinspection ConstantConditions
            throw new CheckStylePluginException("incompatible class loader: "
                    + (pClassLoader != null ? pClassLoader.getClass().getName() : "null"));
        }
        return result;
    }

    /**
     * Determine the base path of the plugin. When running in IntelliJ, this is something like
     * {@code C:/Users/jdoe/.IdeaIC2016.3/config/plugins/CheckStyle-IDEA} (on Windows). When running in a unit test,
     * it is this project's build directory, for example {@code D:/Documents/Projects/checkstyle-idea/build} (again
     * on Windows).
     *
     * @return the base path, as absolute path
     */
    @NotNull
    private String getBasePath() {
        String result = getPluginPath();

        if (result == null) {
            result = getPreinstalledPluginPath();
        }

        if (result == null) {
            result = guessPluginPathFromClasspath();
        }

        if (result == null) {
            throw new CheckStylePluginException("Could not determine plugin directory");
        }
        return result;
    }

    @Nullable
    private String guessPluginPathFromClasspath() {
        final List<URL> urls = getUrls(getClass().getClassLoader());
        String result = guessFromClassPath(urls, CLASSES_URL_2017_2_LATER);
        if (result != null) {
            result += "/build";
        } else {
            result = guessFromClassPath(urls, CLASSES_URL_2017_1_EARLIER);
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
    private String guessFromClassPath(@NotNull final List<URL> pUrls, @NotNull final Pattern pPattern) {
        String result = null;
        for (final URL url : pUrls) {
            Matcher matcher = pPattern.matcher(url.getPath());
            if (matcher.find()) {
                result = matcher.group(1);
                break;
            }
        }
        return result;
    }


    @NotNull
    private String urlDecode(final String urlEncodedString) {
        try {
            return URLDecoder.decode(urlEncodedString, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            return urlEncodedString;
        }
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
}
