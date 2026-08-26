package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.util.ProjectFilePaths;
import org.infernus.idea.checkstyle.util.ProjectPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ConfigurationExporterTest {

    @Mock
    private ProjectPaths projectPaths;
    private final Project project = TestHelper.mockProject();

    @Test
    public void aDescriptionIsLowercasedAndSpacesAreCollapsedToUnderscores() {
        assertThat(ConfigurationExporter.suggestedFileName(locationWithDescription("Google Checks")),
                is("google_checks.xml"));
    }

    @Test
    public void anExistingXmlSuffixIsStrippedBeforeSanitisation() {
        assertThat(ConfigurationExporter.suggestedFileName(locationWithDescription("My Rules.xml")),
                is("my_rules.xml"));
    }

    @Test
    public void aPunctuationOnlyDescriptionFallsBackToCheckstyle() {
        assertThat(ConfigurationExporter.suggestedFileName(locationWithDescription("!!!")),
                is("checkstyle.xml"));
    }

    @Test
    public void aBlankDescriptionFallsBackToCheckstyle() {
        assertThat(ConfigurationExporter.suggestedFileName(locationWithDescription("   ")),
                is("checkstyle.xml"));
    }

    @Test
    public void aLocationWithConfiguredPropertiesHasConfiguredProperties() {
        final ConfigurationLocation location = newFileLocation();
        location.setProperties(Map.of("x", "y"));

        assertThat(ConfigurationExporter.hasConfiguredProperties(location), is(true));
    }

    @Test
    public void aLocationWithNoConfiguredPropertiesHasNoConfiguredProperties() {
        final ConfigurationLocation location = newFileLocation();

        assertThat(ConfigurationExporter.hasConfiguredProperties(location), is(false));
    }

    @Test
    public void exportWritesTheResolvedXmlToTheDestinationFile(@TempDir final Path tempDir) throws IOException {
        final Path source = tempDir.resolve("source.xml");
        Files.writeString(source, "<module name=\"Checker\"/>");
        final Path destination = tempDir.resolve("exported.xml");

        final ConfigurationLocation location = newFileLocation(source.toString());

        ConfigurationExporter.export(location, getClass().getClassLoader(), destination.toFile());

        assertThat(Files.readAllBytes(destination), equalTo(Files.readAllBytes(source)));
    }

    @Test
    public void exportDoesNotMutateTheOriginalLocation(@TempDir final Path tempDir) throws IOException {
        final Path source = tempDir.resolve("source.xml");
        Files.writeString(source, "<module name=\"Checker\"/>");
        final Path destination = tempDir.resolve("exported.xml");

        final ConfigurationLocation location = newFileLocation(source.toString());
        location.setProperties(Map.of("x", "y"));

        ConfigurationExporter.export(location, getClass().getClassLoader(), destination.toFile());

        assertThat(location.getProperties(), equalTo(Map.of("x", "y")));
    }

    private ConfigurationLocation locationWithDescription(final String description) {
        final ConfigurationLocation location = mock(ConfigurationLocation.class);
        when(location.getDescription()).thenReturn(description);
        return location;
    }

    private ConfigurationLocation newFileLocation() {
        return newFileLocation("aLocation");
    }

    private ConfigurationLocation newFileLocation(final String path) {
        when(project.getService(ProjectFilePaths.class))
                .thenReturn(ProjectFilePaths.testInstanceWith(project, projectPaths));

        return new ConfigurationLocationFactory().create(project, "anId", ConfigurationType.LOCAL_FILE,
                path, "aDescription", TestHelper.NAMED_SCOPE);
    }
}
