package org.infernus.idea.checkstyle.checker;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ConfigurationLocationStatusTest {

    @Test
    void allValuesAreDeclared() {
        ConfigurationLocationStatus[] values = ConfigurationLocationStatus.values();
        assertThat(values.length, is(3));
    }

    @Test
    void valueOfPresentReturnsPresentStatus() {
        assertThat(ConfigurationLocationStatus.valueOf("PRESENT"), is(ConfigurationLocationStatus.PRESENT));
    }

    @Test
    void valueOfNotPresentReturnsNotPresentStatus() {
        assertThat(ConfigurationLocationStatus.valueOf("NOT_PRESENT"), is(ConfigurationLocationStatus.NOT_PRESENT));
    }

    @Test
    void valueOfBlockedReturnsBlockedStatus() {
        assertThat(ConfigurationLocationStatus.valueOf("BLOCKED"), is(ConfigurationLocationStatus.BLOCKED));
    }
}
