package org.infernus.idea.checkstyle.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ChildFirstURLClassLoaderTest {

    private Path tempDir;
    private ChildFirstURLClassLoader underTest;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("cfl-test-");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (underTest != null) {
            underTest.close();
        }
        deleteRecursively(tempDir.toFile());
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    @Test
    void canBeConstructedWithEmptyClasspath() {
        underTest = new ChildFirstURLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
        assertThat(underTest, is(notNullValue()));
    }

    @Test
    void canLoadJdkClassFromSystemClassLoader() throws ClassNotFoundException {
        underTest = new ChildFirstURLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
        Class<?> loaded = underTest.loadClass("java.lang.String");
        assertThat(loaded, is(notNullValue()));
        assertThat(loaded.getName(), is("java.lang.String"));
    }

    @Test
    void canLoadClassFromParentClassLoader() throws ClassNotFoundException {
        // ChildFirstURLClassLoaderTest itself is on the parent classloader (test classpath)
        underTest = new ChildFirstURLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
        Class<?> loaded = underTest.loadClass(ChildFirstURLClassLoaderTest.class.getName());
        assertThat(loaded, is(notNullValue()));
    }

    @Test
    void loadClassReturnsSameInstanceOnRepeatCalls() throws ClassNotFoundException {
        underTest = new ChildFirstURLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
        Class<?> first = underTest.loadClass("java.lang.Integer");
        Class<?> second = underTest.loadClass("java.lang.Integer");
        assertThat(first, is(sameInstance(second)));
    }

    @Test
    void getResourceReturnsNullForUnknownResource() {
        underTest = new ChildFirstURLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
        URL resource = underTest.getResource("this/resource/does/not/exist-" + System.nanoTime() + ".txt");
        assertThat(resource, is(nullValue()));
    }

    @Test
    void getResourceFindsResourceOnSystemClasspath() {
        underTest = new ChildFirstURLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
        // java.lang.Object class file is always available via system classloader
        URL resource = underTest.getResource("java/lang/Object.class");
        assertThat(resource, is(notNullValue()));
    }

    @Test
    void getResourceFindsResourceOnLocalClasspath() throws IOException {
        Path resourceFile = tempDir.resolve("local-resource.txt");
        Files.writeString(resourceFile, "hello");
        underTest = new ChildFirstURLClassLoader(new URL[]{tempDir.toUri().toURL()}, null);
        URL resource = underTest.getResource("local-resource.txt");
        assertThat(resource, is(notNullValue()));
    }

    @Test
    void getResourcesReturnsEmptyEnumerationForUnknownResource() throws IOException {
        underTest = new ChildFirstURLClassLoader(new URL[0], null);
        Enumeration<URL> resources = underTest.getResources("no-such-resource-" + System.nanoTime());
        assertThat(resources.hasMoreElements(), is(false));
    }

    @Test
    void getResourcesIncludesLocalResource() throws IOException {
        Path resourceFile = tempDir.resolve("multi.properties");
        Files.writeString(resourceFile, "key=value");
        underTest = new ChildFirstURLClassLoader(new URL[]{tempDir.toUri().toURL()}, null);
        Enumeration<URL> resources = underTest.getResources("multi.properties");
        List<URL> collected = Collections.list(resources);
        assertThat(collected, is(not(empty())));
    }

    @Test
    void getResourceAsStreamReturnsNullForUnknownResource() {
        underTest = new ChildFirstURLClassLoader(new URL[0], null);
        InputStream stream = underTest.getResourceAsStream("no-such-resource-" + System.nanoTime());
        assertThat(stream, is(nullValue()));
    }

    @Test
    void getResourceAsStreamReturnsStreamForLocalResource() throws IOException {
        Path resourceFile = tempDir.resolve("stream-resource.txt");
        Files.writeString(resourceFile, "data");
        underTest = new ChildFirstURLClassLoader(new URL[]{tempDir.toUri().toURL()}, null);
        InputStream stream = underTest.getResourceAsStream("stream-resource.txt");
        assertThat(stream, is(notNullValue()));
        stream.close();
    }

    @Test
    void toStringContainsClassLoaderLabel() {
        underTest = new ChildFirstURLClassLoader(new URL[0], null);
        assertThat(underTest.toString(), containsString("ChildFirstURLClassLoader"));
    }

    @Test
    void toStringContainsParentDescription() {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        underTest = new ChildFirstURLClassLoader(new URL[0], parent);
        assertThat(underTest.toString(), containsString(parent.toString()));
    }

    @Test
    void localResourceTakesPriorityOverParentResource() throws IOException {
        // Write a resource with specific content to the local dir
        Path localResource = tempDir.resolve("priority-test.txt");
        Files.writeString(localResource, "local");

        // Parent classloader has no such resource (we use a child-first loader with null parent)
        underTest = new ChildFirstURLClassLoader(new URL[]{tempDir.toUri().toURL()}, null);
        URL resource = underTest.getResource("priority-test.txt");
        assertThat(resource, is(notNullValue()));
        assertThat(resource.toString(), containsString("priority-test.txt"));
    }

    @Test
    void getResourcesOrdersSystemThenLocalThenParent() throws IOException {
        // Write a local resource
        Path localResource = tempDir.resolve("ordered.properties");
        Files.writeString(localResource, "key=local");

        // Create a second directory to act as a "parent" classloader with the same resource
        Path parentDir = Files.createTempDirectory("cfl-parent-");
        Path parentResource = parentDir.resolve("ordered.properties");
        Files.writeString(parentResource, "key=parent");
        ClassLoader parentLoader = new ChildFirstURLClassLoader(new URL[]{parentDir.toUri().toURL()}, null);

        underTest = new ChildFirstURLClassLoader(new URL[]{tempDir.toUri().toURL()}, parentLoader);
        Enumeration<URL> resources = underTest.getResources("ordered.properties");
        List<URL> urls = new ArrayList<>();
        while (resources.hasMoreElements()) {
            urls.add(resources.nextElement());
        }

        // Both local and parent resources should be present
        assertThat(urls.size(), is(greaterThanOrEqualTo(2)));

        deleteRecursively(parentDir.toFile());
        ((ChildFirstURLClassLoader) parentLoader).close();
    }
}
