package org.infernus.idea.checkstyle.checker;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceFilteringURLClassLoaderTest {
    private static final String FILTERED_FILE = "filteredFile.txt";
    private static final String PASSED_FILE = "passedFile.txt";

    private final ClassLoader parentClassLoader = mock(ClassLoader.class);
    private final ClassLoader underTest = new ResourceFilteringURLClassLoader(new URL[0], parentClassLoader, FILTERED_FILE);

    private URL passedUrl;

    @Before
    public void prepareParentClassLoader() throws IOException {
        passedUrl = new URL("file:///passed.file");

        when(parentClassLoader.getResource(FILTERED_FILE)).thenReturn(new URL("file:///filtered.file"));
        when(parentClassLoader.getResource(PASSED_FILE)).thenReturn(passedUrl);
        when(parentClassLoader.getResources(PASSED_FILE)).thenReturn(enumeration(List.of(passedUrl)));
    }

    @Test
    public void getResourceIgnoresTheSpecifiedResourceFile() {
        assertThat(underTest.getResource(FILTERED_FILE), nullValue());
    }

    @Test
    public void getResourceIgnoresTheSpecifiedResourceFileWhenAtTheEndOfAPath() {
        assertThat(underTest.getResource("/foobar/" + FILTERED_FILE), nullValue());
    }

    @Test
    public void getResourceHandlesWithTrailingSlashes() {
        assertThat(underTest.getResource("/foobar/"), nullValue());
    }

    @Test
    public void getResourcePassesOtherFiles() {
        assertThat(underTest.getResource(PASSED_FILE), equalTo(passedUrl));
    }

    @Test
    public void getResourcesIgnoresTheSpecifiedResourceFile() throws IOException {
        assertThat(underTest.getResources(FILTERED_FILE), equalTo(emptyEnumeration()));
    }

    @Test
    public void getResourcesPassesOtherFiles() throws IOException {
        Enumeration<URL> resources = underTest.getResources(PASSED_FILE);

        assertThat(resources.hasMoreElements(), equalTo(true));
        assertThat(resources.nextElement(), equalTo(passedUrl));
        assertThat(resources.hasMoreElements(), equalTo(false));
    }

    @Test
    public void resourcesIgnoresTheSpecifiedResourceFile() {
        Stream<URL> resources = underTest.resources(FILTERED_FILE);

        assertThat(resources.count(), equalTo(0L));
    }

    @Test
    public void resourcesPassesOtherFiles() {
        Stream<URL> resources = underTest.resources(PASSED_FILE);

        assertThat(resources.collect(Collectors.toList()), equalTo(List.of(passedUrl)));
    }


}
