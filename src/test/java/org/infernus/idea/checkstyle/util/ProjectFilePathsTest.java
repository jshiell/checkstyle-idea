package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class ProjectFilePathsTest {

    private static final String UNIX_PROJECT_BASE_PATH = "/the/base-project/path";

    private ProjectFilePaths underTest;

    @BeforeEach
    public void setUp() {
        underTest = projectFilePathsForUnix();
    }

    @Test
    public void anAbsolutePathCanBeMadeProjectRelative() {
        assertThat(underTest.makeProjectRelative("/the/base-project/another-project/rules.xml"),
                is(equalTo(UNIX_PROJECT_BASE_PATH + "/../another-project/rules.xml")));
    }

    @Test
    public void aPathWithRelativeElementsCanBeMadeProjectRelative() {
        assertThat(underTest.makeProjectRelative("/the/base-project/another-project/../somewhere/rules.xml"),
                is(equalTo(UNIX_PROJECT_BASE_PATH + "/../somewhere/rules.xml")));
    }

    @Test
    public void aPathWithNoCommonElementsCanBeMadeProjectRelative() {
        assertThat(underTest.makeProjectRelative("/somewhere/else/entirely/another-project/rules.xml"),
                is(equalTo(UNIX_PROJECT_BASE_PATH + "/../../../somewhere/else/entirely/another-project/rules.xml")));
    }

    private ProjectFilePaths projectFilePathsForUnix() {
        Project project = mock(Project.class);
        VirtualFile projectBaseFile = mock(VirtualFile.class);
        ProjectPaths projectPaths = mock(ProjectPaths.class);

        when(projectBaseFile.getPath()).thenReturn(UNIX_PROJECT_BASE_PATH);
        when(projectPaths.projectPath(project)).thenReturn(projectBaseFile);

        return ProjectFilePaths.testInstanceWith(project, '/', File::getAbsolutePath, projectPaths);
    }

}
