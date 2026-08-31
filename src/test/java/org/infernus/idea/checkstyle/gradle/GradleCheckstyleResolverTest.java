package org.infernus.idea.checkstyle.gradle;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.idea.IdeaModule;
import org.infernus.idea.checkstyle.gradle.tooling.CheckstyleGradleModel;
import org.infernus.idea.checkstyle.gradle.tooling.CheckstyleGradleModelImpl;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverExtension;
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @SuppressWarnings("unchecked")
    private static CheckstyleGradleModuleData childData(final DataNode<ModuleData> parent) {
        return parent.getChildren().stream()
                .filter(child -> child.getKey().equals(CheckstyleGradleModuleData.KEY))
                .map(child -> (CheckstyleGradleModuleData) child.getData())
                .findFirst()
                .orElse(null);
    }
}
