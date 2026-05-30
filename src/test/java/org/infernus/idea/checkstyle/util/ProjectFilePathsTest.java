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

    // --- makeProjectRelative ---

    @Test
    public void anAbsolutePathCanBeMadeProjectRelative() {
        assertThat(underTest.makeProjectRelative("/the/base-project/another-project/rules.xml"),
                is(equalTo("../another-project/rules.xml")));
    }

    @Test
    public void aPathWithRelativeElementsCanBeMadeProjectRelative() {
        assertThat(underTest.makeProjectRelative("/the/base-project/another-project/../somewhere/rules.xml"),
                is(equalTo("../somewhere/rules.xml")));
    }

    @Test
    public void aPathWithNoCommonElementsCanBeMadeProjectRelative() {
        assertThat(underTest.makeProjectRelative("/somewhere/else/entirely/another-project/rules.xml"),
                is(equalTo("../../../somewhere/else/entirely/another-project/rules.xml")));
    }

    // --- tokenise ---

    @Test
    public void anAbsolutePathUnderProjectDirIsTokenised() {
        assertThat(underTest.tokenise(UNIX_PROJECT_BASE_PATH + "/checkstyle.xml"),
                is(equalTo("$PROJECT_DIR$/checkstyle.xml")));
    }

    @Test
    public void aRelativePathIsTokenisedByPrependingProjectDirToken() {
        assertThat(underTest.tokenise("../another-project/rules.xml"),
                is(equalTo("$PROJECT_DIR$/../another-project/rules.xml")));
    }

    @Test
    public void anAbsolutePathOutsideProjectDirIsReturnedUnchangedByTokenise() {
        assertThat(underTest.tokenise("/some/other/path/rules.xml"),
                is(equalTo("/some/other/path/rules.xml")));
    }

    @Test
    public void aTokenisedPathIsReturnedUnchangedByTokenise() {
        assertThat(underTest.tokenise("$PROJECT_DIR$/checkstyle.xml"),
                is(equalTo("$PROJECT_DIR$/checkstyle.xml")));
    }

    // --- detokenise ---

    @Test
    public void aTokenisedPathIsDetokenised() {
        assertThat(underTest.detokenise("$PROJECT_DIR$/checkstyle.xml"),
                is(equalTo(UNIX_PROJECT_BASE_PATH + "/checkstyle.xml")));
    }

    @Test
    public void aTokenisedRelativePathIsDetokenised() {
        assertThat(underTest.detokenise("$PROJECT_DIR$/../another-project/rules.xml"),
                is(equalTo(UNIX_PROJECT_BASE_PATH + "/../another-project/rules.xml")));
    }

    @Test
    public void anAbsolutePathIsReturnedUnchangedByDetokenise() {
        assertThat(underTest.detokenise("/some/other/path/rules.xml"),
                is(equalTo("/some/other/path/rules.xml")));
    }

    // --- round-trip ---

    @Test
    public void tokeniseAndDetokeniseAreInverses() {
        final String original = UNIX_PROJECT_BASE_PATH + "/sub/checkstyle.xml";
        assertThat(underTest.detokenise(underTest.tokenise(original)), is(equalTo(original)));
    }

    @Test
    public void makeProjectRelativeFollowedByTokeniseProducesStableResult() {
        // First application
        final String relativised = underTest.tokenise(
                underTest.makeProjectRelative("/the/base-project/another-project/rules.xml"));
        assertThat(relativised, is(equalTo("$PROJECT_DIR$/../another-project/rules.xml")));

        // Simulate IntelliJ expand + second load: detokenise → makeProjectRelative → tokenise
        final String reloaded = underTest.tokenise(
                underTest.makeProjectRelative(
                        underTest.detokenise(relativised)));
        assertThat(reloaded, is(equalTo("$PROJECT_DIR$/../another-project/rules.xml")));
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
