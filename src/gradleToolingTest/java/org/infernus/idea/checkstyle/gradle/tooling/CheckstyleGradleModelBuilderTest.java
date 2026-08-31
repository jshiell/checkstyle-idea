package org.infernus.idea.checkstyle.gradle.tooling;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class CheckstyleGradleModelBuilderTest {

    private final CheckstyleGradleModelBuilder builder = new CheckstyleGradleModelBuilder();

    @Test
    void canBuildTheCheckstyleGradleModel() {
        assertThat(builder.canBuild(CheckstyleGradleModel.class.getName()), is(true));
    }

    @Test
    void cannotBuildAnyOtherModel() {
        assertThat(builder.canBuild("some.other.Model"), is(false));
    }
}
