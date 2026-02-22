package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.testFramework.LightPlatformTestCase;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.infernus.idea.checkstyle.util.ProjectPaths;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModuleConfigurationStateTest extends LightPlatformTestCase {

    private final Project project = TestHelper.mockProject();
    private final Module module = mock(Module.class);
    private final PluginConfigurationManager pluginConfigurationManager = new PluginConfigurationManager(project);
    private final ProjectPaths projectPaths = mock(ProjectPaths.class);
    private final ConfigurationLocationFactory configurationLocationFactory = new ConfigurationLocationFactory();
    private final ProjectFilePaths projectFilePaths = ProjectFilePaths.testInstanceWith(project, File.separatorChar, File::getAbsolutePath, projectPaths);
    private final NamedScope allScope = NamedScopeHelper.getScopeByIdWithDefaultFallback(project, "All");

    private ConfigurationLocation expectedLocation;

    public void setUp() throws Exception {
        super.setUp();

        when(module.getProject()).thenReturn(project);

        final VirtualFile projectPath = mock(VirtualFile.class);
        when(projectPath.getPath()).thenReturn("/a/project/path");
        when(projectPaths.projectPath(project)).thenReturn(projectPath);

        when(project.getService(ConfigurationLocationFactory.class)).thenReturn(configurationLocationFactory);
        when(project.getService(ProjectFilePaths.class)).thenReturn(projectFilePaths);
        when(project.getService(PluginConfigurationManager.class)).thenReturn(pluginConfigurationManager);

        ProjectConfigurationState projectConfigurationState = new ProjectConfigurationState(project);
        when(project.getService(ProjectConfigurationState.class)).thenReturn(projectConfigurationState);

        expectedLocation = configurationLocationFactory.create(
                project,
                "id1",
                ConfigurationType.LOCAL_FILE,
                "/a/project/path/test-configs/working-checkstyle-rules-module.xml",
                "Working Module",
                allScope);
        pluginConfigurationManager.setCurrent(PluginConfigurationBuilder.defaultConfiguration(project)
                .withLocations(new TreeSet<>(List.of(
                        configurationLocationFactory.create(
                                project,
                                "id0",
                                ConfigurationType.LOCAL_FILE,
                                "/a/project/path/test-configs/broken-checkstyle-rules-module.xml",
                                "Broken Module",
                                allScope),
                        expectedLocation
                )))
                .build(), false);
    }

    public void testTheActiveLocationCanBeDeserialised() {
        final ModuleConfigurationState.ModuleSettings moduleSettings
                = ModuleConfigurationState.ModuleSettings.create(generateTestConfiguration());

        ModuleConfigurationState underTest = new ModuleConfigurationState(module);
        underTest.loadState(moduleSettings);

        SortedSet<String> activeLocations = underTest.getActiveLocationIds();
        assertThat(activeLocations, hasSize(1));
        assertThat(expectedLocation, not(nullValue()));
        assertThat(activeLocations.first(), equalTo(expectedLocation.getId()));
    }

    @NotNull
    private Map<String, String> generateTestConfiguration() {
        return Map.of(
                "active-configuration", "LOCAL_FILE:/a/project/path/test-configs/working-checkstyle-rules-module.xml:Working Module;All"
        );
    }

}
