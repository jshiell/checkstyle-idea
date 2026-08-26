package org.infernus.idea.checkstyle.config;

import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigurationExporterTest {

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

    private ConfigurationLocation locationWithDescription(final String description) {
        final ConfigurationLocation location = mock(ConfigurationLocation.class);
        when(location.getDescription()).thenReturn(description);
        return location;
    }
}
