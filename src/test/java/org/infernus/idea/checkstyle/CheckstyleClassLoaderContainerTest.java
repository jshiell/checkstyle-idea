package org.infernus.idea.checkstyle;

import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;


public class CheckstyleClassLoaderContainerTest {

    @Test
    void constructorWithDownloadedJarsAcceptsEmptyList() {
        var project = mock(com.intellij.openapi.project.Project.class);
        var service = mock(CheckstyleProjectService.class);

        var container = new CheckstyleClassLoaderContainer(project, service, List.of());

        assertNotNull(container.getClassLoader());
    }

    @Test
    void constructorWithDownloadedJarsAcceptsRealJarPaths(@TempDir final Path tempDir) throws Exception {
        var jarPath = tempDir.resolve("fake.jar");
        jarPath.toFile().createNewFile();

        var project = mock(com.intellij.openapi.project.Project.class);
        var service = mock(CheckstyleProjectService.class);

        var container = new CheckstyleClassLoaderContainer(project, service, List.of(jarPath));

        assertNotNull(container.getClassLoader());
    }

    @Test
    void constructorWithDownloadedJarsDoesNotRequireClasspathsProperties() {
        var project = mock(com.intellij.openapi.project.Project.class);
        var service = mock(CheckstyleProjectService.class);

        assertDoesNotThrow(() -> new CheckstyleClassLoaderContainer(project, service, List.of()));
    }

    @Test
    void constructorWithDownloadedJarsClassLoaderCanFindCheckstyleActionsImpl() throws ClassNotFoundException {
        var project = mock(com.intellij.openapi.project.Project.class);
        var service = mock(CheckstyleProjectService.class);

        var container = new CheckstyleClassLoaderContainer(project, service, List.of());

        assertNotNull(container.getClassLoader().loadClass("org.infernus.idea.checkstyle.service.CheckstyleActionsImpl"));
    }

    @Test
    void originalConstructorThrowsForUnbundledVersion() {
        var project = mock(com.intellij.openapi.project.Project.class);
        var service = mock(CheckstyleProjectService.class);

        assertThrows(CheckStylePluginException.class,
                () -> new CheckstyleClassLoaderContainer(project, service, "10.26.1", null));
    }

    @Test
    void closeReleasesTheUnderlyingJarHandles(@TempDir final Path tempDir) throws Exception {
        var jarPath = tempDir.resolve("real.jar");
        try (var jarOut = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            jarOut.putNextEntry(new ZipEntry("marker.txt"));
            jarOut.write("marker".getBytes());
            jarOut.closeEntry();
        }

        var project = mock(com.intellij.openapi.project.Project.class);
        var service = mock(CheckstyleProjectService.class);
        var container = new CheckstyleClassLoaderContainer(project, service, List.of(jarPath));

        assertNotNull(container.getClassLoader().getResource("marker.txt"));

        container.close();

        assertNull(container.getClassLoader().getResource("marker.txt"));
    }

    @Test
    void closeIsIdempotent(@TempDir final Path tempDir) {
        var jarPath = tempDir.resolve("fake.jar");
        assertDoesNotThrow(() -> jarPath.toFile().createNewFile());

        var project = mock(com.intellij.openapi.project.Project.class);
        var service = mock(CheckstyleProjectService.class);
        var container = new CheckstyleClassLoaderContainer(project, service, List.of(jarPath));

        assertDoesNotThrow(container::close);
        assertDoesNotThrow(container::close);
    }
}
