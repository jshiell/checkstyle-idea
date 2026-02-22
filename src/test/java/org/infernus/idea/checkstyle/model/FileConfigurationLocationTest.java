package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FilenameUtils;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.config.Descriptor;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.infernus.idea.checkstyle.util.ProjectPaths;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FileConfigurationLocationTest {

    private static final String PROJECT_BASE_PATH = "/the/base-project/path";

    @Mock
    private VirtualFile projectBase;
    @Mock
    private ProjectPaths projectPaths;
    private final Project project = TestHelper.mockProject();

    private FileConfigurationLocation underTest;

    @BeforeEach
    public void setUp() {
        underTest = useUnixPaths();

        underTest.setLocation("aLocation");
        underTest.setDescription("aDescription");
        underTest.setNamedScope(TestHelper.NAMED_SCOPE);
    }

    @Test
    public void descriptorShouldContainsTypeLocationAndDescription() {
        assertThat(Descriptor.of(underTest, project).toString(), is(equalTo("LOCAL_FILE:aLocation:aDescription;test")));
    }

    @Test
    public void aUnixLocationContainingTheProjectPathShouldBeDetokenisedCorrectly() {
        underTest.setLocation(PROJECT_BASE_PATH + "/a-path/to/checkstyle.xml");

        assertThat(underTest.getLocation(), is(equalTo(PROJECT_BASE_PATH + "/a-path/to/checkstyle.xml")));
    }

    @Test
    public void directoryTraversalsInARelativePathShouldNotBeAlteredByDetokenisation() {
        underTest.setLocation(PROJECT_BASE_PATH + "/../a-path/to/checkstyle.xml");

        assertThat(underTest.getLocation(), is(equalTo(PROJECT_BASE_PATH + "/../a-path/to/checkstyle.xml")));
    }

    @Test
    public void aUnixLocationShouldBeStoredAndRetrievedCorrectlyWhenTheProjectPathIsNotUsed() {
        underTest.setLocation("/a-volume/a-path/to/checkstyle.xml");

        assertThat(underTest.getLocation(), is(equalTo("/a-volume/a-path/to/checkstyle.xml")));
    }

    @Test
    public void aUnixLocationShouldBeStoredAndRetrievedCorrectlyWhenTheProjectPathIsNotUsedAndTheFileExistsInAPartiallyMatchingSiblingDirectory() {
        // Issue #9

        underTest.setLocation(PROJECT_BASE_PATH + "-sibling/a-path/to/checkstyle.xml");

        assertThat(underTest.getLocation(), is(equalTo(PROJECT_BASE_PATH + "-sibling/a-path/to/checkstyle.xml")));
    }

    private FileConfigurationLocation useUnixPaths() {
        ProjectFilePaths testProjectFilePaths = testProjectFilePaths('/', project);
        when(project.getService(ProjectFilePaths.class)).thenReturn(testProjectFilePaths);

        return new FileConfigurationLocation(project, "unixTest");
    }

    @NotNull
    private ProjectFilePaths testProjectFilePaths(final char separatorChar, final Project project) {
        Function<File, String> absolutePathOf = file -> {
            // a nasty hack to pretend we're on a Windows box when required...
            if (file.getPath().startsWith("c:")) {
                return file.getPath().replace('/', '\\').replaceAll("\\\\\\\\", "\\\\");
            }

            return FilenameUtils.separatorsToUnix(file.getPath());
        };

        return ProjectFilePaths.testInstanceWith(project, separatorChar, absolutePathOf, projectPaths);
    }

}
