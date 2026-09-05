package org.infernus.idea.checkstyle;

import com.intellij.ide.trustedProjects.TrustedProjects;
import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.util.TempDirProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.quality.Strictness;

import org.infernus.idea.checkstyle.exception.CheckStylePluginException;
import org.infernus.idea.checkstyle.exception.CheckstyleDownloadException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class CheckstyleProjectServiceTest {
    private static final String BUNDLED_VERSION = "14.1.0";
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
        when(project.getServiceIfCreated(org.infernus.idea.checkstyle.checker.CheckerFactoryCache.class))
                .thenReturn(new org.infernus.idea.checkstyle.checker.CheckerFactoryCache());

        underTest = new CheckstyleProjectService(project);
    }

    /**
     * Third-party classpath entries are only loaded for trusted projects, so every test that expects a
     * non-empty third-party classpath to reach the classloader has to say so explicitly. Lenient, because
     * a test may build no classloader at all on some paths and never exercise the stub.
     */
    private static MockedStatic<TrustedProjects> aTrustedProject(final Project trustedProject) {
        MockedStatic<TrustedProjects> trustedProjects =
                mockStatic(TrustedProjects.class, withSettings().strictness(Strictness.LENIENT));
        trustedProjects.when(() -> TrustedProjects.isProjectTrusted(trustedProject)).thenReturn(true);
        return trustedProjects;
    }

    private static MockedStatic<TrustedProjects> anUntrustedProject(final Project untrustedProject) {
        MockedStatic<TrustedProjects> trustedProjects =
                mockStatic(TrustedProjects.class, withSettings().strictness(Strictness.LENIENT));
        trustedProjects.when(() -> TrustedProjects.isProjectTrusted(untrustedProject)).thenReturn(false);
        return trustedProjects;
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
    public void nonBundledVersionUsesDownloadedPaths(@TempDir final Path tempDir) throws Exception {
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
    public void nonBundledVersionAddsThirdPartyClasspath(@TempDir final Path tempDir) throws Exception {
        Path fakeCheckstyleJar = tempDir.resolve("checkstyle-10.4.jar");
        fakeCheckstyleJar.toFile().createNewFile();
        Path thirdPartyJar = tempDir.resolve("third-party.jar");
        thirdPartyJar.toFile().createNewFile();

        CheckstyleArtifactDownloader mockDownloader = mock(CheckstyleArtifactDownloader.class);
        when(mockDownloader.download(NON_BUNDLED_VERSION)).thenReturn(List.of(fakeCheckstyleJar));

        CheckstyleProjectService serviceWithDownloader =
                new CheckstyleProjectService(project, mockDownloader);
        serviceWithDownloader.activateCheckstyleVersion(NON_BUNDLED_VERSION, List.of(thirdPartyJar.toString()));

        try (MockedStatic<TrustedProjects> ignored = aTrustedProject(project)) {
            URLClassLoader classLoader = (URLClassLoader) serviceWithDownloader.underlyingClassLoader();
            assertThat(Arrays.asList(classLoader.getURLs()), hasItem(thirdPartyJar.toUri().toURL()));
        }
    }

    @Test
    public void untrustedProjectExcludesThirdPartyJarsButStillLoadsCheckstyle(@TempDir final Path tempDir)
            throws Exception {
        Path thirdPartyJar = tempDir.resolve("third-party.jar");
        thirdPartyJar.toFile().createNewFile();

        underTest.activateCheckstyleVersion(BUNDLED_VERSION, List.of(thirdPartyJar.toString()));

        try (MockedStatic<TrustedProjects> ignored = anUntrustedProject(project)) {
            URLClassLoader classLoader = (URLClassLoader) underTest.underlyingClassLoader();

            assertThat(Arrays.asList(classLoader.getURLs()), not(hasItem(thirdPartyJar.toUri().toURL())));
            assertThat(classLoader.loadClass("com.puppycrawl.tools.checkstyle.Checker"), is(not(nullValue())));
        }
    }

    @Test
    public void untrustedProjectNeverFetchesAUrlThirdPartyClasspathEntry(@TempDir final Path tempDir) {
        final AtomicBoolean fetched = new AtomicBoolean(false);
        final ThirdPartyJarCache thirdPartyJarCache = new ThirdPartyJarCache(tempDir.resolve("third-party-jars"),
                (url, target) -> {
                    fetched.set(true);
                    throw new IOException("an untrusted project must not reach the network");
                });

        final CheckstyleProjectService service =
                new CheckstyleProjectService(project, mock(CheckstyleArtifactDownloader.class), thirdPartyJarCache);
        service.activateCheckstyleVersion(BUNDLED_VERSION, List.of("https://example.invalid/custom-check.jar"));

        try (MockedStatic<TrustedProjects> ignored = anUntrustedProject(project)) {
            service.underlyingClassLoader();
        }

        assertFalse(fetched.get(), "an untrusted project must not download third-party check JARs");
    }

    @Test
    public void urlThirdPartyClasspathEntryResolvesThroughTheCache(@TempDir final Path tempDir) throws Exception {
        final Path fakeCheckstyleJar = tempDir.resolve("checkstyle-10.4.jar");
        fakeCheckstyleJar.toFile().createNewFile();

        final CheckstyleArtifactDownloader mockDownloader = mock(CheckstyleArtifactDownloader.class);
        when(mockDownloader.download(NON_BUNDLED_VERSION)).thenReturn(List.of(fakeCheckstyleJar));

        final Path cacheRoot = tempDir.resolve("third-party-jars");
        final byte[] jarBytes = validZipBytes();
        final ThirdPartyJarCache thirdPartyJarCache = new ThirdPartyJarCache(cacheRoot,
                (url, target) -> Files.write(target, jarBytes));

        final CheckstyleProjectService serviceWithDownloader =
                new CheckstyleProjectService(project, mockDownloader, thirdPartyJarCache);
        serviceWithDownloader.activateCheckstyleVersion(NON_BUNDLED_VERSION,
                List.of("https://example.invalid/custom-check.jar"));

        try (MockedStatic<TrustedProjects> ignored = aTrustedProject(project)) {
            final URLClassLoader classLoader = (URLClassLoader) serviceWithDownloader.underlyingClassLoader();
            final Path resolvedCacheFile = thirdPartyJarCache.resolve("https://example.invalid/custom-check.jar");
            assertThat(Arrays.asList(classLoader.getURLs()), hasItem(resolvedCacheFile.toUri().toURL()));
        }
    }

    @Test
    public void urlThirdPartyClasspathEntryDownloadFailureBlocksActivationWithADescriptiveErrorEvenWhenOtherEntriesWouldResolve(
            @TempDir final Path tempDir) throws Exception {
        final Path fakeCheckstyleJar = tempDir.resolve("checkstyle-10.4.jar");
        fakeCheckstyleJar.toFile().createNewFile();
        final Path otherThirdPartyJar = tempDir.resolve("other-third-party.jar");
        otherThirdPartyJar.toFile().createNewFile();

        final CheckstyleArtifactDownloader mockDownloader = mock(CheckstyleArtifactDownloader.class);
        when(mockDownloader.download(NON_BUNDLED_VERSION)).thenReturn(List.of(fakeCheckstyleJar));

        final Path cacheRoot = tempDir.resolve("third-party-jars");
        final ThirdPartyJarCache thirdPartyJarCache = new ThirdPartyJarCache(cacheRoot, (url, target) -> {
            throw new IOException("connection refused");
        });

        final String failingUrl = "https://example.invalid/custom-check.jar";
        final CheckstyleProjectService serviceWithDownloader =
                new CheckstyleProjectService(project, mockDownloader, thirdPartyJarCache);
        serviceWithDownloader.activateCheckstyleVersion(NON_BUNDLED_VERSION,
                List.of(otherThirdPartyJar.toString(), failingUrl));

        try (MockedStatic<TrustedProjects> ignored = aTrustedProject(project)) {
            final CheckStylePluginException ex = assertThrows(CheckStylePluginException.class,
                    serviceWithDownloader::underlyingClassLoader);
            assertThat(ex.getMessage(), containsString(failingUrl));
        }
    }

    private static byte[] validZipBytes() throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("entry.txt"));
            zip.write("content".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    @Test
    public void productionConstructorSetsNonNullDownloader() {
        assertNotNull(underTest.getDownloader());
    }

    @Test
    public void nonBundledVersionDownloadFailureThrowsDescriptiveException() {
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
    public void forVersionWithDownloaderExposesDownloaderViaGetter() {
        CheckstyleArtifactDownloader mockDownloader = mock(CheckstyleArtifactDownloader.class);

        CheckstyleProjectService service =
                CheckstyleProjectService.forVersion(project, BUNDLED_VERSION, null, mockDownloader);

        assertThat(service.getDownloader(), is(mockDownloader));
    }

    @Test
    public void latestVersionLoadsDefaultBundledVersion() throws ClassNotFoundException {
        underTest.activateCheckstyleVersion(VersionListReader.LATEST_VERSION, null);
        assertThat(underTest.underlyingClassLoader().loadClass("com.puppycrawl.tools.checkstyle.Checker"),
                is(not(nullValue())));
    }

    @Test
    public void bundledVersionDoesNotUseDownloader() {
        CheckstyleArtifactDownloader mockDownloader = mock(CheckstyleArtifactDownloader.class);

        CheckstyleProjectService serviceWithDownloader =
                new CheckstyleProjectService(project, mockDownloader);
        serviceWithDownloader.activateCheckstyleVersion(BUNDLED_VERSION, null);
        serviceWithDownloader.underlyingClassLoader();

        verifyNoInteractions(mockDownloader);
    }

    @Test
    public void copyLibsEnabledStabilizesThirdPartyJarsFromProjectDir(@TempDir final Path projectDir,
                                                                      @TempDir final Path copyDir) throws Exception {
        Path thirdPartyJar = projectDir.resolve("ext.jar");
        thirdPartyJar.toFile().createNewFile();

        when(project.getBasePath()).thenReturn(projectDir.toString());
        when(project.getLocationHash()).thenReturn("test-project");

        PluginConfigurationManager copyLibsManager = mock(PluginConfigurationManager.class);
        when(copyLibsManager.getCurrent())
                .thenReturn(PluginConfigurationBuilder.testInstance(BUNDLED_VERSION).withCopyLibraries(true).build());
        when(project.getService(PluginConfigurationManager.class)).thenReturn(copyLibsManager);

        TempDirProvider tempDirProvider = new TempDirProvider() {
            @Override
            public Optional<File> forCopiedLibraries(@NotNull final Project p) {
                return Optional.of(copyDir.toFile());
            }
        };

        underTest = new CheckstyleProjectService(project, tempDirProvider);
        underTest.activateCheckstyleVersion(BUNDLED_VERSION, List.of(thirdPartyJar.toString()));

        try (MockedStatic<TrustedProjects> ignored = aTrustedProject(project)) {
            URLClassLoader classLoader = (URLClassLoader) underTest.underlyingClassLoader();
            URL originalUrl = thirdPartyJar.toUri().toURL();
            URL copiedUrl = copyDir.resolve("ext.jar").toUri().toURL();
            assertThat(Arrays.asList(classLoader.getURLs()), allOf(not(hasItem(originalUrl)), hasItem(copiedUrl)));
        }
    }

    @Test
    public void disposeClosesTheContainerSoANewOneIsBuiltOnNextAccess() {
        underTest.activateCheckstyleVersion(BUNDLED_VERSION, null);
        ClassLoader before = underTest.underlyingClassLoader();

        underTest.dispose();

        ClassLoader after = underTest.underlyingClassLoader();
        assertNotSame(before, after);
    }

    @Test
    public void disposeInvalidatesTheProjectSharedCheckerCacheForTheRegisteredProjectService() {
        org.infernus.idea.checkstyle.checker.CheckerFactoryCache cache =
                mock(org.infernus.idea.checkstyle.checker.CheckerFactoryCache.class);
        when(project.getServiceIfCreated(org.infernus.idea.checkstyle.checker.CheckerFactoryCache.class)).thenReturn(cache);

        underTest.activateCheckstyleVersion(BUNDLED_VERSION, null);
        underTest.underlyingClassLoader();

        underTest.dispose();

        verify(cache).invalidate();
    }

    @Test
    public void disposeDoesNotTouchTheProjectSharedCacheForAThrowawayInstance() {
        org.infernus.idea.checkstyle.checker.CheckerFactoryCache cache =
                mock(org.infernus.idea.checkstyle.checker.CheckerFactoryCache.class);
        when(project.getServiceIfCreated(org.infernus.idea.checkstyle.checker.CheckerFactoryCache.class)).thenReturn(cache);

        CheckstyleProjectService throwaway = CheckstyleProjectService.forVersion(project, BUNDLED_VERSION, null);
        throwaway.underlyingClassLoader();

        throwaway.dispose();

        verifyNoInteractions(cache);
    }

    @Test
    public void disposeWithNoContainerEverBuiltDoesNotThrow() {
        assertDoesNotThrow(() -> underTest.dispose());
    }

    @Test
    public void disposeDoesNotForceCreateTheCheckerCacheWhenItWasNeverCreated() {
        when(project.getServiceIfCreated(org.infernus.idea.checkstyle.checker.CheckerFactoryCache.class))
                .thenReturn(null);

        underTest.activateCheckstyleVersion(BUNDLED_VERSION, null);
        underTest.underlyingClassLoader();

        assertDoesNotThrow(() -> underTest.dispose());

        verify(project, never()).getService(org.infernus.idea.checkstyle.checker.CheckerFactoryCache.class);
    }
}
