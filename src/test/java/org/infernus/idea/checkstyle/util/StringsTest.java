package org.infernus.idea.checkstyle.util;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class StringsTest {

    @Test
    void nullIsBlank() {
        assertThat(Strings.isBlank(null), is(true));
    }

    @Test
    void emptyStringIsBlank() {
        assertThat(Strings.isBlank(""), is(true));
    }

    @Test
    void whitespaceOnlyIsBlank() {
        assertThat(Strings.isBlank("   "), is(true));
    }

    @Test
    void nonEmptyStringIsNotBlank() {
        assertThat(Strings.isBlank("hello"), is(false));
    }

    @Test
    void stringWithLeadingAndTrailingWhitespaceIsNotBlank() {
        assertThat(Strings.isBlank("  hello  "), is(false));
    }
}
