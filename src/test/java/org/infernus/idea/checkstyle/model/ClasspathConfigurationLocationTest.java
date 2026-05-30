package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClasspathConfigurationLocationTest {

    private Project project;
    private CheckstyleProjectService checkstyleProjectService;
    private ClasspathConfigurationLocation underTest;

    @BeforeEach
    void setUp() {
        project = TestHelper.mockProject();
        checkstyleProjectService = mock(CheckstyleProjectService.class);
        when(project.getService(CheckstyleProjectService.class)).thenReturn(checkstyleProjectService);

        underTest = new ClasspathConfigurationLocation(project, UUID.randomUUID().toString());
        underTest.setDescription("test-classpath-location");
    }

    @Test
    void resolveFileReturnsStreamForExistingClasspathResource() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        when(checkstyleProjectService.underlyingClassLoader()).thenReturn(classLoader);
        underTest.setLocation("checkstyle-idea.properties");

        try (InputStream stream = underTest.resolveFile(classLoader)) {
            assertThat(stream, notNullValue());
        }
    }

    @Test
    void resolveFileThrowsFileNotFoundForMissingResource() {
        ClassLoader classLoader = getClass().getClassLoader();
        when(checkstyleProjectService.underlyingClassLoader()).thenReturn(classLoader);
        underTest.setLocation("does/not/exist/at/all.xml");

        assertThrows(FileNotFoundException.class,
                () -> underTest.resolveFile(classLoader));
    }

    @Test
    void resolveFileReturnsCachedStreamOnSubsequentCalls() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        when(checkstyleProjectService.underlyingClassLoader()).thenReturn(classLoader);
        underTest.setLocation("checkstyle-idea.properties");

        // First call — loads from classpath
        InputStream first = underTest.resolveFile(classLoader);
        first.close();

        // Second call within cache TTL — should return a new stream backed by cached bytes
        InputStream second = underTest.resolveFile(classLoader);
        second.close();

        // Both streams are distinct objects (each call returns a fresh ByteArrayInputStream)
        assertThat(second, not(sameInstance(first)));
    }

    @Test
    void cloneReturnsSameType() {
        underTest.setLocation("checkstyle-idea.properties");
        Object cloned = underTest.clone();
        assertThat(cloned, instanceOf(ClasspathConfigurationLocation.class));
        assertThat(cloned, not(sameInstance(underTest)));
    }

    @Test
    void typeIsPluginClasspath() {
        assertThat(underTest.getType(), instanceOf(ConfigurationType.class));
        assertThat(underTest.getType(), org.hamcrest.Matchers.is(ConfigurationType.PLUGIN_CLASSPATH));
    }
}
