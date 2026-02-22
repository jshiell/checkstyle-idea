package org.infernus.idea.checkstyle.util;

import com.intellij.util.lang.UrlClassLoader;

import java.net.URL;
import java.net.URLClassLoader;

public final class ClassLoaderDumper {

    private ClassLoaderDumper() {
    }

    public static String dumpClassLoader(final ClassLoader classLoader) {
        StringBuilder dump = new StringBuilder();

        if (classLoader != null) {
            ClassLoader currentLoader = classLoader;
            while (currentLoader != null) {
                switch (currentLoader) {
                    case URLClassLoader urlClassLoader -> {
                        dump.append("URLClassLoader: ")
                                .append(currentLoader.getClass().getName())
                                .append('\n');
                        for (final URL url : urlClassLoader.getURLs()) {
                            dump.append("- url=").append(url).append('\n');
                        }
                    }
                    case UrlClassLoader urlClassLoader -> {
                        dump.append("UrlClassLoader: ")
                                .append(currentLoader.getClass().getName())
                                .append('\n');
                        for (final URL url : urlClassLoader.getUrls()) {
                            dump.append("- url=").append(url).append('\n');
                        }
                    }
                    default -> dump.append("ClassLoader: ").append(currentLoader.getClass().getName());
                }

                currentLoader = currentLoader.getParent();
            }
        }

        return dump.toString();
    }
}
