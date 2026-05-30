package org.infernus.idea.checkstyle.csapi;

import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.infernus.idea.checkstyle.model.ConfigurationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BundledConfigTest {

    @Mock private ConfigurationLocation configurationLocation;

    @Test
    void sunChecksHasExpectedDescription() {
        assertThat(BundledConfig.SUN_CHECKS.getDescription(), containsString("Sun"));
    }

    @Test
    void googleChecksHasExpectedDescription() {
        assertThat(BundledConfig.GOOGLE_CHECKS.getDescription(), containsString("Google"));
    }

    @Test
    void getDefaultReturnsSunChecks() {
        assertThat(BundledConfig.getDefault(), is(BundledConfig.SUN_CHECKS));
    }

    @Test
    void fromDescriptionWithSunReturnsSunChecks() {
        assertThat(BundledConfig.fromDescription("Sun Checks"), is(BundledConfig.SUN_CHECKS));
    }

    @Test
    void fromDescriptionWithoutSunReturnsGoogleChecks() {
        assertThat(BundledConfig.fromDescription("Google Checks"), is(BundledConfig.GOOGLE_CHECKS));
    }

    @Test
    void matchesReturnsTrueForMatchingBundledLocation() {
        when(configurationLocation.getType()).thenReturn(ConfigurationType.BUNDLED);
        when(configurationLocation.getLocation()).thenReturn("(bundled)");
        when(configurationLocation.getDescription()).thenReturn("Sun Checks");

        assertThat(BundledConfig.SUN_CHECKS.matches(configurationLocation), is(true));
    }

    @Test
    void matchesReturnsFalseForNonBundledType() {
        when(configurationLocation.getType()).thenReturn(ConfigurationType.LOCAL_FILE);

        assertThat(BundledConfig.SUN_CHECKS.matches(configurationLocation), is(false));
    }

    @Test
    void getAllBundledConfigsIncludesBothDefaults() {
        Collection<BundledConfig> configs = BundledConfig.getAllBundledConfigs();
        assertThat(configs, notNullValue());
        assertThat(configs.size() >= 2, is(true));
    }

    @Test
    void sunChecksHasSmallerSortOrderThanGoogleChecks() {
        assertThat(BundledConfig.SUN_CHECKS.getSortOrder() < BundledConfig.GOOGLE_CHECKS.getSortOrder(), is(true));
    }
}
