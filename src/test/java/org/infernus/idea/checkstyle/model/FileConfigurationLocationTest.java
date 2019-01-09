package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FileConfigurationLocationTest {
    private static final String PROJECT_PATH = "/the/base-project/path";

    @Mock
    private Project project;
    @Mock
    private VirtualFile projectBase;

    private FileConfigurationLocation underTest;

    @Before
    public void setUp() {
        when(project.getBaseDir()).thenReturn(projectBase);
        when(projectBase.getPath()).thenReturn(PROJECT_PATH);

        underTest = new TestFileConfigurationLocation(project, '/');
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
    public void theLegacyProjectDirectoryTokenShouldBeTokenisedInDescriptorForUnixPaths() {
        underTest.setLocation("$PRJ_DIR$/a-path/to/checkstyle.xml");

        assertThat(underTest.getDescriptor(), is(equalTo("LOCAL_FILE:$PROJECT_DIR$/a-path/to/checkstyle.xml:aDescription")));
    }

    @Test
    public void theProjectDirectoryShouldBeTokenisedInDescriptorForWindowsPaths() {
        underTest = new TestFileConfigurationLocation(project, '\\');
        reset(project);
        when(project.getBaseDir()).thenReturn(projectBase);
        when(projectBase.getPath()).thenReturn("c:/some-where/a-project");

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
        underTest = new TestFileConfigurationLocation(project, '\\');
        reset(project);
        when(project.getBaseDir()).thenReturn(projectBase);
        when(projectBase.getPath()).thenReturn("c:/some-where/a-project");

        underTest.setLocation("c:\\some-where\\a-project\\a\\file\\location\\checkstyle.xml");

        assertThat(underTest.getLocation(), is(equalTo("c:\\some-where\\a-project\\a\\file\\location\\checkstyle.xml")));
    }

    @Test
    public void aWindowsLocationShouldBeStoredAndRetrievedCorrectlyWhenTheProjectPathIsNotUsed() {
        underTest = new TestFileConfigurationLocation(project, '\\');

        underTest.setLocation("c:\\a\\file\\location\\checkstyle.xml");

        assertThat(underTest.getLocation(), is(equalTo("c:\\a\\file\\location\\checkstyle.xml")));
    }

    private class TestFileConfigurationLocation extends FileConfigurationLocation {
        private final char separatorChar;

        TestFileConfigurationLocation(final Project project,
                                      final char separatorChar) {
            super(project);
            this.separatorChar = separatorChar;
        }

        @Override
        char separatorChar() {
            return separatorChar;
        }

        @Override
        String absolutePathOf(final File file) {
            // a nasty hack to pretend we're on a Windows box when required...
            if (file.getPath().startsWith("c:")) {
                return file.getPath().replace('/', '\\').replaceAll("\\\\\\\\", "\\\\");
            }

            return FilenameUtils.separatorsToUnix(file.getPath());
        }
    }

}
