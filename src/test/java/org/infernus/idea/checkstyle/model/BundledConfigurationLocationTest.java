package org.infernus.idea.checkstyle.model;

import com.intellij.openapi.project.Project;
import org.infernus.idea.checkstyle.TestHelper;
import org.infernus.idea.checkstyle.csapi.BundledConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void sunChecksSortsBefore_GoogleChecks() {
        // SUN_CHECKS has sortOrder 0, GOOGLE_CHECKS has sortOrder 1
        // BundledConfigurationLocation uses priority sort order, so compareTo should be negative
        assertTrue(sunChecks.compareTo(googleChecks) < 0);
    }
}
