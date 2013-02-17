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
public class RelativeFileConfigurationLocationTest {

    private static final String PROJECT_PATH = "/the/base-project/path";

    @Mock
    private Project project;
    @Mock
    private VirtualFile projectBase;

    private FileConfigurationLocation underTest;

    @Before
    public void setUp() {
        when(projectBase.getPath()).thenReturn(PROJECT_PATH);
        when(project.getBaseDir()).thenReturn(projectBase);

        underTest = new RelativeFileConfigurationLocation(project);
        underTest.setLocation("aLocation");
        underTest.setDescription("aDescription");
    }

    @Test
    public void anAbsolutePathIsStoredAsProjectRelative() {
        underTest.setLocation("/the/base-project/another-project/rules.xml");

        assertThat(underTest.getLocation(), is(equalTo(PROJECT_PATH + "/../another-project/rules.xml")));
    }

    @Test
    public void aPathWithRelativeElementsIsStoredAsProjectRelative() {
        underTest.setLocation("/the/base-project/another-project/../somewhere/rules.xml");

        assertThat(underTest.getLocation(), is(equalTo(PROJECT_PATH + "/../somewhere/rules.xml")));
    }

    @Test
    public void aPathWithNoCommonElementsIsStoredAsProjectRelative() {
        underTest.setLocation("/somewhere/else/entirely/another-project/rules.xml");

        assertThat(underTest.getLocation(), is(equalTo(PROJECT_PATH + "/../../../somewhere/else/entirely/another-project/rules.xml")));
    }

}
