package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BundledConfigurationLocationTest {

    private Project project;
    private BundledConfigurationLocation sunChecks;
    private BundledConfigurationLocation googleChecks;

    @BeforeEach
    void setUp() {
        project = TestHelper.mockProject();
        sunChecks = new BundledConfigurationLocation(BundledConfig.SUN_CHECKS, project);
        googleChecks = new BundledConfigurationLocation(BundledConfig.GOOGLE_CHECKS, project);
    }

    @Test
    void getBundledConfigReturnsBundledConfig() {
        assertThat(sunChecks.getBundledConfig(), is(BundledConfig.SUN_CHECKS));
        assertThat(googleChecks.getBundledConfig(), is(BundledConfig.GOOGLE_CHECKS));
    }

    @Test
    void locationIsFixedToBundledLocationString() {
        // setLocation() is a no-op for bundled configs
        String originalLocation = sunChecks.getLocation();
        sunChecks.setLocation("some/other/path");
        assertThat(sunChecks.getLocation(), is(originalLocation));
    }

    @Test
    void descriptionIsFixedToBundledDescription() {
        // setDescription() is a no-op for bundled configs
        String originalDescription = sunChecks.getDescription();
        sunChecks.setDescription("Custom Description");
        assertThat(sunChecks.getDescription(), is(originalDescription));
    }

    @Test
    void bundledConfigDescriptionMatchesSunChecks() {
        assertThat(sunChecks.getDescription(), is(BundledConfig.SUN_CHECKS.getDescription()));
    }

    @Test
    void bundledConfigDescriptionMatchesGoogleChecks() {
        assertThat(googleChecks.getDescription(), is(BundledConfig.GOOGLE_CHECKS.getDescription()));
    }

    @Test
    void isRemovableReturnsFalse() {
        assertFalse(sunChecks.isRemovable());
        assertFalse(googleChecks.isRemovable());
    }

    @Test
    void cloneReturnsSameType() {
        BundledConfigurationLocation cloned = sunChecks.clone();
        assertThat(cloned, not(sameInstance(sunChecks)));
        assertThat(cloned.getBundledConfig(), is(BundledConfig.SUN_CHECKS));
    }

    @Test
    void clonePreservesBundledConfig() {
        BundledConfigurationLocation cloned = googleChecks.clone();
        assertThat(cloned.getBundledConfig(), is(BundledConfig.GOOGLE_CHECKS));
    }

    @Test
    void clonePreservesIdPropertiesAndNamedScope() {
        BundledConfigurationLocation copy = new BundledConfigurationLocation("a-user-created-copy-id", BundledConfig.GOOGLE_CHECKS, project);
        copy.setProperties(java.util.Map.of("someProperty", "someValue"));
        copy.setNamedScope(TestHelper.NAMED_SCOPE);

        BundledConfigurationLocation cloned = copy.clone();

        assertThat(cloned.getId(), is("a-user-created-copy-id"));
        assertThat(cloned.getProperties(), is(copy.getProperties()));
        assertThat(cloned.getNamedScope(), is(copy.getNamedScope()));
    }

    @Test
    void idMatchesBundledConfigId() {
        assertThat(sunChecks.getId(), is(BundledConfig.SUN_CHECKS.getId()));
        assertThat(googleChecks.getId(), is(BundledConfig.GOOGLE_CHECKS.getId()));
    }

    @Test
    void typeIsBundled() {
        assertThat(sunChecks.getType(), is(ConfigurationType.BUNDLED));
        assertThat(googleChecks.getType(), is(ConfigurationType.BUNDLED));
    }

    @Test
    void sunChecksSortsBeforeGoogleChecks() {
        // SUN_CHECKS has sortOrder 0, GOOGLE_CHECKS has sortOrder 1
        // BundledConfigurationLocation uses priority sort order, so compareTo should be negative
        assertTrue(sunChecks.compareTo(googleChecks) < 0);
    }

    @Test
    void differentlyDescribedCopiesOfTheSameBundledConfigAreNotEqual() throws Exception {
        // Two copies of the same BundledConfig have the same sort order, so compareForPrioritySortOrder
        // must fall back to description (then id) to tell them apart. setDescription() only becomes a
        // real setter in a later increment, so the description difference is forced directly here.
        BundledConfigurationLocation copyOfGoogleChecks = new BundledConfigurationLocation(BundledConfig.GOOGLE_CHECKS, project);
        forceDescription(copyOfGoogleChecks, "My Custom Google Checks");

        assertThat(googleChecks, is(not(equalTo(copyOfGoogleChecks))));

        TreeSet<ConfigurationLocation> locations = new TreeSet<>();
        locations.add(googleChecks);
        locations.add(copyOfGoogleChecks);
        assertThat(locations.size(), is(2));
    }

    private void forceDescription(final BundledConfigurationLocation location, final String description) throws Exception {
        Field descriptionField = ConfigurationLocation.class.getDeclaredField("description");
        descriptionField.setAccessible(true);
        descriptionField.set(location, description);
    }
}
