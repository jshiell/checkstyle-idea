package org.infernus.idea.checkstyle.maven;

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
import kotlin.sequences.SequencesKt;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MavenCheckstyleConfiguratorAfterImportTest extends BasePlatformTestCase {

    private MavenCheckstyleConfigurator configurator;
    private PluginConfigurationManager configManager;
    private MavenAfterImportConfigurator.Context context;
    private MavenProject mavenProject;
    private Path physicalTempDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configurator = new MavenCheckstyleConfigurator();
        configManager = getProject().getService(PluginConfigurationManager.class);

        physicalTempDir = Files.createTempDirectory("maven-test");
        VfsRootAccess.allowRootAccess(getTestRootDisposable(), physicalTempDir.toString());

        mavenProject = mock(MavenProject.class);
        when(mavenProject.getMavenId()).thenReturn(new MavenId("test", "test", "1"));
        when(mavenProject.findPlugin("org.apache.maven.plugins", "maven-checkstyle-plugin"))
            .thenReturn(null);
        when(mavenProject.getLocalRepository()).thenReturn(new File(MavenUtil.resolveDefaultLocalRepository().toString()));
        when(mavenProject.getFile()).thenReturn(mock(VirtualFile.class));

        @SuppressWarnings("unchecked")
        MavenProjectWithModules<Module> projectWithModules = mock(MavenProjectWithModules.class);
        when(projectWithModules.getMavenProject()).thenReturn(mavenProject);

        context = mock(MavenAfterImportConfigurator.Context.class);
        when(context.getProject()).thenReturn(getProject());
        when(context.getMavenProjectsWithModules())
            .thenReturn(SequencesKt.asSequence(List.of(projectWithModules).iterator()));
    }

    private MavenPlugin pluginWithConfig(Element configElement) {
        MavenPlugin plugin = mock(MavenPlugin.class);
        when(plugin.getConfigurationElement()).thenReturn(configElement);
        when(plugin.getDependencies()).thenReturn(List.of(dep("com.puppycrawl.tools", "checkstyle", "13.6.0")));
        when(plugin.getMavenId()).thenReturn(new MavenId("org.apache.maven.plugins", "maven-checkstyle-plugin", "3.6.0"));
        when(mavenProject.findPlugin("org.apache.maven.plugins", "maven-checkstyle-plugin"))
            .thenReturn(plugin);
        return plugin;
    }

    private MavenPlugin pluginWithDependencies(List<MavenId> deps) {
        MavenPlugin plugin = mock(MavenPlugin.class);
        when(plugin.getConfigurationElement()).thenReturn(null);
        when(plugin.getDependencies()).thenReturn(deps);
        when(plugin.getMavenId()).thenReturn(new MavenId("org.apache.maven.plugins", "maven-checkstyle-plugin", "3.6.0"));
        when(mavenProject.findPlugin("org.apache.maven.plugins", "maven-checkstyle-plugin"))
            .thenReturn(plugin);
        return plugin;
    }

    private static MavenId dep(String groupId, String artifactId, String version) {
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
    private VirtualFile fixtureFile(String filename, String content) throws Exception {
        Files.writeString(physicalTempDir.resolve(filename), content);
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(physicalTempDir.resolve(filename));
        when(mavenProject.getDirectoryFile()).thenReturn(vf.getParent());
        return vf;
    }

    // --- tests ---

    public void testImportSettingsFromMavenIsDisabled_doesNothing() {
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

    public void testImportSettingsFromMavenIsEnabled_updatesVersion() {
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

    public void testImportSettingsFromMavenIsEnabled_updatesThirdPartyClasspath() {
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

        String expectedJar = MavenUtil.resolveDefaultLocalRepository()
            + "/com/checkstyle/third/party/rules/cool-stuff/3.2.1/cool-stuff-3.2.1.jar"
                .replace("/", File.separator);
        assertEquals(List.of(expectedJar), configManager.getCurrent().getThirdPartyClasspath());
    }

    public void testMavenCheckstylePluginNotConfigured_doesNotThrow() {
        enableMavenImport();
        // mavenProject.findPlugin returns null (set up in setUp)

        configurator.afterImport(context);
        // no assertion needed — just mustn't throw
    }

    public void testConfigLocationMissingAndMavenConfigExists_removesMavenConfigLocation() throws Exception {
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

    public void testConfigLocationExistsAndMavenConfigAlreadyExists_overwritesWithNewConfig() throws Exception {
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

    public void testMavenConfigDoesNotAlreadyExist_addsNewConfig() throws Exception {
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

    public void testConfigLocationNotOnDiskOrClasspath_doesNotAddLocation() throws Exception {
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

    public void testOnlySuppressionLocationChanges_updatesProperties() throws Exception {
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
