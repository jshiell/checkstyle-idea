package org.infernus.idea.checkstyle.gradle.tooling;

import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CheckstyleGradleModelBuilderTest {

    private static final String MODEL_NAME = CheckstyleGradleModel.class.getName();

    private final CheckstyleGradleModelBuilder builder = new CheckstyleGradleModelBuilder();

    @Test
    void canBuildTheCheckstyleGradleModel() {
        assertThat(builder.canBuild(MODEL_NAME), is(true));
    }

    @Test
    void cannotBuildAnyOtherModel() {
        assertThat(builder.canBuild("some.other.Model"), is(false));
    }

    @Test
    void buildAllReturnsNullWhenNoCheckstylePluginIsApplied() {
        final Project project = ProjectBuilder.builder().build();

        assertThat(builder.buildAll(MODEL_NAME, project), is(nullValue()));
    }

    @Test
    void configFileIsNullWhenCheckstylePluginIsAppliedButNothingIsConfigured() {
        final Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("checkstyle");

        final CheckstyleGradleModel model = (CheckstyleGradleModel) builder.buildAll(MODEL_NAME, project);

        assertThat(model, is(notNullValue()));
        assertThat(model.getConfigFile(), is(nullValue()));
    }

    @Test
    void configFileIsTheAbsolutePathWhenSetToAnExistingFile(@TempDir final Path tempDir) throws IOException {
        final Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("checkstyle");
        final Path configFile = tempDir.resolve("checkstyle.xml");
        Files.writeString(configFile, "<module name=\"Checker\"/>");
        checkstyleExtension(project).setConfigFile(configFile.toFile());

        final CheckstyleGradleModel model = (CheckstyleGradleModel) builder.buildAll(MODEL_NAME, project);

        assertThat(model.getConfigFile(), is(configFile.toFile().getAbsolutePath()));
    }

    @Test
    void toolVersionReflectsAnExplicitlySetValue() {
        final Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("checkstyle");
        checkstyleExtension(project).setToolVersion("10.12.1");

        final CheckstyleGradleModel model = (CheckstyleGradleModel) builder.buildAll(MODEL_NAME, project);

        assertThat(model.getToolVersion(), is("10.12.1"));
    }

    @Test
    void configPropertiesContainsStringifiedValues(@TempDir final Path tempDir) {
        final Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("checkstyle");
        final File cacheFile = tempDir.resolve("build/checkstyle.cache").toFile();
        checkstyleExtension(project).setConfigProperties(Map.of("checkstyle.cache.file", cacheFile));

        final CheckstyleGradleModel model = (CheckstyleGradleModel) builder.buildAll(MODEL_NAME, project);

        assertThat(model.getConfigProperties(),
                is(Map.of("checkstyle.cache.file", cacheFile.toString())));
    }

    @Test
    void configFileIsNullForANonFileBackedTextResource() {
        final Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("checkstyle");
        final CheckstyleExtension checkstyleExtension = checkstyleExtension(project);
        checkstyleExtension.setConfig(project.getResources().getText().fromString("<module name=\"Checker\"/>"));

        // asFile() really does materialise a real, existing temp file for a non-file-backed resource -
        // the point of this test is that its existence alone must not be enough to import it.
        assertThat(checkstyleExtension.getConfigFile().isFile(), is(true));

        final CheckstyleGradleModel model = (CheckstyleGradleModel) builder.buildAll(MODEL_NAME, project);

        assertThat(model.getConfigFile(), is(nullValue()));
    }

    @Test
    void buildAllReturnsNullWhenTheExtensionThrows() {
        final Project project = mock(Project.class);
        final ExtensionContainer extensions = mock(ExtensionContainer.class);
        final CheckstyleExtension checkstyleExtension = mock(CheckstyleExtension.class);
        when(project.getExtensions()).thenReturn(extensions);
        when(extensions.findByName("checkstyle")).thenReturn(checkstyleExtension);
        when(checkstyleExtension.getConfigFile()).thenThrow(new RuntimeException("misbehaving extension"));

        assertThat(builder.buildAll(MODEL_NAME, project), is(nullValue()));
    }

    private static CheckstyleExtension checkstyleExtension(final Project project) {
        return project.getExtensions().getByType(CheckstyleExtension.class);
    }
}
