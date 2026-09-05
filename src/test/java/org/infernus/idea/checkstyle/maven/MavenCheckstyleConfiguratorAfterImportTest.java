package org.infernus.idea.checkstyle.maven;

import com.intellij.ide.trustedProjects.TrustedProjects;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import kotlin.sequences.SequencesKt;
import org.infernus.idea.checkstyle.CheckstyleArtifactDownloader;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.exception.CheckstyleDownloadException;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.jdom.Element;
import org.jetbrains.idea.maven.importing.MavenAfterImportConfigurator;
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator.MavenProjectWithModules;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.model.MavenPlugin;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.utils.MavenUtil;
import org.mockito.MockedStatic;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.anyString;

public class MavenCheckstyleConfiguratorAfterImportTest extends BasePlatformTestCase {

    private MavenCheckstyleConfigurator configurator;
    private PluginConfigurationManager configManager;
    private MavenAfterImportConfigurator.Context context;
    private MavenProject mavenProject;
    private Path physicalTempDir;
    private CheckstyleArtifactDownloader checkstyleArtifactDownloader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        checkstyleArtifactDownloader = mock(CheckstyleArtifactDownloader.class);
        when(checkstyleArtifactDownloader.download(anyString())).thenReturn(List.of());
        configurator = new MavenCheckstyleConfigurator(checkstyleArtifactDownloader);
        configManager = getProject().getService(PluginConfigurationManager.class);

        physicalTempDir = Files.createTempDirectory("maven-test");
        VfsRootAccess.allowRootAccess(getTestRootDisposable(), physicalTempDir.toString());

        mavenProject = mock(MavenProject.class);
        when(mavenProject.getMavenId()).thenReturn(new MavenId("test", "test", "1"));
        when(mavenProject.findPlugin("org.apache.maven.plugins", "maven-checkstyle-plugin"))
            .thenReturn(null);
        when(mavenProject.getLocalRepositoryPath()).thenReturn(MavenUtil.resolveDefaultLocalRepository(null));
        when(mavenProject.getFile()).thenReturn(mock(VirtualFile.class));

        @SuppressWarnings("unchecked")
        final MavenProjectWithModules<Module> projectWithModules = mock(MavenProjectWithModules.class);
        when(projectWithModules.getMavenProject()).thenReturn(mavenProject);

        context = mock(MavenAfterImportConfigurator.Context.class);
        when(context.getProject()).thenReturn(getProject());
        when(context.getMavenProjectsWithModules())
            .thenReturn(SequencesKt.asSequence(List.of(projectWithModules).iterator()));
    }

    private MavenPlugin pluginWithConfig(final Element configElement) {
        MavenPlugin plugin = mock(MavenPlugin.class);
        when(plugin.getConfigurationElement()).thenReturn(configElement);
        when(plugin.getDependencies()).thenReturn(List.of(dep("com.puppycrawl.tools", "checkstyle", "14.1.0")));
        when(plugin.getMavenId()).thenReturn(new MavenId("org.apache.maven.plugins", "maven-checkstyle-plugin", "3.6.0"));
        when(mavenProject.findPlugin("org.apache.maven.plugins", "maven-checkstyle-plugin"))
            .thenReturn(plugin);
        return plugin;
    }

    private MavenPlugin pluginWithDependencies(final List<MavenId> deps) {
        MavenPlugin plugin = mock(MavenPlugin.class);
        when(plugin.getConfigurationElement()).thenReturn(null);
        when(plugin.getDependencies()).thenReturn(deps);
        when(plugin.getMavenId()).thenReturn(new MavenId("org.apache.maven.plugins", "maven-checkstyle-plugin", "3.6.0"));
        when(mavenProject.findPlugin("org.apache.maven.plugins", "maven-checkstyle-plugin"))
            .thenReturn(plugin);
        return plugin;
    }

    private static MavenId dep(final String groupId,
                               final String artifactId,
                               final String version) {
        return new MavenId(groupId, artifactId, version);
    }

    private void enableMavenImport() {
        configManager.setCurrent(
            PluginConfigurationBuilder.from(configManager.getCurrent())
                .withImportSettingsFromMaven(true)
                .build(),
            true);
    }

    /** Creates a physical file in a temp dir and stubs getDirectoryFile() to return the parent. */
    private VirtualFile fixtureFile(final String filename,
                                    final String content) throws Exception {
        Files.writeString(physicalTempDir.resolve(filename), content);
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(physicalTempDir.resolve(filename));
        when(mavenProject.getDirectoryFile()).thenReturn(vf.getParent());
        return vf;
    }

    // --- tests ---

    public void testImportSettingsFromMavenIsDisabledDoesNothing() {
        configManager.setCurrent(
            PluginConfigurationBuilder.from(configManager.getCurrent())
                .withImportSettingsFromMaven(false)
                .withCheckstyleVersion("10.26.0")
                .build(),
            true);
        pluginWithDependencies(List.of(dep("com.puppycrawl.tools", "checkstyle", "10.26.1")));

        configurator.afterImport(context);

        assertEquals("10.26.0", configManager.getCurrent().getCheckstyleVersion());
    }

    public void testImportSettingsFromMavenIsEnabledUpdatesVersion() {
        configManager.setCurrent(
            PluginConfigurationBuilder.from(configManager.getCurrent())
                .withImportSettingsFromMaven(true)
                .withCheckstyleVersion("10.26.0")
                .build(),
            true);
        pluginWithDependencies(List.of(dep("com.puppycrawl.tools", "checkstyle", "10.26.1")));

        configurator.afterImport(context);

        assertEquals("10.26.1", configManager.getCurrent().getCheckstyleVersion());
    }

    public void testImportSettingsFromMavenIsEnabledUpdatesThirdPartyClasspath() {
        enableMavenImport();
        configManager.setCurrent(
            PluginConfigurationBuilder.from(configManager.getCurrent())
                .withThirdPartyClassPath(List.of("/com/stuff/something.jar"))
                .build(),
            true);
        pluginWithDependencies(List.of(
            dep("com.puppycrawl.tools", "checkstyle", "10.26.1"),
            dep("com.checkstyle.third.party.rules", "cool-stuff", "3.2.1")));

        configurator.afterImport(context);

        String expectedJar = MavenUtil.resolveDefaultLocalRepository(null)
            + "/com/checkstyle/third/party/rules/cool-stuff/3.2.1/cool-stuff-3.2.1.jar"
                .replace("/", File.separator);
        assertEquals(List.of(expectedJar), configManager.getCurrent().getThirdPartyClasspath());
    }

    public void testMavenCheckstylePluginNotConfiguredDoesNotThrow() {
        enableMavenImport();
        // mavenProject.findPlugin returns null (set up in setUp)

        configurator.afterImport(context);
        // no assertion needed — just mustn't throw
    }

    public void testConfigLocationMissingAndMavenConfigExistsRemovesMavenConfigLocation() {
        enableMavenImport();
        var factory = getProject().getService(ConfigurationLocationFactory.class);
        var mavenLoc = factory.create(getProject(), "maven-config-location",
            ConfigurationType.PROJECT_RELATIVE, "checkstyle.xml", "Maven Config Location",
            NamedScopeHelper.getDefaultScope(getProject()));
        configManager.setCurrent(
            PluginConfigurationBuilder.from(configManager.getCurrent())
                .withLocations(new TreeSet<>(List.of(mavenLoc)))
                .withActiveLocationIds(new TreeSet<>(List.of(mavenLoc.getId())))
                .build(),
            true);
        pluginWithConfig(null);  // no configLocation element

        configurator.afterImport(context);

        assertTrue(configManager.getCurrent().getLocations().stream()
            .noneMatch(loc -> "maven-config-location".equals(loc.getId())));
    }

    public void testConfigLocationExistsAndMavenConfigAlreadyExistsOverwritesWithNewConfig() throws Exception {
        enableMavenImport();
        var checkstyleVf = fixtureFile("checkstyle.xml", "<config></config>");
        var factory = getProject().getService(ConfigurationLocationFactory.class);
        var mavenLoc = factory.create(getProject(), "maven-config-location",
            ConfigurationType.PROJECT_RELATIVE, "checkstyle-existing.xml", "Maven Config Location",
            NamedScopeHelper.getDefaultScope(getProject()));
        configManager.setCurrent(
            PluginConfigurationBuilder.from(configManager.getCurrent())
                .withLocations(new TreeSet<>(List.of(mavenLoc)))
                .withActiveLocationIds(new TreeSet<>(List.of(mavenLoc.getId())))
                .build(),
            true);

        var config = new Element("configuration");
        config.addContent(new Element("configLocation").setText("checkstyle.xml"));
        pluginWithConfig(config);

        configurator.afterImport(context);

        String expectedPath = mavenProject.getDirectoryFile().toNioPath().resolve("checkstyle.xml").normalize().toString();
        String storedPath = Path.of(configManager.getCurrent().getLocations().stream()
            .filter(loc -> "maven-config-location".equals(loc.getId()))
            .map(ConfigurationLocation::getLocation)
            .findFirst()
            .get()).normalize().toString();
        assertEquals(expectedPath, storedPath);
    }

    public void testMavenConfigDoesNotAlreadyExistAddsNewConfig() throws Exception {
        enableMavenImport();
        fixtureFile("checkstyle.xml", "<config></config>");
        configManager.setCurrent(
            PluginConfigurationBuilder.from(configManager.getCurrent())
                .withLocations(new TreeSet<>())
                .withActiveLocationIds(new TreeSet<>())
                .build(),
            true);

        var config = new Element("configuration");
        config.addContent(new Element("configLocation").setText("checkstyle.xml"));
        pluginWithConfig(config);

        configurator.afterImport(context);

        String expectedPath = mavenProject.getDirectoryFile().toNioPath().resolve("checkstyle.xml").normalize().toString();
        String storedPath = Path.of(configManager.getCurrent().getLocations().stream()
            .filter(loc -> "maven-config-location".equals(loc.getId()))
            .map(ConfigurationLocation::getLocation)
            .findFirst()
            .get()).normalize().toString();
        assertEquals(expectedPath, storedPath);
    }

    public void testConfigLocationNotOnDiskOrClasspathDoesNotAddLocation() throws Exception {
        enableMavenImport();
        fixtureFile(".placeholder", "");  // set up a valid directory VF
        configManager.setCurrent(
            PluginConfigurationBuilder.from(configManager.getCurrent())
                .withLocations(new TreeSet<>())
                .withActiveLocationIds(new TreeSet<>())
                .build(),
            true);

        var config = new Element("configuration");
        config.addContent(new Element("configLocation").setText("nonexistent-resource-that-does-not-exist-anywhere.xml"));
        pluginWithConfig(config);

        configurator.afterImport(context);

        assertTrue(configManager.getCurrent().getLocations().stream()
            .noneMatch(loc -> "maven-config-location".equals(loc.getId())));
    }

    public void testConfigLocationOnClasspathButResourceMissingDoesNotThrow() throws Exception {
        enableMavenImport();
        fixtureFile(".placeholder", "");  // required: sets up mavenProject.getDirectoryFile(),
                                           // which createConfigurationLocation() dereferences on the
                                           // way to the classpath-resource fallback branch. The classloader
                                           // now builds successfully (the version downloads fine); the
                                           // resource is simply not present in it, so resolution still
                                           // fails, just for a different reason than before.
        configManager.setCurrent(
            PluginConfigurationBuilder.from(configManager.getCurrent())
                .withLocations(new TreeSet<>())
                .withActiveLocationIds(new TreeSet<>())
                .build(),
            true);

        var config = new Element("configuration");
        config.addContent(new Element("configLocation").setText("classpath-resource-that-does-not-exist.xml"));
        MavenPlugin plugin = pluginWithConfig(config);
        when(plugin.getDependencies()).thenReturn(List.of(dep("com.puppycrawl.tools", "checkstyle", "10.21.3")));

        configurator.afterImport(context); // must not throw

        assertTrue(configManager.getCurrent().getLocations().stream()
            .noneMatch(loc -> "maven-config-location".equals(loc.getId())));
    }

    public void testConfigLocationOnClasspathWithNonBundledVersionAndSuccessfulDownloadResolves() throws Exception {
        enableMavenImport();
        fixtureFile(".placeholder", "");  // required: sets up mavenProject.getDirectoryFile()
        configManager.setCurrent(
            PluginConfigurationBuilder.from(configManager.getCurrent())
                .withLocations(new TreeSet<>())
                .withActiveLocationIds(new TreeSet<>())
                .build(),
            true);

        final Path fakeM2Root = physicalTempDir.resolve("fake-m2-repo");
        Files.createDirectories(fakeM2Root);
        when(mavenProject.getLocalRepositoryPath()).thenReturn(fakeM2Root);

        final MavenId thirdPartyJarId = new MavenId("com.checkstyle.third.party.rules", "cool-stuff", "3.2.1");
        final Path thirdPartyJarPath = MavenUtil.getArtifactPath(fakeM2Root, thirdPartyJarId, "jar", null);
        Files.createDirectories(thirdPartyJarPath.getParent());
        try (var out = Files.newOutputStream(thirdPartyJarPath);
             var zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("our_checks.xml"));
            zip.write("<config></config>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        final Path placeholderEngineJar = physicalTempDir.resolve("checkstyle-10.21.3-placeholder.jar");
        Files.createFile(placeholderEngineJar);
        when(checkstyleArtifactDownloader.download("10.21.3")).thenReturn(List.of(placeholderEngineJar));

        var config = new Element("configuration");
        config.addContent(new Element("configLocation").setText("our_checks.xml"));
        MavenPlugin plugin = pluginWithConfig(config);
        when(plugin.getDependencies()).thenReturn(List.of(
            dep("com.puppycrawl.tools", "checkstyle", "10.21.3"),
            dep("com.checkstyle.third.party.rules", "cool-stuff", "3.2.1")));

        try (MockedStatic<TrustedProjects> ignored = mockStatic(TrustedProjects.class,
                withSettings().strictness(Strictness.LENIENT))) {
            ignored.when(() -> TrustedProjects.isProjectTrusted(getProject())).thenReturn(true);

            configurator.afterImport(context);
        }

        var mavenLocation = configManager.getCurrent().getLocations().stream()
            .filter(loc -> "maven-config-location".equals(loc.getId()))
            .findFirst();
        assertTrue(mavenLocation.isPresent());
        assertEquals(ConfigurationType.PLUGIN_CLASSPATH, mavenLocation.get().getType());
        assertEquals("our_checks.xml", mavenLocation.get().getLocation());
    }

    public void testConfigLocationOnClasspathWithNonBundledVersionAndFailedDownloadDoesNotThrow() throws Exception {
        enableMavenImport();
        fixtureFile(".placeholder", "");  // required: sets up mavenProject.getDirectoryFile()
        configManager.setCurrent(
            PluginConfigurationBuilder.from(configManager.getCurrent())
                .withLocations(new TreeSet<>())
                .withActiveLocationIds(new TreeSet<>())
                .build(),
            true);

        when(checkstyleArtifactDownloader.download("10.21.3"))
            .thenThrow(new CheckstyleDownloadException("connection refused"));

        var config = new Element("configuration");
        config.addContent(new Element("configLocation").setText("classpath-resource-that-does-not-exist.xml"));
        MavenPlugin plugin = pluginWithConfig(config);
        when(plugin.getDependencies()).thenReturn(List.of(dep("com.puppycrawl.tools", "checkstyle", "10.21.3")));

        configurator.afterImport(context); // must not throw

        assertTrue(configManager.getCurrent().getLocations().stream()
            .noneMatch(loc -> "maven-config-location".equals(loc.getId())));
    }

    public void testOnlySuppressionLocationChangesUpdatesProperties() throws Exception {
        enableMavenImport();
        fixtureFile("checkstyle.xml", "<config></config>");
        var factory = getProject().getService(ConfigurationLocationFactory.class);
        var mavenLoc = factory.create(getProject(), "maven-config-location",
            ConfigurationType.PROJECT_RELATIVE,
            mavenProject.getDirectoryFile().toNioPath().resolve("checkstyle.xml").normalize().toString(),
            "Maven Config Location", NamedScopeHelper.getDefaultScope(getProject()));
        mavenLoc.setProperties(Map.of("checkstyle.suppressions.file", "old-suppressions.xml"));
        configManager.setCurrent(
            PluginConfigurationBuilder.from(configManager.getCurrent())
                .withLocations(new TreeSet<>(List.of(mavenLoc)))
                .withActiveLocationIds(new TreeSet<>(List.of("maven-config-location")))
                .build(),
            true);

        var config = new Element("configuration");
        config.addContent(new Element("configLocation").setText("checkstyle.xml"));
        config.addContent(new Element("suppressionsLocation").setText("new-suppressions.xml"));
        pluginWithConfig(config);

        configurator.afterImport(context);

        var props = configManager.getCurrent().getLocations().stream()
            .filter(loc -> "maven-config-location".equals(loc.getId()))
            .findFirst()
            .orElseThrow()
            .getProperties();
        assertEquals("new-suppressions.xml", props.get("checkstyle.suppressions.file"));
    }
}
