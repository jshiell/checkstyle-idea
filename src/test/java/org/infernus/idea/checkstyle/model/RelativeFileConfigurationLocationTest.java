package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FilenameUtils;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.infernus.idea.checkstyle.util.ProjectPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.util.UUID;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RelativeFileConfigurationLocationTest {

    private static final String PROJECT_BASE_PATH = "/the/base-project/path";

    @Mock
    private VirtualFile projectBase;

    private FileConfigurationLocation underTest;

    @BeforeEach
    public void setUp() {
        ProjectPaths projectPaths = mock(ProjectPaths.class);

        Function<File, String> absolutePathOf = file -> {
            // a nasty hack to pretend we're on a Windows box when required...
            if (file.getPath().startsWith("c:")) {
                return file.getPath().replace('/', '\\').replaceAll("\\\\\\\\", "\\\\");
            }

            return FilenameUtils.separatorsToUnix(file.getPath());
        };

        final Project project = TestHelper.mockProject();
        ProjectFilePaths testProjectFilePaths = ProjectFilePaths.testInstanceWith(project, '/', absolutePathOf, projectPaths);
        when(project.getService(ProjectFilePaths.class)).thenReturn(testProjectFilePaths);

        when(projectBase.getPath()).thenReturn(PROJECT_BASE_PATH);
        when(projectPaths.projectPath(project)).thenReturn(projectBase);

        underTest = new RelativeFileConfigurationLocation(project, UUID.randomUUID().toString());
        underTest.setLocation("aLocation");
        underTest.setDescription("aDescription");
    }

    @Test
    public void anAbsolutePathIsStoredAsProjectRelative() {
        underTest.setLocation("/the/base-project/another-project/rules.xml");

        assertThat(underTest.getLocation(), is(equalTo(PROJECT_BASE_PATH + "/../another-project/rules.xml")));
    }

    @Test
    public void aPathWithRelativeElementsIsStoredAsProjectRelative() {
        underTest.setLocation("/the/base-project/another-project/../somewhere/rules.xml");

        assertThat(underTest.getLocation(), is(equalTo(PROJECT_BASE_PATH + "/../somewhere/rules.xml")));
    }

    @Test
    public void aPathWithNoCommonElementsIsStoredAsProjectRelative() {
        underTest.setLocation("/somewhere/else/entirely/another-project/rules.xml");

        assertThat(underTest.getLocation(), is(equalTo(PROJECT_BASE_PATH + "/../../../somewhere/else/entirely/another-project/rules.xml")));
    }

}
