package org.infernus.idea.checkstyle.checker;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ListPropertyResolverTest {

    @Test
    void resolveReturnsTheValueForAKnownProperty() {
        ListPropertyResolver underTest = new ListPropertyResolver(Map.of("key", "value"));

        assertThat(underTest.resolve("key"), is("value"));
    }

    @Test
    void resolveReturnsNullForAnUnknownProperty() {
        ListPropertyResolver underTest = new ListPropertyResolver(Map.of());

        assertThat(underTest.resolve("unknown"), is(nullValue()));
    }

    @Test
    void resolveReturnsNullForABlankValue() {
        ListPropertyResolver underTest = new ListPropertyResolver(Map.of("key", "  "));

        assertThat(underTest.resolve("key"), is(nullValue()));
    }

    @Test
    void resolveReturnsNullForAnEmptyValue() {
        ListPropertyResolver underTest = new ListPropertyResolver(Map.of("key", ""));

        assertThat(underTest.resolve("key"), is(nullValue()));
    }

    @Test
    void setPropertyAddsANewProperty() {
        ListPropertyResolver underTest = new ListPropertyResolver(Map.of());

        underTest.setProperty("newKey", "newValue");

        assertThat(underTest.resolve("newKey"), is("newValue"));
    }

    @Test
    void setPropertyOverwritesAnExistingProperty() {
        ListPropertyResolver underTest = new ListPropertyResolver(Map.of("key", "original"));

        underTest.setProperty("key", "updated");

        assertThat(underTest.resolve("key"), is("updated"));
    }

    @Test
    void setPropertiesAddsAllEntriesFromTheMap() {
        ListPropertyResolver underTest = new ListPropertyResolver(Map.of());

        underTest.setProperties(Map.of("a", "1", "b", "2"));

        assertThat(underTest.resolve("a"), is("1"));
        assertThat(underTest.resolve("b"), is("2"));
    }

    @Test
    void setPropertiesWithNullMapIsToleratedGracefully() {
        ListPropertyResolver underTest = new ListPropertyResolver(Map.of("key", "value"));

        underTest.setProperties(null);

        assertThat(underTest.resolve("key"), is("value"));
    }

    @Test
    void getPropertyNamesToValuesReturnsTheCurrentMap() {
        ListPropertyResolver underTest = new ListPropertyResolver(Map.of("key", "value"));

        assertThat(underTest.getPropertyNamesToValues(), hasEntry("key", "value"));
    }
}
