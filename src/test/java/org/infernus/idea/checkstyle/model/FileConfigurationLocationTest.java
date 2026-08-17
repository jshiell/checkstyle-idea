package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FilenameUtils;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.config.Descriptor;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.infernus.idea.checkstyle.util.ProjectPaths;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FileConfigurationLocationTest {

    private static final String PROJECT_BASE_PATH = "/the/base-project/path";
    private static final String ENABLE_EXTERNAL_DTD_LOAD = "checkstyle.enableExternalDtdLoad";

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

    @AfterEach
    public void clearExternalDtdLoad() {
        System.clearProperty(ENABLE_EXTERNAL_DTD_LOAD);
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

    @Test
    public void theBaseUriIsTheConfigurationFilesOwnUri(@TempDir final Path tempDir) throws IOException {
        final Path configFile = Files.createFile(tempDir.resolve("checkstyle.xml"));
        underTest.setLocation(configFile.toString());

        assertThat(underTest.baseUri(), is(equalTo(configFile.toFile().toURI().toString())));
    }

    @Test
    public void theBaseUriIsNullWhenTheConfigurationFileDoesNotExist() {
        underTest.setLocation("/a-volume/a-path/to/no-such-checkstyle.xml");

        assertThat(underTest.baseUri(), is(nullValue()));
    }

    @Test
    public void theBaseUriIsNullForAConfigurationFileInsideAJar(@TempDir final Path tempDir) throws IOException {
        final Path jarFile = Files.createFile(tempDir.resolve("rules.jar"));
        underTest.setLocation(jarFile + "!/checkstyle.xml");

        assertThat(underTest.baseUri(), is(nullValue()));
    }

    @Test
    public void propertiesAreReadFromAnEntityIncludeWhenExternalDtdLoadIsEnabled(@TempDir final Path tempDir)
            throws IOException {
        System.setProperty(ENABLE_EXTERNAL_DTD_LOAD, "true");
        underTest.setLocation(configurationWithAnEntityIncludeIn(tempDir).toString());

        underTest.resolve(getClass().getClassLoader()).close();

        assertThat(underTest.getProperties(), hasEntry("main-property", ""));
        assertThat(underTest.getProperties(), hasEntry("included-property", "aDefault"));
    }

    @Test
    public void propertiesAreNotReadFromAnEntityIncludeWhenExternalDtdLoadIsDisabled(@TempDir final Path tempDir)
            throws IOException {
        underTest.setLocation(configurationWithAnEntityIncludeIn(tempDir).toString());

        underTest.resolve(getClass().getClassLoader()).close();

        assertThat(underTest.getProperties(), not(hasKey("included-property")));
    }

    @Test
    public void propertiesAreReadWhenExternalDtdLoadIsEnabledAndThereAreNoEntities(@TempDir final Path tempDir)
            throws IOException {
        System.setProperty(ENABLE_EXTERNAL_DTD_LOAD, "true");
        final Path configFile = tempDir.resolve("checkstyle.xml");
        Files.writeString(configFile, """
                <module name="Checker">
                  <property name="something" value="${main-property}"/>
                </module>""");
        underTest.setLocation(configFile.toString());

        underTest.resolve(getClass().getClassLoader()).close();

        assertThat(underTest.getProperties(), hasEntry("main-property", ""));
    }

    private Path configurationWithAnEntityIncludeIn(final Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("included.xml"), """
                <module name="IncludedModule">
                  <property name="anotherThing" value="${included-property}" default="aDefault"/>
                </module>""");

        final Path configFile = tempDir.resolve("checkstyle.xml");
        Files.writeString(configFile, """
                <!DOCTYPE module [
                  <!ENTITY includedModules SYSTEM "./included.xml">
                ]>
                <module name="Checker">
                  <property name="something" value="${main-property}"/>
                  &includedModules;
                </module>""");
        return configFile;
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
