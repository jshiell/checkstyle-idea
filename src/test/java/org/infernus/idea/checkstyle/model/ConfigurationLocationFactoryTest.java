package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class ConfigurationLocationFactoryTest {

    private final ConfigurationLocationFactory underTest = new ConfigurationLocationFactory();

    @Test
    public void aFileConfigurationLocationIsCorrectlyParsed() {
        assertThat(
                underTest.create(mock(Project.class), "LOCAL_FILE:/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml:Some checkstyle rules"),
                allOf(
                        hasProperty("location", is("/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml")),
                        hasProperty("description", is("Some checkstyle rules"))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void aTruncatedConfigurationLocationThrowsAnIllegalArgumentException() {
        underTest.create(mock(Project.class), "LOCAL_FILE:/Users/aUser/Projects/aProject/checkstyle/cs-rules.xml");
    }

    @Test(expected = IllegalArgumentException.class)
    public void aConfigurationLocationWithNoFieldSeparatorsThrowsAnIllegalArgumentException() {
        underTest.create(mock(Project.class), "LOCAL_FILE");
    }

}
