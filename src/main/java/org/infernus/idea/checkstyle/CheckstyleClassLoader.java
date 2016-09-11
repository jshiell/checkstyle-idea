package org.infernus.idea.checkstyle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.ide.plugins.cl.PluginClassLoader;
import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.util.Strings;
import org.jetbrains.annotations.NotNull;


/**
 * Loads Checkstyle classes from a given Checkstyle version.
 */
public class CheckstyleClassLoader
{
    private static final String PROP_FILE = "checkstyle-classpaths.properties";

    /** <img src="doc-files/CheckstyleClassLoader-1.png"/> */
    private static final Pattern CLASSES_URL = Pattern.compile("^(.*?)[/\\\\]classes[/\\\\]?$");

    private final ClassLoader classLoader;


    public CheckstyleClassLoader(final String pCheckstyleVersion) {
        final Properties classPathInfos = loadClassPathInfos();
        final String cpProp = classPathInfos.getProperty(pCheckstyleVersion);
        if (Strings.isBlank(cpProp)) {
            throw new CheckStylePluginException("Unsupported Checkstyle version: " + pCheckstyleVersion);
        }
        classLoader = buildClassLoader(cpProp);
    }


    @NotNull
    private Properties loadClassPathInfos() {
        final Properties result = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(PROP_FILE)) {
            result.load(is);
        } catch (IOException e) {
            throw new CheckStylePluginException("Could not read plugin-internal file: " + PROP_FILE, e);
        }
        return result;
    }


    @NotNull
    private ClassLoader buildClassLoader(final String pClassPathFromProps) {
        final String basePath = getBasePath();
        List<URL> urls = new ArrayList<>();
        try {
            urls.add(new File(basePath, "checkstyle/classes").toURI().toURL());
            for (String jar : pClassPathFromProps.trim().split("\\s*;\\s*")) {
                urls.add(new File(basePath, jar).toURI().toURL());
            }
        } catch (MalformedURLException e) {
            throw new CheckStylePluginException("internal error", e);
        }
        // The plugin classloader is the new classloader's parent classloader.
        return new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
    }


    @NotNull
    private String getBasePath() {
        String result = null;
        for (URL url : ((PluginClassLoader) getClass().getClassLoader()).getUrls()) {
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
            return (CheckstyleActions) classLoader.loadClass(
                    "org.infernus.idea.checkstyle.service.CheckstyleActionsImpl").newInstance();
        } catch (ReflectiveOperationException e) {
            throw new CheckStylePluginException("internal error", e);
        }
    }
}
