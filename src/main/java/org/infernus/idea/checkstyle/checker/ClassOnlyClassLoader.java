package org.infernus.idea.checkstyle.checker;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

/**
 * This is a URLClassLoader that doesn't allow loading of resources, to avoid
 * polluting the CheckStyle classpath with more than is required.
 */
public class ClassOnlyClassLoader extends URLClassLoader {

    public ClassOnlyClassLoader(final URL[] urls, final ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public URL findResource(final String name) {
        return null;
    }

    @Override
    public Enumeration<URL> findResources(final String name) throws IOException {
        return null;
    }

}
