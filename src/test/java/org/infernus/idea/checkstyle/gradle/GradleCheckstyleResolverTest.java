package org.infernus.idea.checkstyle.gradle;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.project.Project;
import java.util.Map;
import java.util.Set;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.idea.IdeaModule;
import org.infernus.idea.checkstyle.config.PluginConfigurationBuilder;
import org.infernus.idea.checkstyle.config.PluginConfigurationManager;
import org.infernus.idea.checkstyle.gradle.tooling.CheckstyleGradleModel;
import org.infernus.idea.checkstyle.gradle.tooling.CheckstyleGradleModelBuilder;
import org.infernus.idea.checkstyle.gradle.tooling.CheckstyleGradleModelImpl;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverExtension;
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GradleCheckstyleResolverTest {

    private final GradleCheckstyleResolver resolver = new GradleCheckstyleResolver();
    private final ProjectResolverContext resolverContext = mock(ProjectResolverContext.class);
    private final IdeaModule gradleModule = mock(IdeaModule.class);
    private final GradleProject gradleProject = mock(GradleProject.class);
    private final GradleProjectResolverExtension nextResolver = mock(GradleProjectResolverExtension.class);
    private final ModuleData moduleData = new ModuleData("module-id", GradleConstants.SYSTEM_ID, "typeId",
            "moduleName", "/path/to/module", "/path/to/module/build.gradle");
    private final DataNode<ModuleData> ideModule = new DataNode<>(ProjectKeys.MODULE, moduleData, null);

    @BeforeEach
    void setUp() {
        when(gradleModule.getGradleProject()).thenReturn(gradleProject);
        when(gradleProject.getPath()).thenReturn(":app");
        resolver.setProjectResolverContext(resolverContext);
        resolver.setNext(nextResolver);
    }

    @Test
    void attachesAChildDataNodeWithThePopulatedModel() {
        final CheckstyleGradleModel model = new CheckstyleGradleModelImpl("/path/to/checkstyle.xml",
                Map.of("checkstyle.cache.file", "/path/to/cache"), "10.12.1");
        when(resolverContext.getExtraProject(gradleModule, CheckstyleGradleModel.class)).thenReturn(model);

        resolver.populateModuleExtraModels(gradleModule, ideModule);

        final CheckstyleGradleModuleData data = childData(ideModule);
        assertThat(data.getGradleProjectPath(), is(":app"));
        assertThat(data.getConfigFile(), is("/path/to/checkstyle.xml"));
        assertThat(data.getConfigProperties(), is(Map.of("checkstyle.cache.file", "/path/to/cache")));
        assertThat(data.getToolVersion(), is("10.12.1"));
    }

    @Test
    void attachesAChildDataNodeWithANothingConfiguredPayloadWhenNoModelIsAvailable() {
        when(resolverContext.getExtraProject(gradleModule, CheckstyleGradleModel.class)).thenReturn(null);

        resolver.populateModuleExtraModels(gradleModule, ideModule);

        final CheckstyleGradleModuleData data = childData(ideModule);
        assertThat(data, is(notNullValue()));
        assertThat(data.getGradleProjectPath(), is(":app"));
        assertThat(data.getConfigFile(), is(nullValue()));
        assertThat(data.getConfigProperties(), is(Map.of()));
        assertThat(data.getToolVersion(), is(nullValue()));
    }

    @Test
    void callsTheNextResolverInTheChain() {
        resolver.populateModuleExtraModels(gradleModule, ideModule);

        verify(nextResolver).populateModuleExtraModels(eq(gradleModule), eq(ideModule));
    }

    @Test
    void checkstyleGradleModuleDataSurvivesASerializableRoundTrip() throws Exception {
        final CheckstyleGradleModuleData original = new CheckstyleGradleModuleData(":app",
                "/path/to/checkstyle.xml", Map.of("checkstyle.cache.file", "/path/to/cache"), "10.12.1");

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        final CheckstyleGradleModuleData restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (CheckstyleGradleModuleData) in.readObject();
        }

        assertThat(restored.getGradleProjectPath(), is(original.getGradleProjectPath()));
        assertThat(restored.getConfigFile(), is(original.getConfigFile()));
        assertThat(restored.getConfigProperties(), is(original.getConfigProperties()));
        assertThat(restored.getToolVersion(), is(original.getToolVersion()));
    }

    @Test
    void registersTheToolingClassesWhenGradleImportIsEnabled() {
        givenProjectWithGradleImport(true);

        assertThat(resolver.getExtraProjectModelClasses(), hasItems(CheckstyleGradleModel.class));
        assertThat(resolver.getToolingExtensionsClasses(), hasItems(CheckstyleGradleModelBuilder.class,
                CheckstyleGradleModel.class, CheckstyleGradleModelImpl.class));
    }

    @Test
    void registersNoToolingClassesWhenGradleImportIsDisabled() {
        givenProjectWithGradleImport(false);

        assertNoClassesRegistered(resolver);
    }

    @Test
    void registersNoToolingClassesWhenTheProjectCannotBeFound() {
        final ExternalSystemTaskId taskId = mock(ExternalSystemTaskId.class);
        when(resolverContext.getExternalSystemTaskId()).thenReturn(taskId);
        when(taskId.findProject()).thenReturn(null);

        assertNoClassesRegistered(resolver);
    }

    @Test
    void registersNoToolingClassesWhenThereIsNoTaskId() {
        when(resolverContext.getExternalSystemTaskId()).thenReturn(null);

        assertNoClassesRegistered(resolver);
    }

    @Test
    void registersNoToolingClassesWhenTheResolverContextHasNotBeenSet() {
        assertNoClassesRegistered(new GradleCheckstyleResolver());
    }

    @Test
    void registersNoToolingClassesWhenTheProjectIsDisposed() {
        final Project project = givenProjectWithGradleImport(true);
        when(project.isDisposed()).thenReturn(true);

        assertNoClassesRegistered(resolver);
    }

    @Test
    void registersNoToolingClassesWhenTheConfigurationManagerIsUnavailable() {
        final Project project = givenProjectWithGradleImport(true);
        when(project.getService(PluginConfigurationManager.class)).thenReturn(null);

        assertNoClassesRegistered(resolver);
    }

    @Test
    void registersNoToolingClassesWhenReadingTheConfigurationFails() {
        final Project project = givenProjectWithGradleImport(true);
        when(project.getService(PluginConfigurationManager.class)).thenThrow(new IllegalStateException("boom"));

        assertNoClassesRegistered(resolver);
    }

    private Project givenProjectWithGradleImport(final boolean enabled) {
        final Project project = mock(Project.class);
        final PluginConfigurationManager configurationManager = mock(PluginConfigurationManager.class);
        when(configurationManager.getCurrent()).thenReturn(PluginConfigurationBuilder.testInstance("10.0.0")
                .withImportSettingsFromGradle(enabled)
                .build());
        when(project.getService(PluginConfigurationManager.class)).thenReturn(configurationManager);

        final ExternalSystemTaskId taskId = mock(ExternalSystemTaskId.class);
        when(taskId.findProject()).thenReturn(project);
        when(resolverContext.getExternalSystemTaskId()).thenReturn(taskId);
        return project;
    }

    private static void assertNoClassesRegistered(final GradleCheckstyleResolver resolver) {
        assertThat(resolver.getExtraProjectModelClasses(), is(Set.of()));
        assertThat(resolver.getToolingExtensionsClasses(), is(Set.of()));
    }

    @SuppressWarnings("unchecked")
    private static CheckstyleGradleModuleData childData(final DataNode<ModuleData> parent) {
        return parent.getChildren().stream()
                .filter(child -> child.getKey().equals(CheckstyleGradleModuleData.KEY))
                .map(child -> (CheckstyleGradleModuleData) child.getData())
                .findFirst()
                .orElse(null);
    }
}
