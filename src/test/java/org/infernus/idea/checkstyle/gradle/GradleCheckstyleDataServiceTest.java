package org.infernus.idea.checkstyle.gradle;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.VersionListReader;
import org.infernus.idea.checkstyle.config.PluginConfiguration;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.infernus.idea.checkstyle.util.ProjectPaths;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GradleCheckstyleDataServiceTest {

    private final Project project = TestHelper.mockProject();
    private final PluginConfigurationManager pluginConfigurationManager = mock(PluginConfigurationManager.class);
    private final ProjectData projectData = mock(ProjectData.class);
    private final IdeModelsProvider modelsProvider = mock(IdeModelsProvider.class);

    private final GradleCheckstyleDataService dataService = new GradleCheckstyleDataService();

    @BeforeEach
    void setUp() {
        when(project.getService(PluginConfigurationManager.class)).thenReturn(pluginConfigurationManager);
        when(project.getService(ConfigurationLocationFactory.class)).thenReturn(new ConfigurationLocationFactory());

        // FileConfigurationLocation.setLocation/getLocation always tokenise/detokenise against the
        // project base directory (for $PROJECT_DIR$ storage), regardless of ConfigurationType - so
        // every test needs this wired, not just the PROJECT_RELATIVE-specific ones. Not stubbing a
        // project base directory here (projectPaths.projectPath returns null) makes tokenise() a no-op,
        // which is fine for tests that don't care about the stored location string's exact form.
        final ProjectPaths projectPaths = mock(ProjectPaths.class);
        when(project.getService(ProjectFilePaths.class))
                .thenReturn(ProjectFilePaths.testInstanceWith(project, projectPaths));
    }

    private void givenProjectBaseDirectory(final String basePath) {
        final ProjectPaths projectPaths = mock(ProjectPaths.class);
        final VirtualFile projectBaseDir = mock(VirtualFile.class);
        when(projectBaseDir.getPath()).thenReturn(basePath);
        when(projectPaths.projectPath(project)).thenReturn(projectBaseDir);
        when(project.getService(ProjectFilePaths.class))
                .thenReturn(ProjectFilePaths.testInstanceWith(project, projectPaths));
    }

    @Test
    void doesNothingWhenImportSettingsFromGradleIsDisabled(@TempDir final Path tempDir) {
        givenCurrentConfiguration(configuration(false));

        dataService.onSuccessImport(List.of(moduleNode(":", tempDir.resolve("checkstyle.xml"), Map.of(), null)),
                projectData, project, modelsProvider);

        verify(pluginConfigurationManager, never()).setCurrent(any(), anyBoolean());
    }

    @Test
    void addsAndActivatesALocationWithTheReservedIdWhenEnabled(@TempDir final Path tempDir) throws Exception {
        givenCurrentConfiguration(configuration(true));
        final File configFile = existingFile(tempDir, "checkstyle.xml");

        dataService.onSuccessImport(List.of(moduleNode(":", configFile.toPath(), Map.of(), null)),
                projectData, project, modelsProvider);

        final PluginConfiguration applied = capturedConfiguration();
        assertThat(applied.getLocationById(GradleCheckstyleDataService.GRADLE_CONFIG_LOCATION_ID).isPresent(), is(true));
        assertThat(applied.getActiveLocationIds().contains(GradleCheckstyleDataService.GRADLE_CONFIG_LOCATION_ID),
                is(true));
    }

    @Test
    void rootModuleWinsOverASubproject(@TempDir final Path tempDir) throws Exception {
        givenCurrentConfiguration(configuration(true));
        final File rootConfig = existingFile(tempDir, "root-checkstyle.xml");
        final File subConfig = existingFile(tempDir, "sub-checkstyle.xml");

        dataService.onSuccessImport(List.of(
                        moduleNode(":app", subConfig.toPath(), Map.of(), null),
                        moduleNode(":", rootConfig.toPath(), Map.of(), null)),
                projectData, project, modelsProvider);

        final ConfigurationLocation location = locationFrom(capturedConfiguration());
        assertThat(location.getLocation(), is(rootConfig.getAbsolutePath()));
    }

    @Test
    void firstSubprojectBySortedPathWinsWhenRootHasNoData(@TempDir final Path tempDir) throws Exception {
        givenCurrentConfiguration(configuration(true));
        final File appConfig = existingFile(tempDir, "app-checkstyle.xml");
        final File libConfig = existingFile(tempDir, "lib-checkstyle.xml");

        dataService.onSuccessImport(List.of(
                        moduleNode(":", null, Map.of(), null),
                        moduleNode(":lib", libConfig.toPath(), Map.of(), null),
                        moduleNode(":app", appConfig.toPath(), Map.of(), null)),
                projectData, project, modelsProvider);

        final ConfigurationLocation location = locationFrom(capturedConfiguration());
        assertThat(location.getLocation(), is(appConfig.getAbsolutePath()));
    }

    @Test
    void configPropertiesAreAppliedToTheLocation(@TempDir final Path tempDir) throws Exception {
        givenCurrentConfiguration(configuration(true));
        final File configFile = existingFile(tempDir, "checkstyle.xml");
        final String cacheFile = tempDir.resolve("build/checkstyle.cache").toString();

        dataService.onSuccessImport(
                List.of(moduleNode(":", configFile.toPath(), Map.of("checkstyle.cache.file", cacheFile), null)),
                projectData, project, modelsProvider);

        final ConfigurationLocation location = locationFrom(capturedConfiguration());
        assertThat(location.getProperties(), is(Map.of("checkstyle.cache.file", cacheFile)));
    }

    @Test
    void supportedToolVersionUpdatesTheCheckstyleVersion(@TempDir final Path tempDir) throws Exception {
        givenCurrentConfiguration(configuration(true));
        final File configFile = existingFile(tempDir, "checkstyle.xml");
        final String supportedVersion = new VersionListReader().getSupportedVersions().first();

        dataService.onSuccessImport(
                List.of(moduleNode(":", configFile.toPath(), Map.of(), supportedVersion)),
                projectData, project, modelsProvider);

        assertThat(capturedConfiguration().getCheckstyleVersion(), is(supportedVersion));
    }

    @Test
    void unsupportedToolVersionLeavesTheCheckstyleVersionUnchanged(@TempDir final Path tempDir) throws Exception {
        final PluginConfiguration current = configuration(true);
        givenCurrentConfiguration(current);
        final File configFile = existingFile(tempDir, "checkstyle.xml");

        dataService.onSuccessImport(
                List.of(moduleNode(":", configFile.toPath(), Map.of(), "not-a-real-version")),
                projectData, project, modelsProvider);

        assertThat(capturedConfiguration().getCheckstyleVersion(), is(current.getCheckstyleVersion()));
    }

    @Test
    void previouslyAddedLocationIsRemovedWhenNoIncomingModuleHasAUsableConfigFile(@TempDir final Path tempDir)
            throws Exception {
        final ConfigurationLocationFactory factory = new ConfigurationLocationFactory();
        when(project.getService(ConfigurationLocationFactory.class)).thenReturn(factory);
        final File configFile = existingFile(tempDir, "checkstyle.xml");
        final ConfigurationLocation existingReservedLocation = factory.create(project,
                GradleCheckstyleDataService.GRADLE_CONFIG_LOCATION_ID, ConfigurationType.LOCAL_FILE,
                configFile.getAbsolutePath(), GradleCheckstyleDataService.GRADLE_CONFIG_LOCATION_DESCRIPTION,
                NamedScopeHelper.getDefaultScope(project));
        final PluginConfiguration current = PluginConfigurationBuilder.testInstance("10.0.0")
                .withImportSettingsFromGradle(true)
                .withLocations(new TreeSet<>(List.of(existingReservedLocation)))
                .withActiveLocationIds(new TreeSet<>(List.of(GradleCheckstyleDataService.GRADLE_CONFIG_LOCATION_ID)))
                .build();
        givenCurrentConfiguration(current);

        dataService.onSuccessImport(List.of(moduleNode(":", null, Map.of(), null)),
                projectData, project, modelsProvider);

        final PluginConfiguration applied = capturedConfiguration();
        assertThat(applied.getLocationById(GradleCheckstyleDataService.GRADLE_CONFIG_LOCATION_ID).isPresent(),
                is(false));
    }

    @Test
    void nonSentinelLocationHoldingTheReservedIdIsReplaced(@TempDir final Path tempDir) throws Exception {
        final ConfigurationLocationFactory factory = new ConfigurationLocationFactory();
        when(project.getService(ConfigurationLocationFactory.class)).thenReturn(factory);
        final File oldConfigFile = existingFile(tempDir, "old-checkstyle.xml");
        final ConfigurationLocation handCreatedLocation = factory.create(project,
                GradleCheckstyleDataService.GRADLE_CONFIG_LOCATION_ID, ConfigurationType.LOCAL_FILE,
                oldConfigFile.getAbsolutePath(), "Not the sentinel description",
                NamedScopeHelper.getDefaultScope(project));
        final PluginConfiguration current = PluginConfigurationBuilder.testInstance("10.0.0")
                .withImportSettingsFromGradle(true)
                .withLocations(new TreeSet<>(List.of(handCreatedLocation)))
                .build();
        givenCurrentConfiguration(current);
        final File newConfigFile = existingFile(tempDir, "new-checkstyle.xml");

        dataService.onSuccessImport(List.of(moduleNode(":", newConfigFile.toPath(), Map.of(), null)),
                projectData, project, modelsProvider);

        final ConfigurationLocation location = locationFrom(capturedConfiguration());
        assertThat(location.getLocation(), is(newConfigFile.getAbsolutePath()));
        assertThat(location.getDescription(), is(GradleCheckstyleDataService.GRADLE_CONFIG_LOCATION_DESCRIPTION));
    }

    @Test
    void secondImportWithIdenticalResultDoesNotCallSetCurrentAgain(@TempDir final Path tempDir) throws Exception {
        givenCurrentConfiguration(configuration(true));
        final File configFile = existingFile(tempDir, "checkstyle.xml");

        dataService.onSuccessImport(List.of(moduleNode(":", configFile.toPath(), Map.of(), null)),
                projectData, project, modelsProvider);
        final PluginConfiguration afterFirstImport = capturedConfiguration();
        // Simulate the manager now holding the configuration produced by the first import.
        when(pluginConfigurationManager.getCurrent()).thenReturn(afterFirstImport);

        dataService.onSuccessImport(List.of(moduleNode(":", configFile.toPath(), Map.of(), null)),
                projectData, project, modelsProvider);

        verify(pluginConfigurationManager, times(1)).setCurrent(any(), anyBoolean());
    }

    @Test
    void configFileUnderTheProjectBaseDirIsProjectRelative(@TempDir final Path tempDir) throws Exception {
        when(project.getBasePath()).thenReturn(tempDir.toString());
        givenProjectBaseDirectory(tempDir.toString());
        givenCurrentConfiguration(configuration(true));
        final File configFile = existingFile(tempDir, "checkstyle.xml");

        dataService.onSuccessImport(List.of(moduleNode(":", configFile.toPath(), Map.of(), null)),
                projectData, project, modelsProvider);

        final ConfigurationLocation location = locationFrom(capturedConfiguration());
        assertThat(location.getType(), is(ConfigurationType.PROJECT_RELATIVE));
    }

    @Test
    void configFileOutsideTheProjectBaseDirIsLocalFile(@TempDir final Path tempDir) throws Exception {
        final Path projectDir = tempDir.resolve("project");
        Files.createDirectories(projectDir);
        when(project.getBasePath()).thenReturn(projectDir.toString());
        givenProjectBaseDirectory(projectDir.toString());
        givenCurrentConfiguration(configuration(true));
        final File configFile = existingFile(tempDir, "outside-checkstyle.xml");

        dataService.onSuccessImport(List.of(moduleNode(":", configFile.toPath(), Map.of(), null)),
                projectData, project, modelsProvider);

        final ConfigurationLocation location = locationFrom(capturedConfiguration());
        assertThat(location.getType(), is(ConfigurationType.LOCAL_FILE));
    }

    // --- helpers ---

    private void givenCurrentConfiguration(final PluginConfiguration configuration) {
        when(pluginConfigurationManager.getCurrent()).thenReturn(configuration);
    }

    private PluginConfiguration capturedConfiguration() {
        final ArgumentCaptor<PluginConfiguration> captor = ArgumentCaptor.forClass(PluginConfiguration.class);
        verify(pluginConfigurationManager).setCurrent(captor.capture(), anyBoolean());
        return captor.getValue();
    }

    private static ConfigurationLocation locationFrom(final PluginConfiguration configuration) {
        return configuration.getLocationById(GradleCheckstyleDataService.GRADLE_CONFIG_LOCATION_ID).orElseThrow();
    }

    private static PluginConfiguration configuration(final boolean importSettingsFromGradle) {
        return PluginConfigurationBuilder.testInstance("10.0.0")
                .withImportSettingsFromGradle(importSettingsFromGradle)
                .build();
    }

    @NotNull
    private static File existingFile(final Path tempDir, final String name) throws Exception {
        final Path file = tempDir.resolve(name);
        Files.writeString(file, "<module name=\"Checker\"/>");
        return file.toFile();
    }

    private static DataNode<CheckstyleGradleModuleData> moduleNode(final String gradleProjectPath,
                                                                     final Path configFile,
                                                                     final Map<String, String> configProperties,
                                                                     final String toolVersion) {
        final CheckstyleGradleModuleData data = new CheckstyleGradleModuleData(gradleProjectPath,
                configFile != null ? configFile.toString() : null, configProperties, toolVersion);
        return new DataNode<>(CheckstyleGradleModuleData.KEY, data, null);
    }
}
