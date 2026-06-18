package org.infernus.idea.checkstyle;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckstyleDownloadException;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
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
    public void nonBundledVersion_addsThirdPartyClasspath(@TempDir Path tempDir) throws Exception {
        Path fakeCheckstyleJar = tempDir.resolve("checkstyle-10.4.jar");
        fakeCheckstyleJar.toFile().createNewFile();
        Path thirdPartyJar = tempDir.resolve("third-party.jar");
        thirdPartyJar.toFile().createNewFile();

        CheckstyleArtifactDownloader mockDownloader = mock(CheckstyleArtifactDownloader.class);
        when(mockDownloader.download(NON_BUNDLED_VERSION)).thenReturn(List.of(fakeCheckstyleJar));

        CheckstyleProjectService serviceWithDownloader =
                new CheckstyleProjectService(project, mockDownloader);
        serviceWithDownloader.activateCheckstyleVersion(NON_BUNDLED_VERSION, List.of(thirdPartyJar.toString()));

        URLClassLoader classLoader = (URLClassLoader) serviceWithDownloader.underlyingClassLoader();
        assertThat(Arrays.asList(classLoader.getURLs()), hasItem(thirdPartyJar.toUri().toURL()));
    }

    @Test
    public void productionConstructorSetsNonNullDownloader() {
        assertNotNull(underTest.getDownloader());
    }

    @Test
    public void nonBundledVersion_downloadFailure_throwsDescriptiveException() {
        CheckstyleArtifactDownloader mockDownloader = mock(CheckstyleArtifactDownloader.class);
        when(mockDownloader.download(NON_BUNDLED_VERSION))
                .thenThrow(new CheckstyleDownloadException("connection refused"));

        CheckstyleProjectService service = new CheckstyleProjectService(project, mockDownloader);
        service.activateCheckstyleVersion(NON_BUNDLED_VERSION, null);

        CheckStylePluginException ex = assertThrows(CheckStylePluginException.class,
                service::getCheckstyleInstance);
        assertThat(ex.getMessage(), org.hamcrest.Matchers.startsWith("Failed to download Checkstyle " + NON_BUNDLED_VERSION));
        assertThat(ex.getMessage(), containsString("connection refused"));
    }

    @Test
    public void forVersionWithDownloader_exposesDownloaderViaGetter() {
        CheckstyleArtifactDownloader mockDownloader = mock(CheckstyleArtifactDownloader.class);

        CheckstyleProjectService service =
                CheckstyleProjectService.forVersion(project, BUNDLED_VERSION, null, mockDownloader);

        assertThat(service.getDownloader(), is(mockDownloader));
    }

    @Test
    public void latestVersion_loadsDefaultBundledVersion() throws ClassNotFoundException {
        underTest.activateCheckstyleVersion(VersionListReader.LATEST_VERSION, null);
        assertThat(underTest.underlyingClassLoader().loadClass("com.puppycrawl.tools.checkstyle.Checker"),
                is(not(nullValue())));
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
