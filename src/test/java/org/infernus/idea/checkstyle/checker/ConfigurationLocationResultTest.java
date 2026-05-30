package org.infernus.idea.checkstyle.checker;

import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@ExtendWith(MockitoExtension.class)
class ConfigurationLocationResultTest {

    @Mock private ConfigurationLocation configurationLocation;

    @Test
    void notPresentConstantHasNullLocation() {
        assertThat(ConfigurationLocationResult.NOT_PRESENT.location(), is(nullValue()));
    }

    @Test
    void notPresentConstantHasNotPresentStatus() {
        assertThat(ConfigurationLocationResult.NOT_PRESENT.status(), is(ConfigurationLocationStatus.NOT_PRESENT));
    }

    @Test
    void ofCreatesResultWithGivenLocationAndStatus() {
        ConfigurationLocationResult result = ConfigurationLocationResult.of(configurationLocation, ConfigurationLocationStatus.PRESENT);
        assertThat(result.location(), is(configurationLocation));
        assertThat(result.status(), is(ConfigurationLocationStatus.PRESENT));
    }

    @Test
    void ofAllowsNullLocation() {
        ConfigurationLocationResult result = ConfigurationLocationResult.of(null, ConfigurationLocationStatus.BLOCKED);
        assertThat(result.location(), is(nullValue()));
        assertThat(result.status(), is(ConfigurationLocationStatus.BLOCKED));
    }
}
