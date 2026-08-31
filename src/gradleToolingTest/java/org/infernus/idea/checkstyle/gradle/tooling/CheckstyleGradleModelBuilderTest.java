package org.infernus.idea.checkstyle.gradle.tooling;

import org.gradle.api.Project;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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

    private static CheckstyleExtension checkstyleExtension(final Project project) {
        return project.getExtensions().getByType(CheckstyleExtension.class);
    }
}
