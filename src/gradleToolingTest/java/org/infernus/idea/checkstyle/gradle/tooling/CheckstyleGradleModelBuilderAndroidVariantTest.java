package org.infernus.idea.checkstyle.gradle.tooling;

import org.gradle.api.Project;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * The "Android variant" from issue #439: AGP applies the {@code checkstyle {}} extension transitively
 * with no useful configuration, and the real configuration lives on a raw
 * {@code task checkstyle(type: Checkstyle) { ... }} instead.
 */
class CheckstyleGradleModelBuilderAndroidVariantTest {

    private static final String MODEL_NAME = CheckstyleGradleModel.class.getName();

    private final CheckstyleGradleModelBuilder builder = new CheckstyleGradleModelBuilder();

    @Test
    void nullWhenNeitherExtensionNorRawTaskArePresent() {
        final Project project = ProjectBuilder.builder().build();

        assertThat(builder.buildAll(MODEL_NAME, project), is(nullValue()));
    }

    @Test
    void configFileComesFromARawTaskWhenNoExtensionIsApplied(@TempDir final Path tempDir) throws IOException {
        final Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        final Path configFile = tempDir.resolve("checkstyle.xml");
        Files.writeString(configFile, "<module name=\"Checker\"/>");
        final Checkstyle task = project.getTasks().register("checkstyle", Checkstyle.class).get();
        task.setConfigFile(configFile.toFile());

        final CheckstyleGradleModel model = (CheckstyleGradleModel) builder.buildAll(MODEL_NAME, project);

        assertThat(model.getConfigFile(), is(configFile.toFile().getAbsolutePath()));
    }

    @Test
    void rawTaskWinsWhenExtensionIsLeftAtItsUnsetDefault(@TempDir final Path tempDir) throws IOException {
        final Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("checkstyle");
        final Path configFile = tempDir.resolve("checkstyle.xml");
        Files.writeString(configFile, "<module name=\"Checker\"/>");
        final Checkstyle task = project.getTasks().register("androidCheckstyle", Checkstyle.class).get();
        task.setConfigFile(configFile.toFile());

        final CheckstyleGradleModel model = (CheckstyleGradleModel) builder.buildAll(MODEL_NAME, project);

        assertThat(model.getConfigFile(), is(configFile.toFile().getAbsolutePath()));
    }

    @Test
    void extensionWinsWhenBothItAndARawTaskPointAtExistingFiles(@TempDir final Path tempDir) throws IOException {
        final Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("checkstyle");
        final Path extensionConfigFile = tempDir.resolve("extension-checkstyle.xml");
        Files.writeString(extensionConfigFile, "<module name=\"Checker\"/>");
        project.getExtensions().getByType(CheckstyleExtension.class).setConfigFile(extensionConfigFile.toFile());
        final Path taskConfigFile = tempDir.resolve("task-checkstyle.xml");
        Files.writeString(taskConfigFile, "<module name=\"Checker\"/>");
        final Checkstyle task = project.getTasks().register("androidCheckstyle", Checkstyle.class).get();
        task.setConfigFile(taskConfigFile.toFile());

        final CheckstyleGradleModel model = (CheckstyleGradleModel) builder.buildAll(MODEL_NAME, project);

        assertThat(model.getConfigFile(), is(extensionConfigFile.toFile().getAbsolutePath()));
    }
}
