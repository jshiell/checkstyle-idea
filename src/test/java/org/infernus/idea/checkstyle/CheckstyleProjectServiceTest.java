package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.SortedSet;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;


public class CheckstyleProjectServiceTest {
    private static final String BUNDLED_VERSION = "10.0";
    private static final String NON_BUNDLED_VERSION = "10.4";

    private Project project;
    private CheckstyleProjectService underTest;

    @BeforeEach
    public void setUp() {
        project = mock(Project.class);

        PluginConfigurationManager pluginConfigManager = mock(PluginConfigurationManager.class);
        when(pluginConfigManager.getCurrent())
                .thenReturn(PluginConfigurationBuilder.testInstance(BUNDLED_VERSION).build());
        when(project.getService(PluginConfigurationManager.class)).thenReturn(pluginConfigManager);

        underTest = new CheckstyleProjectService(project);
    }

    @Test
    public void readingSupportedVersionsReturnsASetOfVersions() {
        SortedSet<String> versions = underTest.getSupportedVersions();
        assertThat(versions, hasItem(BUNDLED_VERSION));
        assertThat(versions.comparator(), is(instanceOf(VersionComparator.class)));
    }

    @Test
    public void classLoaderCanBeRetrievedByExternalTools() {
        underTest.activateCheckstyleVersion(BUNDLED_VERSION, null);
        assertThat(underTest.underlyingClassLoader(), is(not(nullValue())));
    }

    @Test
    public void classLoaderCanLoadCheckStyleInternalClasses() throws ClassNotFoundException {
        underTest.activateCheckstyleVersion(BUNDLED_VERSION, null);
        assertThat(underTest.underlyingClassLoader().loadClass("com.puppycrawl.tools.checkstyle.Checker"),
                is(not(nullValue())));
    }

    @Test
    public void nonBundledVersion_usesDownloadedPaths(@TempDir Path tempDir) throws Exception {
        Path fakeJar = tempDir.resolve("checkstyle-10.4.jar");
        fakeJar.toFile().createNewFile();

        CheckstyleArtifactDownloader mockDownloader = mock(CheckstyleArtifactDownloader.class);
        when(mockDownloader.download(NON_BUNDLED_VERSION)).thenReturn(List.of(fakeJar));

        CheckstyleProjectService serviceWithDownloader =
                new CheckstyleProjectService(project, mockDownloader);
        serviceWithDownloader.activateCheckstyleVersion(NON_BUNDLED_VERSION, null);

        assertNotNull(serviceWithDownloader.underlyingClassLoader());
        verify(mockDownloader).download(NON_BUNDLED_VERSION);
    }

    @Test
    public void bundledVersion_doesNotUseDownloader() {
        CheckstyleArtifactDownloader mockDownloader = mock(CheckstyleArtifactDownloader.class);

        CheckstyleProjectService serviceWithDownloader =
                new CheckstyleProjectService(project, mockDownloader);
        serviceWithDownloader.activateCheckstyleVersion(BUNDLED_VERSION, null);
        serviceWithDownloader.underlyingClassLoader();

        verifyNoInteractions(mockDownloader);
    }
}
