package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FilenameUtils;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.picocontainer.PicoContainer;

import java.io.File;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FileConfigurationLocationTest {
    private static final String PROJECT_PATH = "/the/base-project/path";

    @Mock
    private Project project;
    @Mock
    private VirtualFile projectBase;
    @Mock
    private PicoContainer picoContainer;

    private FileConfigurationLocation underTest;

    @Before
    public void setUp() {
        underTest = useUnixPaths();

        underTest.setLocation("aLocation");
        underTest.setDescription("aDescription");
    }

    @Test
    public void descriptorShouldContainsTypeLocationAndDescription() {
        assertThat(underTest.getDescriptor(), is(equalTo("LOCAL_FILE:aLocation:aDescription")));
    }

    @Test
    public void theProjectDirectoryShouldBeTokenisedInDescriptorForUnixPaths() {
        underTest.setLocation(PROJECT_PATH + "/a-path/to/checkstyle.xml");

        assertThat(underTest.getDescriptor(), is(equalTo("LOCAL_FILE:$PROJECT_DIR$/a-path/to/checkstyle.xml:aDescription")));
    }

    @Test
    public void directoryTraversalsInARelativePathShouldNotBeAlteredByTokenisation() {
        underTest.setLocation(PROJECT_PATH + "/../a-path/to/checkstyle.xml");

        assertThat(underTest.getDescriptor(), is(equalTo("LOCAL_FILE:$PROJECT_DIR$/../a-path/to/checkstyle.xml:aDescription")));
    }

    @Test
    public void theProjectDirectoryShouldBeTokenisedInDescriptorForWindowsPaths() {
        underTest = useWindowsFilePaths();

        underTest.setLocation("c:\\some-where\\a-project\\a\\file\\location-in\\checkstyle.xml");
        underTest.setDescription("aDescription");

        assertThat(underTest.getDescriptor(), is(equalTo("LOCAL_FILE:$PROJECT_DIR$/a/file/location-in/checkstyle.xml:aDescription")));
    }

    @Test
    public void aUnixLocationContainingTheProjectPathShouldBeDetokenisedCorrectly() {
        underTest.setLocation(PROJECT_PATH + "/a-path/to/checkstyle.xml");

        assertThat(underTest.getLocation(), is(equalTo(PROJECT_PATH + "/a-path/to/checkstyle.xml")));
    }

    @Test
    public void directoryTraversalsInARelativePathShouldNotBeAlteredByDetokenisation() {
        underTest.setLocation(PROJECT_PATH + "/../a-path/to/checkstyle.xml");

        assertThat(underTest.getLocation(), is(equalTo(PROJECT_PATH + "/../a-path/to/checkstyle.xml")));
    }

    @Test
    public void aUnixLocationShouldBeStoredAndRetrievedCorrectlyWhenTheProjectPathIsNotUsed() {
        underTest.setLocation("/a-volume/a-path/to/checkstyle.xml");

        assertThat(underTest.getLocation(), is(equalTo("/a-volume/a-path/to/checkstyle.xml")));
    }

    @Test
    public void aUnixLocationShouldBeStoredAndRetrievedCorrectlyWhenTheProjectPathIsNotUsedAndTheFileExistsInAPartiallyMatchingSiblingDirectory() {
        // Issue #9

        underTest.setLocation(PROJECT_PATH + "-sibling/a-path/to/checkstyle.xml");

        assertThat(underTest.getLocation(), is(equalTo(PROJECT_PATH + "-sibling/a-path/to/checkstyle.xml")));
    }

    @Test
    public void aWindowsLocationContainingTheProjectPathShouldBeDetokenisedCorrectly() {
        underTest = useWindowsFilePaths();

        underTest.setLocation("c:\\some-where\\a-project\\a\\file\\location\\checkstyle.xml");

        assertThat(underTest.getLocation(), is(equalTo("c:\\some-where\\a-project\\a\\file\\location\\checkstyle.xml")));
    }

    @Test
    public void aWindowsLocationShouldBeStoredAndRetrievedCorrectlyWhenTheProjectPathIsNotUsed() {
        underTest = useWindowsFilePaths();

        underTest.setLocation("c:\\a\\file\\location\\checkstyle.xml");

        assertThat(underTest.getLocation(), is(equalTo("c:\\a\\file\\location\\checkstyle.xml")));
    }

    private FileConfigurationLocation useWindowsFilePaths() {
        reset(picoContainer);
        ProjectFilePaths testProjectFilePaths = testProjectFilePaths('\\');
        when(picoContainer.getComponentInstance(ProjectFilePaths.class.getName())).thenReturn(testProjectFilePaths);
        when(project.getBaseDir()).thenReturn(projectBase);

        reset(project);
        when(project.getPicoContainer()).thenReturn(picoContainer);
        when(project.getBaseDir()).thenReturn(projectBase);
        when(projectBase.getPath()).thenReturn("c:/some-where/a-project");

        return new FileConfigurationLocation(project);
    }

    private FileConfigurationLocation useUnixPaths() {
        reset(picoContainer);
        ProjectFilePaths testProjectFilePaths = testProjectFilePaths('/');
        when(picoContainer.getComponentInstance(ProjectFilePaths.class.getName())).thenReturn(testProjectFilePaths);

        reset(project);
        when(project.getPicoContainer()).thenReturn(picoContainer);
        when(project.getBaseDir()).thenReturn(projectBase);
        when(projectBase.getPath()).thenReturn(PROJECT_PATH);

        return new FileConfigurationLocation(project);
    }

    @NotNull
    private ProjectFilePaths testProjectFilePaths(char separatorChar) {
        Function<File, String> absolutePathOf = file -> {
            // a nasty hack to pretend we're on a Windows box when required...
            if (file.getPath().startsWith("c:")) {
                return file.getPath().replace('/', '\\').replaceAll("\\\\\\\\", "\\\\");
            }

            return FilenameUtils.separatorsToUnix(file.getPath());
        };

        return new ProjectFilePaths(project, separatorChar, absolutePathOf);
    }

}
