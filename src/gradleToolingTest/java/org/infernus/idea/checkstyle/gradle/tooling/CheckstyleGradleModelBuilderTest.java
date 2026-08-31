package org.infernus.idea.checkstyle.gradle.tooling;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

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
}
