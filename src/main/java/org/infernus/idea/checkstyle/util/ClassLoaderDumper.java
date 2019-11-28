package org.infernus.idea.checkstyle.util;

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
                    final URLClassLoader urlLoader = (URLClassLoader) currentLoader;
                    for (final URL url : urlLoader.getURLs()) {
                        dump.append("- uri=").append(url).append('\n');
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
