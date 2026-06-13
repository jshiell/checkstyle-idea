package org.infernus.idea.checkstyle;

import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;


public class CheckstyleClassLoaderContainerTest {

    @Test
    void constructorWithDownloadedJars_acceptsEmptyList(@TempDir Path tempDir) {
        var project = mock(com.intellij.openapi.project.Project.class);
        var service = mock(CheckstyleProjectService.class);

        var container = new CheckstyleClassLoaderContainer(project, service, List.of());

        assertNotNull(container.getClassLoader());
    }

    @Test
    void constructorWithDownloadedJars_acceptsRealJarPaths(@TempDir Path tempDir) throws Exception {
        var jarPath = tempDir.resolve("fake.jar");
        jarPath.toFile().createNewFile();

        var project = mock(com.intellij.openapi.project.Project.class);
        var service = mock(CheckstyleProjectService.class);

        var container = new CheckstyleClassLoaderContainer(project, service, List.of(jarPath));

        assertNotNull(container.getClassLoader());
    }

    @Test
    void constructorWithDownloadedJars_doesNotRequireClasspathsProperties(@TempDir Path tempDir) {
        var project = mock(com.intellij.openapi.project.Project.class);
        var service = mock(CheckstyleProjectService.class);

        assertDoesNotThrow(() -> new CheckstyleClassLoaderContainer(project, service, List.of()));
    }

    @Test
    void constructorWithDownloadedJars_classLoaderCanFindCheckstyleActionsImpl() throws ClassNotFoundException {
        var project = mock(com.intellij.openapi.project.Project.class);
        var service = mock(CheckstyleProjectService.class);

        var container = new CheckstyleClassLoaderContainer(project, service, List.of());

        assertNotNull(container.getClassLoader().loadClass("org.infernus.idea.checkstyle.service.CheckstyleActionsImpl"));
    }

    @Test
    void originalConstructor_throwsForUnbundledVersion() {
        var project = mock(com.intellij.openapi.project.Project.class);
        var service = mock(CheckstyleProjectService.class);

        assertThrows(CheckStylePluginException.class,
                () -> new CheckstyleClassLoaderContainer(project, service, "10.26.1", null));
    }
}
