package org.infernus.idea.checkstyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.openapi.project.Project;
import com.intellij.util.lang.UrlClassLoader;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.util.Strings;
import org.jetbrains.annotations.NotNull;


/**
 * Loads Checkstyle classes from a given Checkstyle version.
 */
public class CheckstyleClassLoader
{
    private static final String PROP_FILE = "checkstyle-classpaths.properties";

    private static final String CSACTIONS_CLASS = "org.infernus.idea.checkstyle.service.CheckstyleActionsImpl";

    /** <img src="doc-files/CheckstyleClassLoader-1.png"/> */
    private static final Pattern CLASSES_URL = Pattern.compile("^(.*?)[/\\\\]classes(?:[/\\\\]main)?[/\\\\]?$");

    private final ClassLoader classLoader;

    private final Project project;


    public CheckstyleClassLoader(@NotNull final Project pProject, @NotNull final String pCheckstyleVersion) {
        project = pProject;
        final Properties classPathInfos = loadClassPathInfos();
        final String cpProp = classPathInfos.getProperty(pCheckstyleVersion);
        if (Strings.isBlank(cpProp)) {
            throw new CheckStylePluginException("Unsupported Checkstyle version: " + pCheckstyleVersion);
        }
        classLoader = buildClassLoader(cpProp);
    }


    @NotNull
    public static Properties loadClassPathInfos() {
        final Properties result = new Properties();
        try (InputStream is = CheckstyleClassLoader.class.getClassLoader().getResourceAsStream(PROP_FILE)) {
            result.load(is);
        } catch (IOException e) {
            throw new CheckStylePluginException("Could not read plugin-internal file: " + PROP_FILE, e);
        }
        return result;
    }


    @NotNull
    private ClassLoader buildClassLoader(final String pClassPathFromProps) {
        final String basePath = getBasePath();
        final File classesDir4UnitTesting = new File(basePath, "classes/csaccess");
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
                    jar = "tmp/gatherCheckstyleArtifacts" + jar.substring(jar.lastIndexOf('/'));
                }
                urls.add(new File(basePath, jar).toURI().toURL());
            }
        } catch (MalformedURLException e) {
            throw new CheckStylePluginException("internal error", e);
        }
        // The plugin classloader is the new classloader's parent classloader.
        return new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
    }


    @NotNull
    private List<URL> getUrls(@NotNull final ClassLoader pClassLoader) {
        List<URL> result = null;
        if (pClassLoader instanceof UrlClassLoader) {          // happens normally
            result = ((UrlClassLoader) pClassLoader).getUrls();
        } else if (pClassLoader instanceof URLClassLoader) {   // happens in test cases
            result = Arrays.asList(((URLClassLoader) pClassLoader).getURLs());
        } else {
            throw new CheckStylePluginException("incompatible class loader: " + (pClassLoader != null ? pClassLoader
                    .getClass().getName() : "null"));
        }
        return result;
    }


    @NotNull
    private String getBasePath() {
        String result = null;

        for (final URL url : getUrls(getClass().getClassLoader())) {
            String path = url.getPath();
            Matcher matcher = CLASSES_URL.matcher(path);
            if (matcher.find()) {
                result = matcher.group(1);
                break;
            }
        }
        if (result == null) {
            throw new CheckStylePluginException("Could not determine plugin directory");
        }
        return result;
    }


    @NotNull
    public CheckstyleActions loadCheckstyleImpl() {
        try {
            Constructor<?> constructor = classLoader.loadClass(CSACTIONS_CLASS).getConstructor(Project.class);
            return (CheckstyleActions) constructor.newInstance(project);
        } catch (ReflectiveOperationException e) {
            throw new CheckStylePluginException("internal error", e);
        }
    }
}
