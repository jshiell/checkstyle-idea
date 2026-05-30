package org.infernus.idea.checkstyle.config;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.infernus.idea.checkstyle.model.ScanScope;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PluginConfigurationTest {

    private final Project project = TestHelper.mockProject();

    // --- PluginConfigurationBuilder.testInstance ---

    @Test
    void testInstanceHasCorrectVersion() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0").build();
        assertThat(config.getCheckstyleVersion(), is("10.0.0"));
    }

    @Test
    void testInstanceHasDefaultValues() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0").build();
        assertThat(config.getScanScope(), is(ScanScope.AllSources));
        assertFalse(config.isSuppressErrors());
        assertFalse(config.isCopyLibs());
        assertFalse(config.isScrollToSource());
        assertFalse(config.isScanBeforeCheckin());
        assertTrue(config.getLocations().isEmpty());
        assertTrue(config.getThirdPartyClasspath().isEmpty());
        assertTrue(config.getActiveLocationIds().isEmpty());
    }

    // --- Builder withX setters ---

    @Test
    void withCheckstyleVersionChangesVersion() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("9.0.0")
                .withCheckstyleVersion("10.1.0")
                .build();
        assertThat(config.getCheckstyleVersion(), is("10.1.0"));
    }

    @Test
    void withSuppressErrorsChangesFlag() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0")
                .withSuppressErrors(true)
                .build();
        assertTrue(config.isSuppressErrors());
    }

    @Test
    void withCopyLibrariesChangesFlag() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0")
                .withCopyLibraries(true)
                .build();
        assertTrue(config.isCopyLibs());
    }

    @Test
    void withScrollToSourceChangesFlag() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0")
                .withScrollToSource(true)
                .build();
        assertTrue(config.isScrollToSource());
    }

    @Test
    void withScanBeforeCheckinChangesFlag() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0")
                .withScanBeforeCheckin(true)
                .build();
        assertTrue(config.isScanBeforeCheckin());
    }

    @Test
    void withScanScopeChangesScope() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0")
                .withScanScope(ScanScope.AllSources)
                .build();
        assertThat(config.getScanScope(), is(ScanScope.AllSources));
    }

    @Test
    void withThirdPartyClassPathChangesPath() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0")
                .withThirdPartyClassPath(List.of("/some/path"))
                .build();
        assertThat(config.getThirdPartyClasspath(), is(List.of("/some/path")));
    }

    // --- Builder.from() ---

    @Test
    void fromCopiesAllFields() {
        ConfigurationLocation loc = aLocation("loc-1", "First Location");
        TreeSet<ConfigurationLocation> locs = new TreeSet<>();
        locs.add(loc);
        TreeSet<String> activeIds = new TreeSet<>(List.of("loc-1"));

        PluginConfiguration original = PluginConfigurationBuilder.testInstance("10.0.0")
                .withSuppressErrors(true)
                .withCopyLibraries(true)
                .withScrollToSource(true)
                .withScanBeforeCheckin(true)
                .withScanScope(ScanScope.AllSources)
                .withLocations(locs)
                .withActiveLocationIds(activeIds)
                .withThirdPartyClassPath(List.of("/lib"))
                .build();

        PluginConfiguration copy = PluginConfigurationBuilder.from(original).build();

        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
    }

    // --- getLocationById ---

    @Test
    void getLocationByIdFindsCorrectLocation() {
        ConfigurationLocation loc = aLocation("my-id", "My Location");
        TreeSet<ConfigurationLocation> locs = new TreeSet<>();
        locs.add(loc);

        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0")
                .withLocations(locs)
                .build();

        assertTrue(config.getLocationById("my-id").isPresent());
        assertSame(loc, config.getLocationById("my-id").get());
    }

    @Test
    void getLocationByIdReturnsEmptyForMissingId() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0").build();
        assertFalse(config.getLocationById("missing").isPresent());
    }

    // --- getActiveLocations ---

    @Test
    void getActiveLocationsReturnsOnlyActiveOnes() {
        ConfigurationLocation loc1 = aLocation("id-1", "Location One");
        ConfigurationLocation loc2 = aLocation("id-2", "Location Two");
        TreeSet<ConfigurationLocation> locs = new TreeSet<>();
        locs.add(loc1);
        locs.add(loc2);
        TreeSet<String> activeIds = new TreeSet<>(List.of("id-1"));

        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0")
                .withLocations(locs)
                .withActiveLocationIds(activeIds)
                .build();

        assertThat(config.getActiveLocations().size(), is(1));
        assertThat(config.getActiveLocations().first().getId(), is("id-1"));
    }

    @Test
    void getActiveLocationsIsEmptyWhenNoActiveIds() {
        ConfigurationLocation loc = aLocation("id-1", "Location One");
        TreeSet<ConfigurationLocation> locs = new TreeSet<>();
        locs.add(loc);

        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0")
                .withLocations(locs)
                .build();

        assertTrue(config.getActiveLocations().isEmpty());
    }

    // --- equals / hashCode ---

    @Test
    void equalConfigurationsAreEqual() {
        PluginConfiguration config1 = PluginConfigurationBuilder.testInstance("10.0.0").build();
        PluginConfiguration config2 = PluginConfigurationBuilder.testInstance("10.0.0").build();
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    void differentVersionsAreNotEqual() {
        PluginConfiguration config1 = PluginConfigurationBuilder.testInstance("10.0.0").build();
        PluginConfiguration config2 = PluginConfigurationBuilder.testInstance("9.0.0").build();
        assertNotEquals(config1, config2);
    }

    @Test
    void configIsNotEqualToNull() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0").build();
        assertNotEquals(null, config);
    }

    @Test
    void configIsEqualToItself() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0").build();
        assertEquals(config, config);
    }

    // --- hasChangedFrom ---

    @Test
    void hasChangedFromReturnsFalseForEqualConfigs() {
        PluginConfiguration config1 = PluginConfigurationBuilder.testInstance("10.0.0").build();
        PluginConfiguration config2 = PluginConfigurationBuilder.testInstance("10.0.0").build();
        assertFalse(config1.hasChangedFrom(config2));
    }

    @Test
    void hasChangedFromReturnsTrueForDifferentVersion() {
        PluginConfiguration config1 = PluginConfigurationBuilder.testInstance("10.0.0").build();
        PluginConfiguration config2 = PluginConfigurationBuilder.testInstance("9.0.0").build();
        assertTrue(config1.hasChangedFrom(config2));
    }

    // --- Immutability ---

    @Test
    void locationsCollectionIsImmutable() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0").build();
        assertThrows(UnsupportedOperationException.class,
                () -> config.getLocations().add(aLocation("new-id", "New")));
    }

    @Test
    void thirdPartyClasspathIsImmutable() {
        PluginConfiguration config = PluginConfigurationBuilder.testInstance("10.0.0").build();
        assertThrows(UnsupportedOperationException.class,
                () -> config.getThirdPartyClasspath().add("/new/path"));
    }

    // --- helper ---

    private ConfigurationLocation aLocation(final String id, final String description) {
        ConfigurationLocation loc = new ConfigurationLocation(id, ConfigurationType.LOCAL_FILE, project) {
            @Override
            @NotNull
            protected InputStream resolveFile(@NotNull final ClassLoader checkstyleClassLoader) {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public Object clone() {
                return aLocation(id, description);
            }
        };
        loc.setDescription(description);
        return loc;
    }
}
