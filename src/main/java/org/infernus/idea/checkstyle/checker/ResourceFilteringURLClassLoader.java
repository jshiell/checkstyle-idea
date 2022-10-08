package org.infernus.idea.checkstyle.checker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Stream;

public class ResourceFilteringURLClassLoader extends URLClassLoader {
    private final String resourceNameToFilter;

    public ResourceFilteringURLClassLoader(@NotNull final URL[] urls,
                                           @NotNull final ClassLoader parent,
                                           @NotNull final String resourceNameToFilter) {
        super(urls, parent);

        this.resourceNameToFilter = resourceNameToFilter;
    }

    @Override
    protected URL findResource(final String moduleName,
                               final String name) throws IOException {
        if (resourceNameToFilter.equals(filenameOf(name))) {
            return null;
        }
        return super.findResource(moduleName, name);
    }

    @Nullable
    @Override
    public URL getResource(final String name) {
        if (resourceNameToFilter.equals(filenameOf(name))) {
            return null;
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        if (resourceNameToFilter.equals(filenameOf(name))) {
            return Collections.emptyEnumeration();
        }
        return super.getResources(name);
    }

    @Override
    public Stream<URL> resources(final String name) {
        if (resourceNameToFilter.equals(filenameOf(name))) {
            return Stream.empty();
        }
        return super.resources(name);
    }

    private String filenameOf(final String resource) {
        if (resource == null) {
            return null;
        }
        int lastSlashIndex = resource.lastIndexOf('/');
        if (lastSlashIndex >= 0) {
            return resource.substring(lastSlashIndex + 1);
        }
        return resource;
    }
}
