package org.infernus.idea.checkstyle.util;

import com.intellij.util.lang.UrlClassLoader;

import java.net.URL;
import java.net.URLClassLoader;

public class ClassLoaderDumper {

    public static String dumpClassLoader(final ClassLoader classLoader) {
        StringBuilder dump = new StringBuilder();

        if (classLoader != null) {
            ClassLoader currentLoader = classLoader;
            while (currentLoader != null) {
                if (currentLoader instanceof URLClassLoader) {
                    dump.append("URLClassLoader: ")
                            .append(currentLoader.getClass().getName())
                            .append('\n');
                    for (final URL url : ((URLClassLoader) currentLoader).getURLs()) {
                        dump.append("- url=").append(url).append('\n');
                    }
                } else if (currentLoader instanceof UrlClassLoader) {
                    dump.append("UrlClassLoader: ")
                            .append(currentLoader.getClass().getName())
                            .append('\n');
                    for (final URL url : ((UrlClassLoader) currentLoader).getUrls()) {
                        dump.append("- url=").append(url).append('\n');
                    }
                } else {
                    dump.append("ClassLoader: ").append(currentLoader.getClass().getName());
                }

                currentLoader = currentLoader.getParent();
            }
        }

        return dump.toString();
    }
}
