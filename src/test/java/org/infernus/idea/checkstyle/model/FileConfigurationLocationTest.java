package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FileConfigurationLocationTest {
    private static final String PROJECT_PATH = "/the/base-project/path";

    @Mock
    private Project project;
    @Mock
    private VirtualFile projectBase;

    private FileConfigurationLocation unit;

    @Before
    public void setUp() {
        unit = new FileConfigurationLocation(project);
        unit.setLocation("aLocation");
        unit.setDescription("aDescription");

        when(project.getBaseDir()).thenReturn(projectBase);
        when(projectBase.getPath()).thenReturn(PROJECT_PATH);
    }

    @Test
    public void descriptorShouldContainsTypeLocationAndDescription() {
        assertThat(unit.getDescriptor(), is(equalTo("FILE:aLocation:aDescription")));
    }

    @Test
    public void baseDirectoryShouldBeTokenisedInDescriptor() {
        unit.setLocation(PROJECT_PATH + "/a-path/to/checkstyle.xml");

        assertThat(unit.getDescriptor(), is(equalTo("FILE:$PROJECT_DIR$/a-path/to/checkstyle.xml:aDescription")));
    }

    @Test
    public void locationShouldBeDetokenisedCorrectly() {
        unit.setLocation(PROJECT_PATH + "/a-path/to/checkstyle.xml");

        assertThat(unit.getLocation(), is(equalTo(PROJECT_PATH + "/a-path/to/checkstyle.xml")));
    }

}
