package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.VersionListReader;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.infernus.idea.checkstyle.model.BundledConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationLocationFactory;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Characterises the end-to-end issue #529 flow: a user-created copy of a bundled style, with its own
 * description, named scope, and properties, must survive a real serialise/deserialise round-trip
 * alongside the canonical entry, and both must still resolve to the same underlying rules file.
 */
class ProjectConfigurationStateTest {

    private static final String BUNDLED_VERSION = new VersionListReader().getBundledVersions().last();

    private final Project project = TestHelper.mockProject();
    private final ConfigurationLocationFactory configurationLocationFactory = new ConfigurationLocationFactory();

    @BeforeEach
    void setUp() {
        when(project.getService(ConfigurationLocationFactory.class)).thenReturn(configurationLocationFactory);

        final PluginConfigurationManager pluginConfigurationManager = mock(PluginConfigurationManager.class);
        when(pluginConfigurationManager.getCurrent()).thenReturn(PluginConfigurationBuilder.testInstance(BUNDLED_VERSION).build());
        when(project.getService(PluginConfigurationManager.class)).thenReturn(pluginConfigurationManager);
    }

    @Test
    void aCustomDescribedBundledCopySurvivesSerialisationAlongsideTheCanonicalEntry() throws IOException {
        final BundledConfigurationLocation canonical = configurationLocationFactory.create(BundledConfig.GOOGLE_CHECKS, project);

        final ConfigurationLocation customCopy = configurationLocationFactory.create(project, "a-user-created-copy-id",
                ConfigurationType.BUNDLED, BundledConfig.GOOGLE_CHECKS.getId(), "My Custom Google Checks", TestHelper.NAMED_SCOPE);
        customCopy.setProperties(Map.of(
                "org.checkstyle.google.suppressionfilter.config", "my-suppressions.xml",
                "org.checkstyle.google.suppressionxpathfilter.config", "my-xpath-suppressions.xml"));

        final SortedSet<ConfigurationLocation> locations = new TreeSet<>(List.of(canonical, customCopy));
        final PluginConfiguration pluginConfiguration = PluginConfigurationBuilder.testInstance(BUNDLED_VERSION)
                .withLocations(locations)
                .build();

        final ProjectConfigurationState.ProjectSettings serialised =
                ProjectConfigurationState.ProjectSettings.create(pluginConfiguration);
        final PluginConfiguration deserialised =
                serialised.populate(PluginConfigurationBuilder.testInstance(BUNDLED_VERSION), project).build();

        // 3, not 2: the canonical Sun Checks entry is always seeded in alongside whatever was serialised.
        assertThat(deserialised.getLocations(), hasSize(3));

        final BundledConfigurationLocation deserialisedCanonical = bundledLocationWithDescription(deserialised, "Google Checks");
        final BundledConfigurationLocation deserialisedCopy = bundledLocationWithDescription(deserialised, "My Custom Google Checks");

        assertThat(deserialisedCanonical.getBundledConfig(), is(BundledConfig.GOOGLE_CHECKS));
        assertThat(deserialisedCopy.getBundledConfig(), is(BundledConfig.GOOGLE_CHECKS));
        assertThat(deserialisedCopy.getNamedScope(), is(Optional.of(TestHelper.NAMED_SCOPE)));
        assertThat(deserialisedCopy.getProperties(), is(Map.of(
                "org.checkstyle.google.suppressionfilter.config", "my-suppressions.xml",
                "org.checkstyle.google.suppressionxpathfilter.config", "my-xpath-suppressions.xml")));

        final ClassLoader checkstyleClassLoader = activatedCheckstyleClassLoader();
        assertThat(resolvedByteCountOf(deserialisedCanonical, checkstyleClassLoader), is(resolvedByteCountOf(deserialisedCopy, checkstyleClassLoader)));
    }

    private ClassLoader activatedCheckstyleClassLoader() {
        final CheckstyleProjectService checkstyleProjectService = new CheckstyleProjectService(project);
        checkstyleProjectService.activateCheckstyleVersion(BUNDLED_VERSION, null);
        return checkstyleProjectService.underlyingClassLoader();
    }

    private int resolvedByteCountOf(final ConfigurationLocation location, final ClassLoader checkstyleClassLoader) throws IOException {
        try (InputStream resolved = location.resolve(checkstyleClassLoader)) {
            return resolved.readAllBytes().length;
        }
    }

    private BundledConfigurationLocation bundledLocationWithDescription(final PluginConfiguration configuration,
                                                                         final String description) {
        return configuration.getLocations().stream()
                .filter(location -> description.equals(location.getDescription()))
                .map(BundledConfigurationLocation.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No location found with description " + description));
    }
}
