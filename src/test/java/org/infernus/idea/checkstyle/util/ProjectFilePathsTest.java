package org.infernus.idea.checkstyle.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class ProjectFilePathsTest {

    private static final String UNIX_PROJECT_BASE_PATH = "/the/base-project/path";
    private static final String WINDOWS_PROJECT_BASE_PATH = "c:/some-where/a-project";

    private ProjectFilePaths underTest;

    @Before
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

    @Test
    public void directoryTraversalsInARelativePathShouldNotBeAlteredByTokenisation() {
        assertThat(underTest.tokenise(UNIX_PROJECT_BASE_PATH + "/../a-path/to/checkstyle.xml"),
                is(equalTo("$PROJECT_DIR$/../a-path/to/checkstyle.xml")));
    }

    @Test
    public void aUnixLocationContainingTheProjectPathShouldBeDetokenisedCorrectly() {
        assertThat(underTest.detokenise(UNIX_PROJECT_BASE_PATH + "/a-path/to/checkstyle.xml"),
                is(equalTo(UNIX_PROJECT_BASE_PATH + "/a-path/to/checkstyle.xml")));
    }

    @Test
    public void aUnixLocationContainingTheProjectPathTokenShouldBeDetokenisedCorrectly() {
        assertThat(underTest.detokenise("$PROJECT_DIR$/a-path/to/checkstyle.xml"),
                is(equalTo(UNIX_PROJECT_BASE_PATH + "/a-path/to/checkstyle.xml")));
    }

    @Test
    public void aUnixLocationContainingTheLegacyProjectPathTokenShouldBeDetokenisedCorrectly() {
        assertThat(underTest.detokenise("$PRJ_DIR$/a-path/to/checkstyle.xml"),
                is(equalTo(UNIX_PROJECT_BASE_PATH + "/a-path/to/checkstyle.xml")));
    }

    @Test
    public void directoryTraversalsInARelativePathShouldNotBeAlteredByDetokenisation() {
        assertThat(underTest.detokenise(UNIX_PROJECT_BASE_PATH + "/../a-path/to/checkstyle.xml"),
                is(equalTo(UNIX_PROJECT_BASE_PATH + "/../a-path/to/checkstyle.xml")));
    }

    @Test
    public void aUnixLocationWhereTheProjectPathIsNotUsedShouldBeUnalteredByTokenisation() {
        assertThat(underTest.tokenise("/a-volume/a-path/to/checkstyle.xml"),
                is(equalTo("/a-volume/a-path/to/checkstyle.xml")));
    }

    @Test
    public void aUnixLocationWhereTheProjectPathIsNotUsedShouldBeUnalteredByDetokenisation() {
        assertThat(underTest.detokenise("/a-volume/a-path/to/checkstyle.xml"),
                is(equalTo("/a-volume/a-path/to/checkstyle.xml")));
    }

    @Test
    public void aUnixLocationWhereTheProjectPathIsNotUsedAndTheFileExistsInAPartiallyMatchingSiblingDirectoryShouldBeUnalteredByTokenisation() {
        assertThat(underTest.tokenise(UNIX_PROJECT_BASE_PATH + "-sibling/a-path/to/checkstyle.xml"),
                is(equalTo(UNIX_PROJECT_BASE_PATH + "-sibling/a-path/to/checkstyle.xml")));
    }

    @Test
    public void aUnixLocationWhereTheProjectPathIsNotUsedAndTheFileExistsInAPartiallyMatchingSiblingDirectoryShouldBeUnalteredByDetokenisation() {
        assertThat(underTest.detokenise(UNIX_PROJECT_BASE_PATH + "-sibling/a-path/to/checkstyle.xml"),
                is(equalTo(UNIX_PROJECT_BASE_PATH + "-sibling/a-path/to/checkstyle.xml")));
    }

    @Test
    public void theProjectDirectoryShouldBeTokenisedInDescriptorForWindowsPaths() {
        underTest = projectFilePathsForWindows();

        assertThat(underTest.tokenise("c:\\some-where\\a-project\\a\\file\\location-in\\checkstyle.xml"),
                is(equalTo("$PROJECT_DIR$/a/file/location-in/checkstyle.xml")));
    }

    @Test
    public void aWindowsLocationContainingTheProjectPathTokenShouldBeDetokenisedCorrectly() {
        underTest = projectFilePathsForWindows();

        assertThat(underTest.detokenise("$PROJECT_DIR$\\a\\file\\location\\checkstyle.xml"),
                is(equalTo("c:\\some-where\\a-project\\a\\file\\location\\checkstyle.xml")));
    }

    @Test
    public void aWindowsLocationContainingTheProjectPathShouldBeDetokenisedCorrectly() {
        underTest = projectFilePathsForWindows();

        assertThat(underTest.detokenise("c:\\some-where\\a-project\\a\\file\\location\\checkstyle.xml"),
                is(equalTo("c:\\some-where\\a-project\\a\\file\\location\\checkstyle.xml")));
    }

    @Test
    public void aWindowsLocationWhereTheProjectPathIsNotUsedShouldNotBeAlteredByTokenisation() {
        underTest = projectFilePathsForWindows();

        assertThat(underTest.tokenise("c:\\a\\file\\location\\checkstyle.xml"),
                is(equalTo("c:/a/file/location/checkstyle.xml")));
    }

    @Test
    public void aWindowsLocationWhereTheProjectPathIsNotUsedShouldNotBeAlteredByDetokenisation() {
        underTest = projectFilePathsForWindows();

        assertThat(underTest.detokenise("c:\\a\\file\\location\\checkstyle.xml"),
                is(equalTo("c:\\a\\file\\location\\checkstyle.xml")));
    }

    private ProjectFilePaths projectFilePathsForUnix() {
        Project project = mock(Project.class);
        VirtualFile projectBaseFile = mock(VirtualFile.class);
        ProjectPaths projectPaths = mock(ProjectPaths.class);

        when(projectBaseFile.getPath()).thenReturn(UNIX_PROJECT_BASE_PATH);
        when(projectPaths.projectPath(project)).thenReturn(projectBaseFile);

        return new ProjectFilePaths(project, '/', File::getAbsolutePath, projectPaths);
    }

    private ProjectFilePaths projectFilePathsForWindows() {
        Project project = mock(Project.class);
        VirtualFile projectBaseFile = mock(VirtualFile.class);
        ProjectPaths projectPaths = mock(ProjectPaths.class);

        when(projectBaseFile.getPath()).thenReturn(WINDOWS_PROJECT_BASE_PATH);
        when(projectPaths.projectPath(project)).thenReturn(projectBaseFile);

        return new ProjectFilePaths(project, '\\',  file -> {
            // a nasty hack to pretend we're on a Windows box when required...
            if (file.getPath().startsWith("c:")) {
                return file.getPath().replace('/', '\\').replaceAll("\\\\\\\\", "\\\\");
            }

            return FilenameUtils.separatorsToUnix(file.getPath());
        }, projectPaths);
    }

}
