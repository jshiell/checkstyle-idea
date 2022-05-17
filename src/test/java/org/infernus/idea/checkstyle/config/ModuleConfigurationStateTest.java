package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.NamedScopeHelper;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.infernus.idea.checkstyle.util.ProjectPaths;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModuleConfigurationStateTest {

    private final Project project = TestHelper.mockProject();
    private final Module module = mock(Module.class);
    private final PluginConfigurationManager pluginConfigurationManager = new PluginConfigurationManager(project);
    private final ProjectPaths projectPaths = mock(ProjectPaths.class);
    private final ConfigurationLocationFactory configurationLocationFactory = new ConfigurationLocationFactory();
    private final ProjectFilePaths projectFilePaths = new ProjectFilePaths(project, File.separatorChar, File::getAbsolutePath, projectPaths);
    private final NamedScope allScope = NamedScopeHelper.getScopeByIdWithDefaultFallback(project, "All");

    private ConfigurationLocation expectedLocation;

    @Before
    public void configureMocks() {
        when(module.getProject()).thenReturn(project);

        final VirtualFile projectPath = mock(VirtualFile.class);
        when(projectPath.getPath()).thenReturn("/a/project/path");
        when(projectPaths.projectPath(project)).thenReturn(projectPath);

        when(project.getService(ConfigurationLocationFactory.class)).thenReturn(configurationLocationFactory);
        when(project.getService(ProjectFilePaths.class)).thenReturn(projectFilePaths);
        when(project.getService(PluginConfigurationManager.class)).thenReturn(pluginConfigurationManager);

        final ApplicationConfigurationState applicationConfigurationState = new ApplicationConfigurationState();
        final Application application = mock(Application.class);
        when(application.isUnitTestMode()).thenReturn(true);
        when(application.getService(ApplicationConfigurationState.class)).thenReturn(applicationConfigurationState);
        ApplicationManager.setApplication(application, mock(Disposable.class));

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

    @Test
    public void theActiveLocationCanBeDeserialised() {
        final ModuleConfigurationState.ModuleSettings moduleSettings
                = ModuleConfigurationState.ModuleSettings.create(testConfiguration());

        ModuleConfigurationState underTest = new ModuleConfigurationState(module);
        underTest.loadState(moduleSettings);

        SortedSet<String> activeLocations = underTest.getActiveLocationIds();
        assertThat(activeLocations, hasSize(1));
        assertThat(expectedLocation, not(nullValue()));
        assertThat(activeLocations.first(), equalTo(expectedLocation.getId()));
    }

    @NotNull
    private Map<String, String> testConfiguration() {
        return Map.of(
                "active-configuration", "LOCAL_FILE:$PROJECT_DIR$/test-configs/working-checkstyle-rules-module.xml:Working Module;All"
        );
    }

}
