package org.infernus.idea.checkstyle.util;

import java.net.URL;
import java.net.URLClassLoader;

import com.intellij.util.lang.UrlClassLoader;
import org.jetbrains.annotations.NotNull;


public final class Objects
{

    private Objects() {
    }

    // ideally we'd use commons-lang here, but I've learnt my lesson on relying on the IDEA CP

    public static boolean equals(final Object obj1, final Object obj2) {
        if (obj1 == obj2) {
            return true;
        } else if (obj1 == null || obj2 == null) {
            return false;
        }

        return obj1.equals(obj2);
    }


    public static <T extends Comparable<T>> int compare(final T obj1, final T obj2) {
        if (obj1 == null && obj2 == null) {
            return 0;
        } else if (obj1 == null) {
            return -1;
        } else if (obj2 == null) {
            return 1;
        }
        return obj1.compareTo(obj2);
    }


    public static String getClassPath(@NotNull final ClassLoader pClassLoader) {
        StringBuilder sb = new StringBuilder();
        sb.append("Class loader implementation: ");
        sb.append(pClassLoader.getClass().getName());
        sb.append(System.lineSeparator());

        sb.append("URLs:");
        sb.append(System.lineSeparator());
        if (pClassLoader instanceof UrlClassLoader) {
            final UrlClassLoader pluginClassLoader = (UrlClassLoader) pClassLoader;
            for (final URL url : pluginClassLoader.getUrls()) {
                sb.append("\t- ");
                sb.append(url);
                sb.append(System.lineSeparator());
            }
        } else if (pClassLoader instanceof URLClassLoader) {
            final URLClassLoader urlClassLoader = (URLClassLoader) pClassLoader;
            for (final URL url : urlClassLoader.getURLs()) {
                sb.append("\t- ");
                sb.append(url);
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }
}
