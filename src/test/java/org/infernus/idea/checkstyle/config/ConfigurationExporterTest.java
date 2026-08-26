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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
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

    private ConfigurationLocation locationWithDescription(final String description) {
        final ConfigurationLocation location = mock(ConfigurationLocation.class);
        when(location.getDescription()).thenReturn(description);
        return location;
    }

    private ConfigurationLocation newFileLocation() {
        when(project.getService(ProjectFilePaths.class))
                .thenReturn(ProjectFilePaths.testInstanceWith(project, projectPaths));

        return new ConfigurationLocationFactory().create(project, "anId", ConfigurationType.LOCAL_FILE,
                "aLocation", "aDescription", TestHelper.NAMED_SCOPE);
    }
}
